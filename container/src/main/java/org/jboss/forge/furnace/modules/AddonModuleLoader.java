/*
] * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.modules;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.addons.AddonLifecycleManager;
import org.jboss.forge.furnace.addons.AddonView;
import org.jboss.forge.furnace.exception.ContainerException;
import org.jboss.forge.furnace.impl.AddonRepositoryImpl;
import org.jboss.forge.furnace.modules.providers.FurnaceContainerSpec;
import org.jboss.forge.furnace.modules.providers.JGraphTClasspathSpec;
import org.jboss.forge.furnace.modules.providers.SystemClasspathSpec;
import org.jboss.forge.furnace.modules.providers.WeldClasspathSpec;
import org.jboss.forge.furnace.modules.providers.XPathJDKClasspathSpec;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.forge.furnace.repositories.AddonRepository;
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

   private final Iterable<ModuleSpecProvider> moduleProviders;

   private AddonModuleIdentifierCache moduleCache;
   private AddonModuleJarFileCache moduleJarFileCache;

   private AddonLifecycleManager manager;

   private ThreadLocal<AddonView> currentView = new ThreadLocal<AddonView>();
   private ThreadLocal<Set<AddonView>> currentViews = new ThreadLocal<Set<AddonView>>();

   public AddonModuleLoader(Furnace furnace, AddonLifecycleManager manager)
   {
      this.manager = manager;
      this.moduleCache = new AddonModuleIdentifierCache();
      this.moduleJarFileCache = new AddonModuleJarFileCache();
      moduleProviders = ServiceLoader.load(ModuleSpecProvider.class, furnace.getRuntimeClassLoader());
      installModuleMBeanServer();
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

   @Override
   protected Module preloadModule(ModuleIdentifier identifier) throws ModuleLoadException
   {
      Module pluginModule = super.preloadModule(identifier);
      return pluginModule;
   }

   @Override
   protected ModuleSpec findModule(ModuleIdentifier id) throws ModuleLoadException
   {
      ModuleSpec result = findAddonModule(id);
      if (result == null)
         result = findRegularModule(id);

      return result;
   }

   /**
    * Loads a module from the current {@link AddonView} based on the {@link AddonId}
    * 
    * @param views
    */
   public final Module loadModule(Set<AddonView> views, AddonView view, AddonId addonId) throws ModuleLoadException
   {
      try
      {
         this.currentView.set(view);
         this.currentViews.set(views);
         ModuleIdentifier moduleId = moduleCache.getModuleId(views, addonId);
         Module result = loadModule(moduleId);
         return result;
      }
      catch (ModuleLoadException e)
      {
         throw e;
      }
      finally
      {
         this.currentView.remove();
         this.currentViews.remove();
      }
   }

   private ModuleSpec findRegularModule(ModuleIdentifier id)
   {
      ModuleSpec result = null;
      for (ModuleSpecProvider p : moduleProviders)
      {
         result = p.get(this, id);
         if (result != null)
            break;
      }
      return result;
   }

   public ModuleSpec findAddonModule(ModuleIdentifier id)
   {
      for (AddonRepository repository : manager.getRepositories())
      {
         AddonId found = findInstalledModule(repository, id);

         if (found != null)
         {
            Builder builder = ModuleSpec.build(id);

            // Set up the ClassPath for this addon Module

            // TODO Reduce visibility of Weld and JGrapht to Forge Module only.
            builder.addDependency(DependencySpec.createModuleDependencySpec(SystemClasspathSpec.ID));
            builder.addDependency(DependencySpec.createModuleDependencySpec(XPathJDKClasspathSpec.ID));
            builder.addDependency(DependencySpec.createModuleDependencySpec(PathFilters.acceptAll(),
                     PathFilters.rejectAll(), null, FurnaceContainerSpec.ID, false));
            builder.addDependency(DependencySpec.createModuleDependencySpec(PathFilters.acceptAll(),
                     PathFilters.rejectAll(), null, JGraphTClasspathSpec.ID, false));
            builder.addDependency(DependencySpec.createModuleDependencySpec(PathFilters.acceptAll(),
                     PathFilters.rejectAll(), null, WeldClasspathSpec.ID, false));

            builder.addDependency(DependencySpec.createLocalDependencySpec(PathFilters.acceptAll(),
                     PathFilters.acceptAll()));
            try
            {
               addAddonDependencies(repository, found, builder);
            }
            catch (ContainerException e)
            {
               // TODO implement proper fault handling. For now, abort.
               logger.warning(e.getMessage());
               return null;
            }

            addLocalResources(repository, found, builder, id);

            return builder.create();
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

   private void addAddonDependencies(AddonRepository repository, AddonId found, Builder builder)
            throws ContainerException
   {
      Set<AddonDependencyEntry> addons = repository.getAddonDependencies(found);
      for (AddonDependencyEntry dependency : addons)
      {
         AddonId addonId = manager.resolve(this.currentView.get(), dependency.getName());
         ModuleIdentifier moduleId = null;
         if (addonId != null)
         {
            moduleId = findCompatibleInstalledModule(addonId);
            if (moduleId != null)
            {
               builder.addDependency(DependencySpec.createModuleDependencySpec(
                        PathFilters.not(PathFilters.getMetaInfFilter()),
                        dependency.isExported() ? PathFilters.acceptAll() : PathFilters.rejectAll(),
                        this,
                        moduleCache.getModuleId(this.currentViews.get(), addonId),
                        dependency.isOptional()));
            }
         }

         if (!dependency.isOptional() && (addonId == null || moduleId == null))
            throw new ContainerException("Dependency [" + dependency + "] could not be loaded for addon [" + found
                     + "]");
      }
   }

   private AddonId findInstalledModule(AddonRepository repository, ModuleIdentifier moduleId)
   {
      AddonId found = null;
      List<AddonId> enabled = repository.listEnabledCompatibleWithVersion(AddonRepositoryImpl.getRuntimeAPIVersion());
      for (AddonId addon : enabled)
      {
         if (moduleCache.getModuleId(this.currentViews.get(), addon).equals(moduleId))
         {
            found = addon;
            break;
         }
      }
      return found;
   }

   private ModuleIdentifier findCompatibleInstalledModule(AddonId addonId)
   {
      ModuleIdentifier result = null;

      ALL: for (AddonRepository repository : manager.getRepositories())
      {
         for (AddonId id : repository.listEnabledCompatibleWithVersion(AddonRepositoryImpl.getRuntimeAPIVersion()))
         {
            if (id.getName().equals(addonId.getName()))
            {
               result = moduleCache.getModuleId(this.currentViews.get(), id);
               break ALL;
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

   public void releaseAddonModule(Set<AddonView> views, AddonId addonId)
   {
      ModuleIdentifier id = moduleCache.getModuleId(views, addonId);
      moduleJarFileCache.closeJarFileReferences(id);
      moduleCache.clear(views, addonId);
   }

}
