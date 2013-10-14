/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.arquillian;

import java.io.File;
import java.util.ArrayList;
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
import org.jboss.forge.furnace.manager.AddonManager;
import org.jboss.forge.furnace.manager.impl.AddonManagerImpl;
import org.jboss.forge.furnace.manager.maven.addon.MavenAddonDependencyResolver;
import org.jboss.forge.furnace.manager.spi.AddonDependencyResolver;
import org.jboss.forge.furnace.repositories.MutableAddonRepository;
import org.jboss.forge.furnace.util.Addons;
import org.jboss.shrinkwrap.api.Archive;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class IsolatedDeploymentStrategy implements DeploymentStrategyType
{

   @Override
   public void beforeTestMethodExecution(Furnace furnace)
   {
   }

   @Override
   public void deploy(Furnace furnace,
            final MutableAddonRepository target,
            final Deployment deployment,
            final Archive<?> archive,
            final AddonId addonToDeploy) throws DeploymentException
   {
      if (archive instanceof ForgeRemoteAddon)
      {
         ForgeRemoteAddon remoteAddon = (ForgeRemoteAddon) archive;
         AddonDependencyResolver resolver = new MavenAddonDependencyResolver();
         AddonManager addonManager = new AddonManagerImpl(furnace, resolver);
         addonManager.install(remoteAddon.getAddonId(), target).perform();
         waitForDeploymentCompletion(furnace, deployment, addonToDeploy);
      }
      else if (archive instanceof ForgeArchive)
      {
         FurnaceUtil.waitForConfigurationRescan(furnace, new Callable<Void>()
         {
            @Override
            public Void call() throws Exception
            {
               deployToRepository(archive, target, addonToDeploy);
               return null;
            }
         });

         waitForDeploymentCompletion(furnace, deployment, addonToDeploy);
      }
      else
      {
         throw new IllegalArgumentException(
                  "Invalid Archive type. Ensure that your @Deployment method returns type 'ForgeArchive'.");
      }
   }

   private void waitForDeploymentCompletion(Furnace furnace, Deployment deployment, final AddonId addonToDeploy)
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
            DeploymentException e = new DeploymentException("AddonDependency " + addonToDeploy
                     + " failed to deploy.");
            deployment.deployedWithError(e);
            throw new DeploymentException("AddonDependency " + addonToDeploy + " failed to deploy.", e);
         }
      }
      catch (Exception e)
      {
         deployment.deployedWithError(e);
         throw new DeploymentException("AddonDependency " + addonToDeploy + " failed to deploy.", e);
      }
   }

   private void deployToRepository(Archive<?> archive, MutableAddonRepository repository, final AddonId addonToDeploy)
   {
      System.out.println("Deploying [" + addonToDeploy + "] to repository [" + repository + "]");
      File destDir = repository.getAddonBaseDir(addonToDeploy);
      destDir.mkdirs();
      ShrinkWrapUtil.toFile(new File(destDir.getAbsolutePath(), archive.getName()), archive);
      ShrinkWrapUtil.unzip(destDir, archive);
      repository.deploy(addonToDeploy, ((ForgeArchive) archive).getAddonDependencies(), new ArrayList<File>());
      repository.enable(addonToDeploy);
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
