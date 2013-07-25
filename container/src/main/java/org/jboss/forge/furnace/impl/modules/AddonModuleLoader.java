/*
] * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.impl.modules;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.addons.AddonView;
import org.jboss.forge.furnace.exception.ContainerException;
import org.jboss.forge.furnace.impl.addons.AddonLifecycleManager;
import org.jboss.forge.furnace.impl.addons.AddonRepositoryImpl;
import org.jboss.forge.furnace.impl.addons.AddonStateManager;
import org.jboss.forge.furnace.impl.modules.providers.FurnaceContainerSpec;
import org.jboss.forge.furnace.impl.modules.providers.SystemClasspathSpec;
import org.jboss.forge.furnace.impl.modules.providers.XPathJDKClasspathSpec;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.forge.furnace.repositories.AddonRepository;
import org.jboss.forge.furnace.versions.Version;
import org.jboss.modules.DependencySpec;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleSpec;
import org.jboss.modules.ModuleSpec.Builder;
import org.jboss.modules.ResourceLoaderSpec;
import org.jboss.modules.ResourceLoaders;
import org.jboss.modules.filter.PathFilters;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class AddonModuleLoader extends ModuleLoader
{
   private static final Logger logger = Logger.getLogger(AddonModuleLoader.class.getName());

   private Iterable<ModuleSpecProvider> moduleProviders;

   private AddonModuleIdentifierCache moduleCache;
   private AddonModuleJarFileCache moduleJarFileCache;

   private AddonLifecycleManager lifecycleManager;
   private AddonStateManager stateManager;

   private ThreadLocal<Addon> currentAddon = new ThreadLocal<Addon>();

   private Furnace furnace;

   public AddonModuleLoader(Furnace furnace, AddonLifecycleManager lifecycleManager, AddonStateManager stateManager)
   {
      this.furnace = furnace;
      this.lifecycleManager = lifecycleManager;
      this.stateManager = stateManager;
      this.moduleCache = new AddonModuleIdentifierCache();
      this.moduleJarFileCache = new AddonModuleJarFileCache();
      installModuleMBeanServer();
   }

   /**
    * Loads a module for the given Addon.
    */
   public final Module loadAddonModule(Addon addon) throws ModuleLoadException
   {
      try
      {
         this.currentAddon.set(addon);
         ModuleIdentifier moduleId = moduleCache.getModuleId(addon);
         Module result = loadModule(moduleId);
         return result;
      }
      catch (ModuleLoadException e)
      {
         throw e;
      }
      finally
      {
         this.currentAddon.remove();
      }
   }

   @Override
   protected Module preloadModule(ModuleIdentifier identifier) throws ModuleLoadException
   {
      Module pluginModule = super.preloadModule(identifier);
      return pluginModule;
   }

   @Override
   protected ModuleSpec findModule(ModuleIdentifier id) throws ModuleLoadException
   {
      ModuleSpec result = findRegularModule(id);

      if (result == null && currentAddon.get() != null)
         result = findAddonModule(id);

      return result;
   }

   private ModuleSpec findRegularModule(ModuleIdentifier id)
   {
      ModuleSpec result = null;
      for (ModuleSpecProvider p : getModuleProviders())
      {
         result = p.get(this, id);
         if (result != null)
            break;
      }
      return result;
   }

   private Iterable<ModuleSpecProvider> getModuleProviders()
   {
      if (moduleProviders == null)
         moduleProviders = ServiceLoader.load(ModuleSpecProvider.class, furnace.getRuntimeClassLoader());
      return moduleProviders;
   }

   private ModuleSpec findAddonModule(ModuleIdentifier id)
   {
      Addon addon = currentAddon.get();
      if (addon != null)
      {
         Set<AddonView> views = stateManager.getViewsOf(addon);
         AddonId found = addon.getId();
         for (AddonRepository repository : views.iterator().next().getRepositories())
         {
            if (repository.isEnabled(found) && repository.isDeployed(found))
            {
               Addon mappedAddon = moduleCache.getAddon(id);

               if (mappedAddon != null && mappedAddon.getId().equals(found))
               {
                  Builder builder = ModuleSpec.build(id);

                  builder.addDependency(DependencySpec.createModuleDependencySpec(SystemClasspathSpec.ID));
                  builder.addDependency(DependencySpec.createModuleDependencySpec(XPathJDKClasspathSpec.ID));
                  builder.addDependency(DependencySpec.createModuleDependencySpec(PathFilters.acceptAll(),
                           PathFilters.rejectAll(), null, FurnaceContainerSpec.ID, false));
                  try
                  {
                     addContainerDependencies(views, repository, found, builder);
                  }
                  catch (ContainerException e)
                  {
                     logger.warning(e.getMessage());
                     return null;
                  }

                  builder.addDependency(DependencySpec.createLocalDependencySpec(PathFilters.acceptAll(),
                           PathFilters.acceptAll()));

                  try
                  {
                     addAddonDependencies(views, repository, found, builder);
                  }
                  catch (ContainerException e)
                  {
                     logger.warning(e.getMessage());
                     return null;
                  }

                  addLocalResources(repository, found, builder, id);

                  return builder.create();
               }
            }
         }
      }
      return null;
   }

   private void addLocalResources(AddonRepository repository, AddonId found, Builder builder, ModuleIdentifier id)
   {
      List<File> resources = repository.getAddonResources(found);
      for (File file : resources)
      {
         try
         {
            if (file.isDirectory())
            {
               builder.addResourceRoot(
                        ResourceLoaderSpec.createResourceLoaderSpec(
                                 ResourceLoaders.createFileResourceLoader(file.getName(), file),
                                 PathFilters.acceptAll())
                        );
            }
            else if (file.length() > 0)
            {
               JarFile jarFile = new JarFile(file);
               moduleJarFileCache.addJarFileReference(id, jarFile);
               builder.addResourceRoot(
                        ResourceLoaderSpec.createResourceLoaderSpec(
                                 ResourceLoaders.createJarResourceLoader(file.getName(), jarFile),
                                 PathFilters.acceptAll())
                        );
            }
         }
         catch (IOException e)
         {
            throw new ContainerException("Could not load resources from [" + file.getAbsolutePath() + "]", e);
         }
      }
   }

   private void addContainerDependencies(Set<AddonView> views, AddonRepository repository, AddonId found,
            Builder builder)
            throws ContainerException
   {
      Set<AddonDependencyEntry> addons = repository.getAddonDependencies(found);
      for (AddonDependencyEntry dependency : addons)
      {
         /*
          * Containers should always take precedence at runtime.
          */
         if (dependency.getName().startsWith("org.jboss.forge.furnace:container"))
            addAddonDependency(views, found, builder, dependency);
      }
   }

   private void addAddonDependencies(Set<AddonView> views, AddonRepository repository, AddonId found, Builder builder)
            throws ContainerException
   {
      Set<AddonDependencyEntry> addons = repository.getAddonDependencies(found);
      for (AddonDependencyEntry dependency : addons)
      {
         if (!dependency.getName().startsWith("org.jboss.forge.furnace:container"))
            addAddonDependency(views, found, builder, dependency);
      }
   }

   private void addAddonDependency(Set<AddonView> views, AddonId found, Builder builder, AddonDependencyEntry dependency)
   {
      AddonId addonId = stateManager.resolveAddonId(views, dependency.getName());
      ModuleIdentifier moduleId = null;
      if (addonId != null)
      {
         Addon addon = lifecycleManager.getAddon(views, addonId);
         moduleId = findCompatibleInstalledModule(addonId);
         if (moduleId != null)
         {
            builder.addDependency(DependencySpec.createModuleDependencySpec(
                     PathFilters.not(PathFilters.getMetaInfFilter()),
                     dependency.isExported() ? PathFilters.acceptAll() : PathFilters.rejectAll(),
                     this,
                     moduleCache.getModuleId(addon),
                     dependency.isOptional()));
         }
      }

      if (!dependency.isOptional() && (addonId == null || moduleId == null))
         throw new ContainerException("Dependency [" + dependency + "] could not be loaded for addon [" + found
                  + "]");
   }

   private ModuleIdentifier findCompatibleInstalledModule(AddonId addonId)
   {
      ModuleIdentifier result = null;

      Addon addon = currentAddon.get();
      Version runtimeAPIVersion = AddonRepositoryImpl.getRuntimeAPIVersion();

      for (AddonRepository repository : stateManager.getViewsOf(addon).iterator().next().getRepositories())
      {
         List<AddonId> enabled = repository.listEnabledCompatibleWithVersion(runtimeAPIVersion);
         for (AddonId id : enabled)
         {
            if (id.getName().equals(addonId.getName()))
            {
               result = moduleCache.getModuleId(addon);
               break;
            }
         }
      }

      return result;
   }

   @Override
   public String toString()
   {
      return "AddonModuleLoader";
   }

   public void releaseAddonModule(Addon addon)
   {
      ModuleIdentifier id = moduleCache.getModuleId(addon);
      moduleJarFileCache.closeJarFileReferences(id);
      Module loadedModule = findLoadedModuleLocal(id);
      if (loadedModule != null)
         unloadModuleLocal(loadedModule);
      moduleCache.clear(addon);
   }

   /**
    * Installs the MBeanServer.
    */
   private void installModuleMBeanServer()
   {
      try
      {
         Method method = ModuleLoader.class.getDeclaredMethod("installMBeanServer");
         method.setAccessible(true);
         method.invoke(null);
      }
      catch (Exception e)
      {
         throw new ContainerException("Could not install Modules MBean server", e);
      }
   }

}
