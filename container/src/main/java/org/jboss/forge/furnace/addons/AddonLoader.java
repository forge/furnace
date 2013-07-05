package org.jboss.forge.furnace.addons;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.exception.ContainerException;
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
   private AddonLifecycleManager lifecycleManager;
   private AddonStateManager stateManager;
   private AddonModuleLoader loader;

   public AddonLoader(Furnace furnace, AddonLifecycleManager lifecycleManager, AddonStateManager stateManager)
   {
      this.lock = furnace.getLockManager();
      this.lifecycleManager = lifecycleManager;
      this.stateManager = stateManager;
      this.loader = new AddonModuleLoader(furnace, lifecycleManager, stateManager);
   }

   public void loadAddon(Addon addon)
   {
      Assert.notNull(addon, "Addon to load must not be null.");

      if (addon.getStatus().isMissing())
      {
         loader.releaseAddonModule(addon);
         AddonRepository repository =
                  stateManager.getViewsOf(addon).iterator().next().getRepositories().iterator().next();
         if (repository.isEnabled(addon.getId())
                  && repository.isDeployed(addon.getId()))
         {
            Set<AddonDependency> dependencies = fromAddonDependencyEntries(addon,
                     repository.getAddonDependencies(addon.getId()));

            Set<AddonDependency> missingRequiredDependencies = new HashSet<AddonDependency>();
            for (AddonDependency addonDependency : dependencies)
            {
               if (addonDependency instanceof MissingAddonDependencyImpl && !addonDependency.isOptional())
               {
                  missingRequiredDependencies.add(addonDependency);
               }
            }

            if (!missingRequiredDependencies.isEmpty())
            {
               if (stateManager.getMissingDependenciesOf(addon).size() != missingRequiredDependencies.size())
               {
                  logger.warning("Addon [" + addon + "] has [" + missingRequiredDependencies.size()
                           + "] missing dependencies: "
                           + missingRequiredDependencies + " and will be not be loaded until all required"
                           + " dependencies are available.");
               }
               stateManager.setState(addon, new AddonState(missingRequiredDependencies));
            }
            else
            {
               try
               {
                  Module module = loader.loadAddonModule(addon);
                  stateManager.setState(addon, new AddonState(dependencies, repository, module));
               }
               catch (RuntimeException e)
               {
                  logger.log(Level.FINE, "Failed to load addon [" + addon.getId() + "]", e);
                  throw e;
               }
               catch (Exception e)
               {
                  logger.log(Level.FINE, "Failed to load addon [" + addon.getId() + "]", e);
                  throw new ContainerException("Failed to load addon [" + addon.getId() + "]", e);
               }
            }
         }
      }
   }

   private Set<AddonDependency> fromAddonDependencyEntries(Addon addon,
            Set<AddonDependencyEntry> entries)
   {
      Set<AddonDependency> result = new HashSet<AddonDependency>();
      for (AddonDependencyEntry entry : entries)
      {
         Set<AddonView> views = stateManager.getViewsOf(addon);
         AddonId dependencyId = stateManager.resolveAddonId(views, entry.getName());
         if (dependencyId == null)
         {
            if (!entry.isOptional())
            {
               result.add(new MissingAddonDependencyImpl(entry));
            }
         }
         else
         {
            Addon dependency = lifecycleManager.getAddon(views.iterator().next(), dependencyId);
            result.add(new AddonDependencyImpl(lock,
                     dependency,
                     entry.isExported(),
                     entry.isOptional()));
         }
      }
      return result;
   }
}
