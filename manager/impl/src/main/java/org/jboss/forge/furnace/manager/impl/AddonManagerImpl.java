/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.manager.impl;

import static org.jboss.forge.furnace.manager.impl.request.AddonActionRequestFactory.createDeployRequest;
import static org.jboss.forge.furnace.manager.impl.request.AddonActionRequestFactory.createDisableRequest;
import static org.jboss.forge.furnace.manager.impl.request.AddonActionRequestFactory.createEnableRequest;
import static org.jboss.forge.furnace.manager.impl.request.AddonActionRequestFactory.createInstallRequest;
import static org.jboss.forge.furnace.manager.impl.request.AddonActionRequestFactory.createRemoveRequest;
import static org.jboss.forge.furnace.manager.impl.request.AddonActionRequestFactory.createUpdateRequest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.addons.AddonView;
import org.jboss.forge.furnace.lock.LockMode;
import org.jboss.forge.furnace.manager.AddonManager;
import org.jboss.forge.furnace.manager.request.AddonActionRequest;
import org.jboss.forge.furnace.manager.request.DeployRequest;
import org.jboss.forge.furnace.manager.request.DisableRequest;
import org.jboss.forge.furnace.manager.request.EnableRequest;
import org.jboss.forge.furnace.manager.request.InstallRequest;
import org.jboss.forge.furnace.manager.request.RemoveRequest;
import org.jboss.forge.furnace.manager.spi.AddonDependencyResolver;
import org.jboss.forge.furnace.manager.spi.AddonInfo;
import org.jboss.forge.furnace.repositories.AddonRepository;
import org.jboss.forge.furnace.repositories.MutableAddonRepository;
import org.jboss.forge.furnace.util.Assert;
import org.jboss.forge.furnace.versions.SingleVersion;
import org.jboss.forge.furnace.versions.Version;
import org.jboss.forge.furnace.versions.Versions;

