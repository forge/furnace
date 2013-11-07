/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.forge.furnace.util;

import java.lang.ref.Reference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class SecurityActions
{
   private static final Logger log = Logger.getLogger(SecurityActions.class.getName());

   private SecurityActions()
   {
      // forbidden inheritance
   }

   /**
    * Gets context classloader.
    * 
    * @return the current context classloader
    */
   public static ClassLoader getContextClassLoader()
   {
      if (System.getSecurityManager() == null)
      {
         return Thread.currentThread().getContextClassLoader();
      }
      else
      {
         return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>()
         {
            @Override
            public ClassLoader run()
            {
               return Thread.currentThread().getContextClassLoader();
            }
         });
      }
   }

   /**
    * Sets context classloader.
    * 
    * @param classLoader the classloader
    */
   public static void setContextClassLoader(final ClassLoader classLoader)
   {
      if (System.getSecurityManager() == null)
      {
         Thread.currentThread().setContextClassLoader(classLoader);
      }
      else
      {
         AccessController.doPrivileged(new PrivilegedAction<Object>()
         {
            @Override
            public Object run()
            {
               Thread.currentThread().setContextClassLoader(classLoader);
               return null;
            }
         });
      }
   }

   /**
    * Cleanup {@link ThreadLocal} instances of the given {@link Thread}.
    * 
    * @param thread The {@link Thread} to clean up.
    */
   public static void cleanupThreadLocals(Thread thread)
   {
      try
      {
         if (thread != null)
         {
            cleanField(thread, Thread.class.getDeclaredField("threadLocals"));
            cleanField(thread, Thread.class.getDeclaredField("inheritableThreadLocals"));
         }
      }
      catch (Exception e)
      {
         log.log(Level.WARNING, "Failed to cleanup ThreadLocal instances for Thread [" + thread + "]", e);
      }
   }

   private static void cleanField(Thread thread, Field threadLocalsField) throws IllegalAccessException,
            ClassNotFoundException, NoSuchFieldException
   {
      threadLocalsField.setAccessible(true);
      Object threadLocalTable = threadLocalsField.get(thread);

      // Get a reference to the array holding the thread local variables inside the
      // ThreadLocalMap of the current thread
      Class<?> threadLocalMapClass = Class.forName("java.lang.ThreadLocal$ThreadLocalMap");
      Field tableField = threadLocalMapClass.getDeclaredField("table");
      tableField.setAccessible(true);
      if (threadLocalTable != null)
      {
         Object table = tableField.get(threadLocalTable);

         // The key to the ThreadLocalMap is a WeakReference object. The referent field of this object
         // is a reference to the actual ThreadLocal variable
         Field referentField = Reference.class.getDeclaredField("referent");
         referentField.setAccessible(true);

         for (int i = 0; i < Array.getLength(table); i++)
         {
            // Each entry in the table array of ThreadLocalMap is an Entry object
            // representing the thread local reference and its value
            Object entry = Array.get(table, i);
            if (entry != null)
            {
               // Get a reference to the thread local object and remove it from the table
               ThreadLocal<?> threadLocal = (ThreadLocal<?>) referentField.get(entry);
               if (threadLocal != null)
                  threadLocal.remove();
            }
         }
      }
   }

}
