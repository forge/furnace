/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.arquillian;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jboss.arquillian.container.test.spi.ContainerMethodExecutor;
import org.jboss.arquillian.test.spi.TestMethodExecutor;
import org.jboss.arquillian.test.spi.TestResult;
import org.jboss.arquillian.test.spi.TestResult.Status;
import org.jboss.forge.arquillian.protocol.ForgeProtocolConfiguration;
import org.jboss.forge.arquillian.protocol.FurnaceHolder;
import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.proxy.ClassLoaderAdapterBuilder;
import org.jboss.forge.furnace.spi.ExportedInstance;
import org.jboss.forge.furnace.spi.ServiceRegistry;
import org.jboss.forge.furnace.util.Annotations;
import org.jboss.forge.furnace.util.ClassLoaders;

/**
 * @author <a href="mailto:aslak@conduct.no">Aslak Knutsen</a>
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class ForgeTestMethodExecutor implements ContainerMethodExecutor
{
   private Furnace furnace;

   public ForgeTestMethodExecutor(ForgeProtocolConfiguration config, final FurnaceHolder holder)
   {
      if (config == null)
      {
         throw new IllegalArgumentException("ForgeProtocolConfiguration must be specified");
      }
      if (holder == null)
      {
         throw new IllegalArgumentException("Furnace runtime must be provided");
      }
      this.furnace = holder.getFurnace();
   }

   @Override
   public TestResult invoke(final TestMethodExecutor testMethodExecutor)
   {
      try
      {
         if (testMethodExecutor == null)
         {
            throw new IllegalArgumentException("TestMethodExecutor must be specified");
         }

         final String testClassName = testMethodExecutor.getInstance().getClass().getName();

         Object testInstance = null;
         Class<?> testClass = null;
         try
         {
            final AddonRegistry addonRegistry = furnace.getAddonRegistry();

            waitUntilStable(furnace);
            System.out.println("Searching for test [" + testClassName + "]");

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
            String message = "Error launching test "
                     + testMethodExecutor.getInstance().getClass().getName() + "."
                     + testMethodExecutor.getMethod().getName() + "()";
            System.out.println(message);
            throw new IllegalStateException(message, e);
         }

         if (testInstance != null)
         {
            try
            {
               TestResult result = null;
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
                     System.out.println("Could not enhance test class. Falling back to un-proxied invocation.");
                  }

                  Method method = testInstance.getClass().getMethod(testMethodExecutor.getMethod().getName());
                  Annotation[] annotations = method.getAnnotations();

                  for (Annotation annotation : annotations)
                  {
                     if ("org.junit.Ignore".equals(annotation.getClass().getName()))
                     {
                        result = new TestResult(Status.SKIPPED);
                     }
                  }

                  if (result == null)
                  {
                     try
                     {
                        System.out.println("Executing test method: "
                                 + testMethodExecutor.getInstance().getClass().getName() + "."
                                 + testMethodExecutor.getMethod().getName() + "()");

                        try
                        {
                           invokeBefore(testInstance.getClass(), testInstance);
                           method.invoke(testInstance);
                        }
                        catch (Exception e)
                        {
                           throw e;
                        }
                        finally
                        {
                           invokeAfter(testInstance.getClass(), testInstance);
                        }

                        result = new TestResult(Status.PASSED);
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
                  result = new TestResult(Status.FAILED, e);
               }
               catch (Exception e)
               {
                  result = new TestResult(Status.FAILED, e);

                  Throwable cause = e.getCause();
                  while (cause != null)
                  {
                     if (cause instanceof AssertionError)
                     {
                        result = new TestResult(Status.FAILED, cause);
                        break;
                     }
                     cause = cause.getCause();
                  }
               }
               return result;
            }
            catch (Exception e)
            {
               String message = "Error launching test "
                        + testMethodExecutor.getInstance().getClass().getName() + "."
                        + testMethodExecutor.getMethod().getName() + "()";
               System.out.println(message);
               throw new IllegalStateException(message, e);
            }
         }
         else
         {
            throw new IllegalStateException(
                     "Test runner could not locate test class [" + testClassName + "] in any deployed Addon.");
         }
      }
      finally
      {
         furnace = null;
      }
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

   private void waitUntilStable(Furnace furnace) throws InterruptedException
   {
      while (furnace.getStatus().isStarting())
      {
         Thread.sleep(10);
      }
   }
}