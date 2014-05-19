/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.proxy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import org.jboss.forge.furnace.exception.ContainerException;
import org.jboss.forge.furnace.util.ClassLoaders;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class ClassLoaderInterceptor implements ForgeProxy
{
   private static final ThreadLocal<ClassLoader> currentLoader = new ThreadLocal<>();

   private final ClassLoader loader;
   private final Object delegate;

   public ClassLoaderInterceptor(ClassLoader loader, Object delegate)
   {
      this.loader = loader;
      this.delegate = delegate;
   }

   @Override
   public Object invoke(final Object self, final Method thisMethod, final Method proceed, final Object[] args)
            throws Throwable
   {
      if (Thread.currentThread().isInterrupted())
      {
         throw new ContainerException("Thread.interrupt() requested.");
      }

      Callable<Object> task = new Callable<Object>()
      {
         @Override
         public Object call() throws Exception
         {
            try
            {
               if (thisMethod.getDeclaringClass().getName().equals(ForgeProxy.class.getName()))
               {
                  if (thisMethod.getName().equals("getDelegate"))
                     return ClassLoaderInterceptor.this.getDelegate();
                  if (thisMethod.getName().equals("getHandler"))
                     return ClassLoaderInterceptor.this.getHandler();
               }
            }
            catch (Exception e)
            {
            }

            ClassLoader previousLoader = null;
            Object result;
            try
            {
               previousLoader = setCurrentLoader(loader);
               result = thisMethod.invoke(delegate, args);
            }
            catch (InvocationTargetException e)
            {
               if (e.getCause() instanceof Exception)
                  throw (Exception) e.getCause();
               throw e;
            }
            catch (IllegalArgumentException e) {
               System.out.println(thisMethod.getDeclaringClass());
               System.out.println(delegate.getClass());
               throw e;
            }
            finally
            {
               setCurrentLoader(previousLoader);
            }
            return result;
         }
      };

      return ClassLoaders.executeIn(loader, task);
   }

   public static ClassLoader getCurrentloader()
   {
      return currentLoader.get();
   }

   private static ClassLoader setCurrentLoader(ClassLoader loader)
   {
      ClassLoader previous = currentLoader.get();
      currentLoader.set(loader);
      return previous;
   }

   @Override
   public Object getDelegate() throws Exception
   {
      return delegate;
   }

   @Override
   public Object getHandler() throws Exception
   {
      return this;
   }

}