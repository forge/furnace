/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.arquillian.impl;

import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutionException;

import org.jboss.arquillian.container.test.spi.ContainerMethodExecutor;
import org.jboss.arquillian.test.spi.TestMethodExecutor;
import org.jboss.arquillian.test.spi.TestResult;
import org.jboss.forge.arquillian.protocol.FurnaceProtocolConfiguration;
import org.jboss.forge.arquillian.protocol.FurnaceHolder;
import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.proxy.ClassLoaderAdapterBuilder;
import org.jboss.forge.furnace.proxy.Proxies;
import org.jboss.forge.furnace.spi.ExportedInstance;
import org.jboss.forge.furnace.spi.ServiceRegistry;
import org.jboss.forge.furnace.util.Annotations;
import org.jboss.forge.furnace.util.Assert;
import org.jboss.forge.furnace.util.ClassLoaders;

/**
 * @author <a href="mailto:aslak@conduct.no">Aslak Knutsen</a>
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class FurnaceTestMethodExecutor implements ContainerMethodExecutor
{
   private Furnace furnace;

   public FurnaceTestMethodExecutor(FurnaceProtocolConfiguration config, final FurnaceHolder holder)
   {
      Assert.notNull(config, FurnaceProtocolConfiguration.class.getName() + " must be provided.");
      Assert.notNull(holder, FurnaceHolder.class.getName() + " runtime must be provided");
      this.furnace = holder.getFurnace();
   }

   @Override
   public TestResult invoke(final TestMethodExecutor testMethodExecutor)
   {
      TestResult result = null;
      try
      {
         Assert.notNull(testMethodExecutor, TestMethodExecutor.class.getName() + " must be specified");
         final String testClassName = testMethodExecutor.getInstance().getClass().getName();

         Object testInstance = null;
         Class<?> testClass = null;
         try
         {
            final AddonRegistry addonRegistry = furnace.getAddonRegistry();

            waitUntilStable(furnace);
            System.out.println("Furnace test harness is searching for test [" + testClassName + "]");

            for (Addon addon : addonRegistry.getAddons())
            {
               if (addon.getStatus().isStarted())
               {
                  ServiceRegistry registry = addon.getServiceRegistry();
                  ExportedInstance<?> exportedInstance = registry.getExportedInstance(testClassName);

                  if (exportedInstance != null)
                  {
                     if (testInstance == null)
                     {
                        testInstance = exportedInstance.get();
                        testClass = ClassLoaders.loadClass(addon.getClassLoader(), testClassName);
                     }
                     else
                     {
                        throw new IllegalStateException(
                                 "Multiple test classes found in deployed addons. " +
                                          "You must have only one @Deployment(testable=true\"); deployment");
                     }
                  }
               }
            }
         }
         catch (Exception e)
         {
            String message = "Furnace test harness encountered an error while launching test: "
                     + testMethodExecutor.getInstance().getClass().getName() + "."
                     + testMethodExecutor.getMethod().getName() + "()";
            System.err.println(message);
            throw new IllegalStateException(message, e);
         }

         if (testInstance != null)
         {
            try
            {
               try
               {
                  try
                  {
                     testInstance = ClassLoaderAdapterBuilder.callingLoader(getClass().getClassLoader())
                              .delegateLoader(testInstance.getClass().getClassLoader())
                              .enhance(testInstance, testClass);
                  }
                  catch (Exception e)
                  {
                     System.err
                              .println("Furnace test harness could not enhance test class. Falling back to un-proxied invocation.");
                  }

                  Method method = testInstance.getClass().getMethod(testMethodExecutor.getMethod().getName());
                  Annotation[] annotations = method.getAnnotations();

                  for (Annotation annotation : annotations)
                  {
                     if ("org.junit.Ignore".equals(annotation.getClass().getName()))
                     {
                        result = TestResult.skipped(null);
                     }
                  }

                  if (result == null)
                  {
                     try
                     {
                        System.out.println("Furnace test harness is executing test method: "
                                 + testMethodExecutor.getInstance().getClass().getName() + "."
                                 + testMethodExecutor.getMethod().getName() + "()");

                        try
                        {
                           invokeBefore(testInstance.getClass(), testInstance);
                           method.invoke(testInstance);
                           result = TestResult.passed();
                        }
                        catch (Exception e)
                        {
                           /*
                            * https://issues.jboss.org/browse/FORGE-1677
                            */
                           Throwable rootCause = getRootCause(e);
                           if (rootCause != null
                                    && Proxies.isForgeProxy(rootCause)
                                    && "org.junit.internal.AssumptionViolatedException".equals(Proxies
                                             .unwrap(rootCause).getClass()
                                             .getName()))
                           {
                              try
                              {
                                 /*
                                  * Due to ClassLoader and serialization restrictions, we need to create a new instance
                                  * of this class.
                                  */
                                 Throwable thisClassloaderException = (Throwable) Class
                                          .forName("org.junit.internal.AssumptionViolatedException")
                                          .getConstructor(String.class).newInstance(rootCause.getMessage());
                                 thisClassloaderException.setStackTrace(rootCause.getStackTrace());
                                 result = TestResult.skipped(thisClassloaderException);
                              }
                              catch (Exception ex)
                              {
                                 // Ignore failure to create a new exception, just pass through the original.
                                 result = TestResult.skipped(e);
                              }
                           }
                           else
                           {
                              throw e;
                           }
                        }
                        finally
                        {
                           invokeAfter(testInstance.getClass(), testInstance);
                        }

                     }
                     catch (InvocationTargetException e)
                     {
                        if (e.getCause() != null && e.getCause() instanceof Exception)
                           throw (Exception) e.getCause();
                        else
                           throw e;
                     }
                  }
               }
               catch (AssertionError e)
               {
                  result = TestResult.failed(e);
               }
               catch (Exception e)
               {
                  result = TestResult.failed(e);

                  Throwable cause = e.getCause();
                  while (cause != null)
                  {
                     if (cause instanceof AssertionError)
                     {
                        result = TestResult.failed(cause);
                        break;
                     }
                     cause = cause.getCause();
                  }
               }

               if (TestResult.Status.FAILED.equals(result.getStatus()))
               {
                  printGraphToStream(System.err);
               }

               return result;
            }
            catch (Exception e)
            {
               String message = "Error launching test "
                        + testMethodExecutor.getInstance().getClass().getName() + "."
                        + testMethodExecutor.getMethod().getName() + "()";
               System.err.println(message);
               throw new IllegalStateException(message, e);
            }
         }
         else
         {

            for (Addon addon : furnace.getAddonRegistry().getAddons())
            {
               try
               {
                  addon.getFuture().get();
               }
               catch (InterruptedException e)
               {
                  // Do nothing
               }
               catch (ExecutionException e)
               {
                  throw new IllegalStateException(
                           "Test runner could not locate test class [" + testClassName + "] in any deployed Addon.",
                           e.getCause());
               }
            }
            throw new IllegalStateException(
                     "Test runner could not locate test class [" + testClassName + "] in any deployed Addon.");
         }
      }
      catch (RuntimeException e)
      {
         printGraphToStream(System.err);
         throw e;
      }
      finally
      {
         furnace = null;
      }
   }

   private void printGraphToStream(PrintStream stream)
   {
      stream.println("Test failed - printing current Addon graph:");
      stream.println(furnace.getAddonRegistry().toString());
   }

   @SuppressWarnings("unchecked")
   private void invokeBefore(Class<?> clazz, Object instance) throws Exception
   {
      if (clazz.getSuperclass() != null && !Object.class.equals(clazz.getSuperclass()))
         invokeBefore(clazz.getSuperclass(), instance);

      for (Method m : clazz.getMethods())
      {
         if (Annotations.isAnnotationPresent(m,
                  (Class<? extends Annotation>) clazz.getClassLoader().loadClass("org.junit.Before")))
         {
            m.invoke(instance);
         }
      }
   }

   @SuppressWarnings("unchecked")
   private void invokeAfter(Class<?> clazz, Object instance) throws Exception
   {
      for (Method m : clazz.getMethods())
      {
         if (Annotations.isAnnotationPresent(m,
                  (Class<? extends Annotation>) clazz.getClassLoader().loadClass("org.junit.After")))
         {
            m.invoke(instance);
         }
      }

      if (clazz.getSuperclass() != null && !Object.class.equals(clazz.getSuperclass()))
         invokeAfter(clazz.getSuperclass(), instance);
   }

   private Throwable getRootCause(Throwable t)
   {
      Throwable cause = t;
      Throwable result = t;
      while (cause != null)
      {
         result = cause;
         cause = cause.getCause();
      }
      return result;
   }

   private void waitUntilStable(Furnace furnace) throws InterruptedException
   {
      while (furnace.getStatus().isStarting())
      {
         Thread.sleep(10);
      }
   }
}