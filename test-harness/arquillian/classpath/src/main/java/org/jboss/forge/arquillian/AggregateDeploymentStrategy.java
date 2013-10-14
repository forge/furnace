/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.arquillian;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.deployment.Deployment;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.forge.arquillian.archive.ForgeRemoteAddon;
import org.jboss.forge.arquillian.util.FurnaceUtil;
import org.jboss.forge.arquillian.util.ShrinkWrapUtil;
import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.lock.LockMode;
import org.jboss.forge.furnace.manager.AddonManager;
import org.jboss.forge.furnace.manager.impl.AddonManagerImpl;
import org.jboss.forge.furnace.manager.maven.addon.MavenAddonDependencyResolver;
import org.jboss.forge.furnace.manager.request.InstallRequest;
import org.jboss.forge.furnace.manager.spi.AddonDependencyResolver;
import org.jboss.forge.furnace.manager.spi.AddonInfo;
import org.jboss.forge.furnace.repositories.AddonRepository;
import org.jboss.forge.furnace.repositories.MutableAddonRepository;
import org.jboss.forge.furnace.util.Addons;
import org.jboss.shrinkwrap.api.Archive;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class AggregateDeploymentStrategy implements DeploymentStrategyType
{
   boolean enabled = false;
   private Map<MutableAddonRepository, Set<AddonId>> deployed = new HashMap<MutableAddonRepository, Set<AddonId>>();

   @Override
   public void beforeTestMethodExecution(final Furnace furnace) throws DeploymentException
   {
      if (!enabled)
      {
         enabled = true;
         FurnaceUtil.waitForConfigurationRescan(furnace, new Callable<Void>()
         {
            @Override
            public Void call() throws Exception
            {
               return furnace.getLockManager().performLocked(LockMode.WRITE, new Callable<Void>()
               {
                  @Override
                  public Void call() throws Exception
                  {
                     for (AddonRepository repo : furnace.getRepositories())
                     {
                        if (repo instanceof MutableAddonRepository)
                        {
                           Set<AddonId> addons = getAddonsFor((MutableAddonRepository) repo);
                           for (AddonId addonId : addons)
                           {
                              ((MutableAddonRepository) repo).enable(addonId);
                           }
                        }
                     }
                     return null;
                  }
               });
            }
         });

         for (AddonRepository repo : deployed.keySet())
         {
            if (repo instanceof MutableAddonRepository)
            {
               Set<AddonId> addons = getAddonsFor((MutableAddonRepository) repo);
               for (AddonId addonId : addons)
               {
                  waitForDeploymentCompletion(furnace, addonId);
               }
            }
         }
      }
   }

   private Set<AddonId> getAddonsFor(MutableAddonRepository repo)
   {
      if (deployed.get(repo) == null)
         deployed.put(repo, new HashSet<AddonId>());
      return deployed.get(repo);
   }

   private void waitForDeploymentCompletion(Furnace furnace, final AddonId addonToDeploy)
            throws DeploymentException
   {
      AddonRegistry registry = furnace.getAddonRegistry();
      Addon addon = registry.getAddon(addonToDeploy);
      try
      {
         Future<Void> future = addon.getFuture();
         future.get();
         if (addon.getStatus().isFailed())
         {
            throw new DeploymentException("AddonDependency " + addonToDeploy + " failed to deploy.");
         }
      }
      catch (Exception e)
      {
         throw new DeploymentException("AddonDependency " + addonToDeploy + " failed to deploy.", e);
      }
   }

   @Override
   public void deploy(Furnace furnace,
            final MutableAddonRepository repository,
            final Deployment deployment,
            final Archive<?> archive,
            final AddonId addonToDeploy) throws DeploymentException
   {
      if (archive instanceof ForgeArchive)
      {
         System.out.println("Deploying [" + addonToDeploy + "] to repository [" + repository + "]");
         File destDir = repository.getAddonBaseDir(addonToDeploy);
         destDir.mkdirs();
         ShrinkWrapUtil.toFile(new File(destDir.getAbsolutePath(), archive.getName()), archive);
         ShrinkWrapUtil.unzip(destDir, archive);
         repository.deploy(addonToDeploy, ((ForgeArchive) archive).getAddonDependencies(), new ArrayList<File>());
         addDeployedAddon(repository, addonToDeploy);
      }
      else if (archive instanceof ForgeRemoteAddon)
      {
         ForgeRemoteAddon remoteAddon = (ForgeRemoteAddon) archive;
         AddonDependencyResolver resolver = new MavenAddonDependencyResolver();
         AddonManager addonManager = new AddonManagerImpl(furnace, resolver);
         InstallRequest request = addonManager.install(remoteAddon.getAddonId(), repository);
         System.setProperty("forge.repo.skip_enable", "true");
         try
         {
            request.perform();
         }
         finally
         {
            System.setProperty("forge.repo.skip_enable", "false");
         }
         addDeployedAddons(repository, request.getRequestedAddonInfo());
      }
      else
      {
         throw new IllegalArgumentException(
                  "Invalid Archive type. Ensure that your @Deployment method returns type 'ForgeArchive'.");
      }
   }

   private void addDeployedAddon(final MutableAddonRepository repository, final AddonId addonToDeploy)
   {
      getAddonsFor(repository).add(addonToDeploy);
   }

   private void addDeployedAddons(MutableAddonRepository repository, AddonInfo info)
   {
      Set<AddonId> addons = getAddonsFor(repository);
      for (AddonInfo i : info.getOptionalAddons())
      {
         addDeployedAddons(repository, i);
      }
      for (AddonInfo i : info.getRequiredAddons())
      {
         addDeployedAddons(repository, i);
      }
      addons.add(info.getAddon());
   }

   @Override
   public void undeploy(Furnace furnace, MutableAddonRepository repository, AddonId addonToUndeploy)
            throws DeploymentException
   {
      AddonRegistry registry = furnace.getAddonRegistry();
      System.out.println("Undeploying [" + addonToUndeploy + "] ... ");

      try
      {
         Addon addonToStop = registry.getAddon(addonToUndeploy);
         if (addonToStop.getStatus().isLoaded())
            ((MutableAddonRepository) addonToStop.getRepository()).disable(addonToUndeploy);
         Addons.waitUntilStopped(addonToStop);
      }
      catch (Exception e)
      {
         throw new DeploymentException("Failed to undeploy " + addonToUndeploy, e);
      }
      finally
      {
         repository.undeploy(addonToUndeploy);
      }
   }

}