/**
 * The {@link AddonManager} implementation
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class AddonManagerImpl implements AddonManager
{
   private final Furnace furnace;
   private final AddonDependencyResolver resolver;
   private final AddonView addonView;

   public AddonManagerImpl(final Furnace furnace, final AddonDependencyResolver resolver)
   {
      this.furnace = furnace;
      this.resolver = resolver;
      this.addonView = null;
   }

   public AddonManagerImpl(final Furnace forge, final AddonDependencyResolver resolver, final AddonView addonView)
   {
      this.furnace = forge;
      this.resolver = resolver;
      this.addonView = addonView;
   }

   @Override
   public AddonInfo info(final AddonId addonId)
   {
      return resolver.resolveAddonDependencyHierarchy(addonId);
   }

   @Override
   public InstallRequest install(final AddonId addonId)
   {
      return install(addonId, getDefaultRepository());
   }

   @Override
   public InstallRequest install(final AddonId addonId, final AddonRepository repository)
   {
      MutableAddonRepository mutableRepo = assertMutableRepository(repository);
      AddonInfo addonInfo = info(addonId);
      List<AddonInfo> allAddons = collectRequiredAddons(addonInfo);
      Map<AddonId, AddonRepository> installedAddonIds = getInstalledAddons();
      List<AddonActionRequest> actions = new ArrayList<>();
      for (AddonInfo newAddonInfo : allAddons)
      {
         AddonActionRequest request = createRequest(addonInfo, newAddonInfo, mutableRepo, installedAddonIds);
         if (request != null)
         {
            actions.add(request);
         }
      }
      return createInstallRequest(addonInfo, actions);
   }

   @Override
   public DeployRequest deploy(AddonId id)
   {
      return deploy(id, getDefaultRepository());
   }

   @Override
   public DeployRequest deploy(AddonId id, AddonRepository repository)
   {
      MutableAddonRepository mutableRepo = assertMutableRepository(repository);
      return createDeployRequest(info(id), mutableRepo, furnace);
   }

   @Override
   public RemoveRequest remove(final AddonId id)
   {
      return remove(id, getDefaultRepository());
   }

   @Override
   public RemoveRequest remove(final AddonId id, final AddonRepository repository)
   {
      AddonInfo info = new ShallowAddonInfo(id);
      return createRemoveRequest(info, assertMutableRepository(repository), furnace);
   }

   @Override
   public DisableRequest disable(final AddonId id)
   {
      return disable(id, getDefaultRepository());
   }

   @Override
   public DisableRequest disable(final AddonId id, final AddonRepository repository)
   {
      AddonInfo info = new ShallowAddonInfo(id);
      return createDisableRequest(info, assertMutableRepository(repository), furnace);
   }

   @Override
   public EnableRequest enable(final AddonId id)
   {
      return enable(id, getDefaultRepository());
   }

   @Override
   public EnableRequest enable(final AddonId id, final AddonRepository repository)
   {
      AddonInfo info = new ShallowAddonInfo(id);
      return createEnableRequest(info, assertMutableRepository(repository), furnace);
   }

   /**
    * Calculate the necessary request based in the list of installed addons for a given {@link MutableAddonRepository}
    *
    * @param addonInfo
    * @param repository
    * @param installedAddons
    * @return
    */
   private AddonActionRequest createRequest(final AddonInfo requestedAddonInfo, final AddonInfo addonInfo,
            final MutableAddonRepository repository,
            final Map<AddonId, AddonRepository> installedAddons)
   {
      final AddonActionRequest request;
      AddonId addon = addonInfo.getAddon();
      if (installedAddons.containsKey(addon))
      {
         // Already contains the installed addon. Update ONLY if the version is SNAPSHOT and if it is the requested
         // addon
         if (Versions.isSnapshot(addon.getVersion()) && addonInfo.equals(requestedAddonInfo))
         {
            AddonRepository addonRepository = installedAddons.get(addon);
            if (repository.equals(addonRepository))
            {
               request = createUpdateRequest(addonInfo, addonInfo, repository, furnace);
            }
            else
            {
               request = createDeployRequest(addonInfo, repository, furnace);
            }
         }
         else
         {
            request = null;
         }
      }
      else
      {
         // Addon is not installed or has a different version
         Entry<AddonId, AddonRepository> differentVersionEntry = null;
         for (Entry<AddonId, AddonRepository> addonEntry : installedAddons.entrySet())
         {
            AddonId addonId = addonEntry.getKey();
            if (addonId.getName().equals(addon.getName()))
            {
               differentVersionEntry = addonEntry;
               break;
            }
         }
         if (differentVersionEntry != null)
         {
            // Avoiding ClassCastExceptions
            Version differentVersion = SingleVersion.valueOf(differentVersionEntry.getKey().getVersion().toString());
            Version addonVersion = SingleVersion.valueOf(addon.getVersion().toString());
            // TODO: Review condition below
            if (differentVersion.compareTo(addonVersion) < 0)
            {
               if (repository.equals(differentVersionEntry.getValue()))
               {
                  request = createUpdateRequest(info(differentVersionEntry.getKey()), addonInfo, repository, furnace);
               }
               else
               {
                  request = createDeployRequest(addonInfo, repository, furnace);
               }
            }
            else
            {
               // No update needed. Don't do anything with it
               request = null;
            }
         }
         else
         {
            request = createDeployRequest(addonInfo, repository, furnace);
         }
      }
      return request;
   }

   private List<AddonInfo> collectRequiredAddons(final AddonInfo addonInfo)
   {
      return furnace.getLockManager().performLocked(LockMode.READ, new Callable<List<AddonInfo>>()
      {
         @Override
         public List<AddonInfo> call() throws Exception
         {
            List<AddonInfo> allAddons = new LinkedList<>();
            collectRequiredAddons(addonInfo, allAddons);
            return allAddons;
         }
      });
   }

   /**
    * Collect all required addons for a specific addon.
    *
    * It traverses the whole graph
    *
    * @param addonInfo
    * @param addons
    */
   private void collectRequiredAddons(AddonInfo addonInfo, List<AddonInfo> addons)
   {
      // Move this addon to the top of the list
      addons.remove(addonInfo);
      addons.add(0, addonInfo);
      for (AddonId addonId : addonInfo.getRequiredAddons())
      {
         if (!addons.contains(addonId) && (!isDeployed(addonId) || !isEnabled(addonId)))
         {
            AddonInfo childInfo = info(addonId);
            collectRequiredAddons(childInfo, addons);
         }
      }
   }

   private MutableAddonRepository getDefaultRepository()
   {
      for (AddonRepository repo : getRepositories())
      {
         if (repo instanceof MutableAddonRepository)
            return (MutableAddonRepository) repo;
      }
      throw new IllegalStateException(
               "No default mutable repository found in Furnace instance. Have you added one using furnace.addRepository(AddonRepositoryMode.MUTABLE, repository) ?");
   }

   private MutableAddonRepository assertMutableRepository(AddonRepository repository)
   {
      Assert.isTrue(repository instanceof MutableAddonRepository, "Addon repository ["
               + repository.getRootDirectory().getAbsolutePath()
               + "] is not writable.");
      return (MutableAddonRepository) repository;
   }

   /**
    * Returns a {@link Map} of the installed addons with the a key of {@link AddonId} and the value of
    * {@link AddonRepository} indicating in which repository the addon is installed.
    */
   private Map<AddonId, AddonRepository> getInstalledAddons()
   {
      Map<AddonId, AddonRepository> addons = new HashMap<>();
      for (AddonRepository repository : getRepositories())
      {
         List<AddonId> listEnabled = repository.listAll();
         for (AddonId addonId : listEnabled)
         {
            addons.put(addonId, repository);
         }
      }
      return addons;
   }

   private boolean isDeployed(AddonId addonId)
   {
      for (AddonRepository repository : getRepositories())
      {
         if (repository.isDeployed(addonId))
            return true;
      }
      return false;
   }

   private boolean isEnabled(AddonId addonId)
   {
      for (AddonRepository repository : getRepositories())
      {
         if (repository.isEnabled(addonId))
            return true;
      }
      return false;
   }

   private Collection<AddonRepository> getRepositories()
   {
      return (addonView == null) ? furnace.getRepositories() : addonView.getRepositories();
   }
}
