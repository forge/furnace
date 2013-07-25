/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.arquillian.services;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.lock.LockMode;
import org.jboss.forge.furnace.services.ExportedInstance;
import org.jboss.forge.furnace.services.ServiceRegistry;
import org.jboss.forge.furnace.util.Addons;
import org.jboss.forge.furnace.util.Assert;
import org.jboss.forge.furnace.util.ClassLoaders;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class ReflectionServiceRegistry implements ServiceRegistry
{
   private Logger log = Logger.getLogger(getClass().getName());

   private Addon addon;
   private Set<Class<?>> serviceTypes;
   private Furnace furnace;

   public ReflectionServiceRegistry(Furnace furnace, Addon addon, Set<Class<?>> serviceTypes)
   {
      this.furnace = furnace;
      this.addon = addon;
      this.serviceTypes = serviceTypes;
   }

   @Override
   public <T> Set<ExportedInstance<T>> getExportedInstances(Class<T> clazz)
   {
      Set<ExportedInstance<T>> result = new HashSet<ExportedInstance<T>>();
      if (serviceTypes.contains(clazz))
      {
         for (Class<?> type : serviceTypes)
         {
            if (clazz.isAssignableFrom(type))
            {
               result.add(new ReflectionExportedInstance<T>(addon, clazz));
            }
         }
      }
      return result;
   }

   @Override
   @SuppressWarnings("unchecked")
   public <T> Set<ExportedInstance<T>> getExportedInstances(String clazz)
   {
      Set<ExportedInstance<T>> result = new HashSet<ExportedInstance<T>>();
      if (serviceTypes.contains(clazz))
      {
         result.addAll(getExportedInstances((Class<T>) ClassLoaders.loadClass(addon.getClassLoader(), clazz)));
      }
      return result;
   }

   @Override
   public <T> ExportedInstance<T> getExportedInstance(final Class<T> requestedType)
   {
      Assert.notNull(requestedType, "Requested Class type may not be null");
      Addons.waitUntilStarted(addon);
      return furnace.getLockManager().performLocked(LockMode.READ, new Callable<ExportedInstance<T>>()
      {
         @Override
         public ExportedInstance<T> call() throws Exception
         {
            /*
             * Double checked waiting, with timeout to prevent complete deadlocks.
             */
            Addons.waitUntilStarted(addon, 10, TimeUnit.SECONDS);
            if (!ClassLoaders.ownsClass(addon.getClassLoader(), requestedType))
            {
               log.fine("Class " + requestedType.getName() + " is not present in this addon [" + addon + "]");
               return null;
            }

            return new ReflectionExportedInstance<T>(addon, requestedType);
         }
      });
   }

   @Override
   @SuppressWarnings("unchecked")
   public <T> ExportedInstance<T> getExportedInstance(String type)
   {
      return getExportedInstance((Class<T>) ClassLoaders.loadClass(addon.getClassLoader(), type));
   }

   @Override
   public Set<Class<?>> getExportedTypes()
   {
      return Collections.unmodifiableSet(serviceTypes);
   }

   @Override
   public <T> Set<Class<T>> getExportedTypes(Class<T> type)
   {
      Set<Class<T>> result = new HashSet<Class<T>>();
      for (Class<?> serviceType : serviceTypes)
      {
         if (type.isAssignableFrom(serviceType))
         {
            result.add(type);
         }
      }
      return result;
   }

   @Override
   public boolean hasService(Class<?> clazz)
   {
      return !getExportedTypes(clazz).isEmpty();
   }

   @Override
   public boolean hasService(String clazz)
   {
      if (ClassLoaders.containsClass(addon.getClassLoader(), clazz))
         return hasService(ClassLoaders.loadClass(addon.getClassLoader(), clazz));
      return false;
   }

}
