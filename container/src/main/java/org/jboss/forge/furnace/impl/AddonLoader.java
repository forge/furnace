package org.jboss.forge.furnace.impl;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonDependency;
import org.jboss.forge.furnace.addons.AddonDependencyImpl;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.addons.AddonView;
import org.jboss.forge.furnace.lock.LockManager;
import org.jboss.forge.furnace.modules.AddonModuleLoader;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.forge.furnace.repositories.AddonRepository;
import org.jboss.forge.furnace.util.Assert;
import org.jboss.modules.Module;

public class AddonLoader
{
   private static final Logger logger = Logger.getLogger(AddonLoader.class.getName());

   private LockManager lock;
   private AddonLifecycleManager manager;

   private AddonModuleLoader loader;

   public AddonLoader(Furnace furnace, AddonLifecycleManager manager)
   {
      this.lock = furnace.getLockManager();
      this.manager = manager;
      this.loader = new AddonModuleLoader(furnace, manager);
   }

   public AddonImpl loadAddon(AddonView view, AddonId addonId)
   {
      Assert.notNull(addonId, "AddonId to load must not be null.");

      AddonImpl addon = null;
      for (Addon existing : view.getAddons())
      {
         if (existing.getId().equals(addonId))
         {
            addon = (AddonImpl) existing;
            break;
         }
      }

      if (addon == null || addon.getStatus().isMissing())
      {
         for (AddonRepository repository : view.getRepositories())
         {
            addon = loadAddonFromRepository(view, repository, addonId);
            if (addon != null)
               break;
         }
      }

      if (addon == null)
      {
         addon = new AddonImpl(lock, addonId);
      }

      return addon;
   }

   private AddonImpl loadAddonFromRepository(AddonView view, AddonRepository repository, final AddonId addonId)
   {
      AddonImpl addon = null;
      if (repository.isEnabled(addonId) && repository.isDeployed(addonId))
      {
         addon = (AddonImpl) view.getAddon(addonId);

         if (addon == null)
         {
            addon = new AddonImpl(lock, addonId);
            addon.setRepository(repository);
            manager.add(addon);
         }

         Set<AddonDependency> dependencies = fromAddonDependencyEntries(view, addon,
                  repository.getAddonDependencies(addonId));
         addon.setDependencies(dependencies);

         if (addon.getModule() == null)
         {
            Set<AddonDependency> missingRequiredDependencies = new HashSet<AddonDependency>();
            for (AddonDependency dependency : addon.getDependencies())
            {
               AddonId dependencyId = dependency.getDependency().getId();

               boolean loaded = false;
               for (Addon a : view.getAddons())
               {
                  if (a.getId().equals(dependencyId) && !a.getStatus().isMissing())
                  {
                     loaded = true;
                  }
               }
               if (!loaded && !dependency.isOptional())
               {
                  missingRequiredDependencies.add(dependency);
               }
            }

            if (!missingRequiredDependencies.isEmpty())
            {
               if (addon.getMissingDependencies().size() != missingRequiredDependencies.size())
               {
                  logger.warning("Addon [" + addon + "] has [" + missingRequiredDependencies.size()
                           + "] missing dependencies: "
                           + missingRequiredDependencies + " and will be not be loaded until all required"
                           + " dependencies are available.");
               }
               addon.setMissingDependencies(missingRequiredDependencies);
            }
            else
            {
               try
               {
                  Module module = loader.loadModule(view, addonId);
                  addon.setModuleLoader(loader);
                  addon.setModule(module);
                  addon.setRepository(repository);
               }
               catch (Exception e)
               {
                  logger.log(Level.FINE, "Failed to load addon [" + addonId + "]", e);
                  // throw new ContainerException("Failed to load addon [" + addonId + "]", e);
               }
            }
         }
      }
      return addon;
   }

   private Set<AddonDependency> fromAddonDependencyEntries(AddonView view, AddonImpl addon,
            Set<AddonDependencyEntry> entries)
   {
      Set<AddonDependency> result = new HashSet<AddonDependency>();
      for (AddonDependencyEntry entry : entries)
      {
         AddonId dependencyId = manager.resolve(view, entry.getName());
         if (dependencyId == null)
         {

         }
         else
         {
            Addon dependency = view.getAddon(dependencyId);
            result.add(new AddonDependencyImpl(lock,
                     addon,
                     dependency.getId().getVersion(),
                     dependency,
                     entry.isExported(),
                     entry.isOptional()));
         }
      }
      return result;
   }
}
