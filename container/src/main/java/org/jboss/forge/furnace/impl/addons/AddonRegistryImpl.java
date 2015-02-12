/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.impl.addons;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonFilter;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.addons.AddonStatus;
import org.jboss.forge.furnace.lock.LockManager;
import org.jboss.forge.furnace.lock.LockMode;
import org.jboss.forge.furnace.repositories.AddonRepository;
import org.jboss.forge.furnace.services.Imported;
import org.jboss.forge.furnace.spi.ServiceRegistry;
import org.jboss.forge.furnace.util.AddonFilters;
import org.jboss.forge.furnace.util.Assert;
import org.jboss.forge.furnace.util.OperatingSystemUtils;

/**
 * Implementation of the {@link AddonRegistry} interface
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class AddonRegistryImpl implements AddonRegistry
{
   private final LockManager lock;
   private final Set<AddonRepository> repositories;
   private final AddonLifecycleManager manager;
   private final String name;

   private static Map<String, Imported<?>> importedCache = new ConcurrentHashMap<>();
   private long cacheVersion = -1;

   public AddonRegistryImpl(LockManager lock, AddonLifecycleManager manager, List<AddonRepository> repositories,
            String name)
   {
      Assert.notNull(lock, "LockManager must not be null.");
      Assert.notNull(manager, "Addon lifecycle manager must not be null.");
      Assert.notNull(repositories, "AddonRepository list must not be null.");
      Assert.isTrue(repositories.size() > 0, "AddonRepository list must not be empty.");

      this.lock = lock;
      this.manager = manager;
      this.repositories = new LinkedHashSet<>(repositories);
      this.name = name;
   }

   @Override
   public void dispose()
   {
      importedCache.clear();
      manager.removeView(this);
      repositories.clear();
   }

   @Override
   public Addon getAddon(final AddonId id)
   {
      Assert.notNull(id, "AddonId must not be null.");
      return lock.performLocked(LockMode.WRITE, new Callable<Addon>()
      {
         @Override
         public Addon call() throws Exception
         {
            Addon result = null;
            while (result == null)
            {
               result = manager.getAddon(AddonRegistryImpl.this, id);
               if (result == null)
                  Thread.sleep(10);
            }
            return result;
         }
      });
   }

   @Override
   public Set<Addon> getAddons()
   {
      return getAddons(AddonFilters.all());
   }

   @Override
   public Set<Addon> getAddons(final AddonFilter filter)
   {
      return lock.performLocked(LockMode.READ, new Callable<Set<Addon>>()
      {
         @Override
         public Set<Addon> call() throws Exception
         {
            HashSet<Addon> result = new HashSet<>();

            for (Addon addon : manager.getAddons(AddonRegistryImpl.this))
            {
               if (filter.accept(addon))
                  result.add(addon);
            }

            return result;
         }
      });
   }

   /**
    * ONLY CALLED INTERNALLY WHEN NEW REPOSITORIES ARE ADDED TO THE ROOT VIEW
    */
   public void addRepository(AddonRepository repository)
   {
      this.repositories.add(repository);
   }

   @Override
   public Set<AddonRepository> getRepositories()
   {
      return Collections.unmodifiableSet(repositories);
   }

   @Override
   @SuppressWarnings("unchecked")
   public <T> Imported<T> getServices(final Class<T> type)
   {
      if (getVersion() != cacheVersion)
      {
         cacheVersion = getVersion();
         importedCache.clear();
      }

      String cacheKey = type.getName()
               + (type.getClassLoader() == null ? "SystemCL" : type.getClassLoader().toString());
      Imported<?> imported = importedCache.get(cacheKey);
      if (imported == null)
      {
         imported = new ImportedImpl<>(this, lock, type);
         importedCache.put(cacheKey, imported);
      }
      return (Imported<T>) imported;
   }

   @Override
   public <T> Imported<T> getServices(final String typeName)
   {
      return new ImportedImpl<>(this, lock, typeName);
   }

   @Override
   public Set<Class<?>> getExportedTypes()
   {
      return lock.performLocked(LockMode.READ, new Callable<Set<Class<?>>>()
      {
         @Override
         public Set<Class<?>> call() throws Exception
         {
            Set<Class<?>> result = new HashSet<>();
            for (Addon addon : getAddons())
            {
               if (AddonStatus.STARTED.equals(addon.getStatus()))
               {
                  ServiceRegistry serviceRegistry = addon.getServiceRegistry();
                  result.addAll(serviceRegistry.getExportedTypes());
               }
            }
            return result;
         }
      });
   }

   @Override
   public <T> Set<Class<T>> getExportedTypes(final Class<T> type)
   {
      return lock.performLocked(LockMode.READ, new Callable<Set<Class<T>>>()
      {
         @Override
         public Set<Class<T>> call() throws Exception
         {
            Set<Class<T>> result = new HashSet<>();
            for (Addon addon : getAddons())
            {
               if (AddonStatus.STARTED.equals(addon.getStatus()))
               {
                  ServiceRegistry serviceRegistry = addon.getServiceRegistry();
                  result.addAll(serviceRegistry.getExportedTypes(type));
               }
            }
            return result;
         }
      });
   }

   @Override
   public long getVersion()
   {
      return manager.getVersion(this);
   }

   @Override
   public String toString()
   {
      StringBuilder builder = new StringBuilder();

      builder.append(name);
      builder.append(OperatingSystemUtils.getLineSeparator());
      builder.append("---REPOSITORIES---").append(OperatingSystemUtils.getLineSeparator());

      Iterator<AddonRepository> repositoryIterator = getRepositories().iterator();
      while (repositoryIterator.hasNext())
      {
         AddonRepository addon = repositoryIterator.next();
         builder.append(addon.toString());
         if (repositoryIterator.hasNext())
            builder.append(OperatingSystemUtils.getLineSeparator());
      }

      builder.append(OperatingSystemUtils.getLineSeparator());
      builder.append("---ADDONS---").append(OperatingSystemUtils.getLineSeparator());

      Iterator<Addon> addonIterator = getAddons().iterator();
      while (addonIterator.hasNext())
      {
         Addon addon = addonIterator.next();
         builder.append(addon.toString());
         if (addonIterator.hasNext())
            builder.append(OperatingSystemUtils.getLineSeparator());
      }

      return builder.toString();
   }

   @Override
   public String getName()
   {
      return name;
   }

   @Override
   public int hashCode()
   {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      AddonRegistryImpl other = (AddonRegistryImpl) obj;
      if (name == null)
      {
         if (other.name != null)
            return false;
      }
      else if (!name.equals(other.name))
         return false;
      return true;
   }

}
