package org.jboss.forge.furnace.addons;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.forge.furnace.Furnace;
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

   public AddonImpl loadAddon(Set<AddonView> views, AddonId addonId)
   {
      Assert.notNull(addonId, "AddonId to load must not be null.");

      AddonImpl addon = null;

      AddonView view = views.iterator().next();
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
            addon = loadAddonFromRepository(views, view, repository, addonId);
            if (addon != null)
               break;
         }
      }

      if (addon != null)
         manager.add(addon);

      return addon;
   }

   private AddonImpl loadAddonFromRepository(Set<AddonView> views, AddonView view, AddonRepository repository,
            final AddonId addonId)
   {
      AddonImpl addon = null;
      if (repository.isEnabled(addonId) && repository.isDeployed(addonId))
      {
         addon = (AddonImpl) view.getAddon(addonId);

         if (addon == null)
         {
            addon = new AddonImpl(lock, addonId);
            addon.setRepository(repository);
         }

         Set<AddonDependency> dependencies = fromAddonDependencyEntries(views, view, addon,
                  repository.getAddonDependencies(addonId));

         if (addon.getModule() == null)
         {
            Set<AddonDependency> missingRequiredDependencies = new HashSet<AddonDependency>();
            for (AddonDependency addonDependency : dependencies)
            {
               Addon dependency = addonDependency.getDependency();
               if (dependency == null && !addonDependency.isOptional())
               {
                  missingRequiredDependencies.add(addonDependency);
               }
            }

            if (missingRequiredDependencies.isEmpty())
               addon.setMissingDependencies(missingRequiredDependencies);

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
                  Module module = loader.loadModule(views, view, addonId);
                  addon.setModuleLoader(loader);
                  addon.setModule(module);
                  addon.setRepository(repository);
               }
               catch (Exception e)
               {
                  logger.log(Level.FINE, "Failed to load addon [" + addonId + "]", e);
               }
            }
         }

         dependencies.removeAll(addon.getMissingDependencies());
         addon.setDependencies(dependencies);
      }
      return addon;
   }

   private Set<AddonDependency> fromAddonDependencyEntries(Set<AddonView> views, AddonView view, AddonImpl addon,
            Set<AddonDependencyEntry> entries)
   {
      Set<AddonDependency> result = new HashSet<AddonDependency>();
      for (AddonDependencyEntry entry : entries)
      {
         AddonId dependencyId = manager.resolve(view, entry.getName());
         if (dependencyId == null)
         {
            if (!entry.isOptional())
            {
               result.add(new MissingAddonDependencyImpl(entry));
            }
         }
         else
         {
            AddonImpl dependency = loadAddon(views, dependencyId);
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
