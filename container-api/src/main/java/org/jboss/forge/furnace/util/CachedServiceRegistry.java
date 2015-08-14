/**
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.util;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.jboss.forge.furnace.spi.ExportedInstance;
import org.jboss.forge.furnace.spi.ServiceRegistry;

/**
 * Decorator that caches lookup invocations from the underlying {@link ServiceRegistry}
 * 
 * This implementation caches only {@link ExportedInstance} lookups, as this is mostly used.
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
@SuppressWarnings("unchecked")
public class CachedServiceRegistry implements ServiceRegistry
{
   private final ServiceRegistry delegate;

   private final Map<String, ExportedInstance<?>> instanceCache = new WeakHashMap<>();
   private final Map<String, Set<ExportedInstance<?>>> instancesCache = new WeakHashMap<>();

   public CachedServiceRegistry(ServiceRegistry delegate)
   {
      Assert.notNull(delegate, "Delegated ServiceRegistry should not be null");
      this.delegate = delegate;
   }

   @Override
   public <T> Set<ExportedInstance<T>> getExportedInstances(Class<T> clazz)
   {
      Set<ExportedInstance<T>> result = (Set) instancesCache.get(clazz.getName());
      if (result == null)
      {
         result = delegate.getExportedInstances(clazz);
         instancesCache.put(clazz.getName(), (Set) result);
      }
      return result;
   }

   @Override
   public <T> Set<ExportedInstance<T>> getExportedInstances(String clazz)
   {
      Set<ExportedInstance<T>> result = (Set) instancesCache.get(clazz);
      if (result == null)
      {
         result = delegate.getExportedInstances(clazz);
         instancesCache.put(clazz, (Set) result);
      }
      return result;
   }

   @Override
   public <T> ExportedInstance<T> getExportedInstance(Class<T> type)
   {
      ExportedInstance<?> exportedInstance = instanceCache.get(type.getName());
      if (exportedInstance == null)
      {
         exportedInstance = delegate.getExportedInstance(type);
         instanceCache.put(type.getName(), exportedInstance);
      }
      return (ExportedInstance<T>) exportedInstance;
   }

   @Override
   public <T> ExportedInstance<T> getExportedInstance(String type)
   {
      ExportedInstance<?> exportedInstance = instanceCache.get(type);
      if (exportedInstance == null)
      {
         exportedInstance = delegate.getExportedInstance(type);
         instanceCache.put(type, exportedInstance);
      }
      return (ExportedInstance<T>) exportedInstance;
   }

   @Override
   public Set<Class<?>> getExportedTypes()
   {
      return delegate.getExportedTypes();
   }

   @Override
   public <T> Set<Class<T>> getExportedTypes(Class<T> type)
   {
      return delegate.getExportedTypes(type);
   }

   @Override
   public boolean hasService(Class<?> clazz)
   {
      return delegate.hasService(clazz);
   }

   @Override
   public boolean hasService(String clazz)
   {
      return delegate.hasService(clazz);
   }

   /**
    * @return the delegate
    */
   public ServiceRegistry getDelegate()
   {
      return delegate;
   }

}
