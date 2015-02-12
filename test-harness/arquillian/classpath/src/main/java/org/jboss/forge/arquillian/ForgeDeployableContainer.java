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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.deployment.Deployment;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.forge.arquillian.archive.ForgeRemoteAddon;
import org.jboss.forge.arquillian.archive.RepositoryForgeArchive;
import org.jboss.forge.arquillian.protocol.ForgeProtocolDescription;
import org.jboss.forge.arquillian.protocol.FurnaceHolder;
import org.jboss.forge.arquillian.util.ShrinkWrapUtil;
import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.impl.FurnaceImpl;
import org.jboss.forge.furnace.impl.util.Files;
import org.jboss.forge.furnace.manager.AddonManager;
import org.jboss.forge.furnace.manager.impl.AddonManagerImpl;
import org.jboss.forge.furnace.manager.maven.addon.MavenAddonDependencyResolver;
import org.jboss.forge.furnace.manager.spi.AddonDependencyResolver;
import org.jboss.forge.furnace.repositories.AddonRepository;
import org.jboss.forge.furnace.repositories.AddonRepositoryMode;
import org.jboss.forge.furnace.repositories.MutableAddonRepository;
import org.jboss.forge.furnace.spi.ContainerLifecycleListener;
import org.jboss.forge.furnace.spi.ListenerRegistration;
import org.jboss.forge.furnace.util.Addons;
import org.jboss.forge.furnace.util.Callables;
import org.jboss.forge.furnace.util.ClassLoaders;
import org.jboss.forge.furnace.util.OperatingSystemUtils;
import org.jboss.forge.furnace.util.SecurityActions;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class ForgeDeployableContainer implements DeployableContainer<ForgeContainerConfiguration>
{
   @Inject
   private Instance<Deployment> deploymentInstance;

   private final FurnaceHolder furnaceHolder = new FurnaceHolder();
   private ForgeRunnable runnable;
   private File addonDir;

   private MutableAddonRepository repository;
   private final Map<String, MutableAddonRepository> deploymentRepositories = new ConcurrentHashMap<String, MutableAddonRepository>();

   private final Map<Deployment, AddonId> deployedAddons = new HashMap<Deployment, AddonId>();
   private Thread thread;

   private boolean undeploying = false;
   private ForgeContainerConfiguration configuration;

   @Override
   public ProtocolMetaData deploy(final Archive<?> archive) throws DeploymentException
   {

      Deployment deployment = deploymentInstance.get();
      final AddonId addonToDeploy = getAddonEntry(deployment);

      if (undeploying)
      {
         System.out.println("Cleaning test runtime.");
         undeploying = false;
         cleanup();
      }

      if (archive instanceof ForgeArchive)
      {
         final MutableAddonRepository target = selectTargetRepository(archive);

         waitForConfigurationRescan(new Callable<Void>()
         {
            @Override
            public Void call() throws Exception
            {
               deployToRepository(archive, target, addonToDeploy);
               return null;
            }
         });

         waitForDeploymentCompletion(deployment, addonToDeploy);
      }
      else if (archive instanceof ForgeRemoteAddon)
      {
         ForgeRemoteAddon remoteAddon = (ForgeRemoteAddon) archive;
         AddonDependencyResolver resolver = new MavenAddonDependencyResolver(configuration.getClassifier());
         AddonManager addonManager = new AddonManagerImpl(runnable.furnace, resolver);

         AddonRepository target = selectTargetRepository(archive);
         addonManager.install(remoteAddon.getAddonId(), target).perform();

         waitForDeploymentCompletion(deployment, addonToDeploy);
      }
      else
      {
         throw new IllegalArgumentException(
                  "Invalid Archive type. Ensure that your @Deployment method returns type 'ForgeArchive'.");
      }

      return new ProtocolMetaData().addContext(furnaceHolder);
   }

   private <T> T waitForConfigurationRescan(Callable<T> action)
   {

      ConfigurationScanListener listener = new ConfigurationScanListener();
      ListenerRegistration<ContainerLifecycleListener> registration = runnable.furnace
               .addContainerLifecycleListener(listener);

      T result = Callables.call(action);

      while (runnable.furnace.getStatus().isStarting() || !listener.isConfigurationScanned())
      {
         try
         {
            Thread.sleep(100);
         }
         catch (InterruptedException e)
         {
            throw new RuntimeException("Sleep interrupted while waiting for configuration rescan.", e);
         }
      }

      registration.removeListener();

      return result;
   }

   private MutableAddonRepository selectTargetRepository(Archive<?> archive)
   {
      MutableAddonRepository target = repository;
      if (archive instanceof RepositoryForgeArchive
               && ((RepositoryForgeArchive) archive).getAddonRepository() != null)
      {
         final String repositoryName = ((RepositoryForgeArchive) archive).getAddonRepository();
         if (deploymentRepositories.get(repositoryName) == null)
         {
            target = waitForConfigurationRescan(new Callable<MutableAddonRepository>()
            {
               @Override
               public MutableAddonRepository call() throws Exception
               {
                  return (MutableAddonRepository) runnable.furnace.addRepository(AddonRepositoryMode.MUTABLE,
                           new File(addonDir, OperatingSystemUtils.getSafeFilename(repositoryName)));
               }
            });
            deploymentRepositories.put(repositoryName, target);
         }
         else
            target = deploymentRepositories.get(repositoryName);
      }
      return target;
   }

   private void waitForDeploymentCompletion(Deployment deployment, final AddonId addonToDeploy)
            throws DeploymentException
   {
      AddonRegistry registry = runnable.getForge().getAddonRegistry();
      Addon addon = registry.getAddon(addonToDeploy);
      try
      {
         Future<Void> future = addon.getFuture();
         if (!future.isDone())
         {
            future.get();
         }
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
      File destDir = repository.getAddonBaseDir(addonToDeploy);
      destDir.mkdirs();
      ShrinkWrapUtil.toFile(new File(destDir.getAbsolutePath(), archive.getName()), archive);
      ShrinkWrapUtil.unzip(destDir, archive);
      System.out.println("Furnace test harness is deploying [" + addonToDeploy + "] to repository [" + repository
               + "] ...");
      repository.deploy(addonToDeploy, ((ForgeArchive) archive).getAddonDependencies(), new ArrayList<File>());
      repository.enable(addonToDeploy);
   }

   private void cleanup()
   {
      try
      {
         stop();
         start();
      }
      catch (LifecycleException e)
      {
         throw new RuntimeException("Failed to clean up after test case.", e);
      }
   }

   @Override
   public void deploy(Descriptor descriptor) throws DeploymentException
   {
      throw new UnsupportedOperationException("Descriptors not supported by Furnace");
   }

   private AddonId getAddonEntry(Deployment deployment)
   {
      if (!deployedAddons.containsKey(deployment))
      {
         String[] coordinates = deployment.getDescription().getName().split(",");
         AddonId entry;
         if (coordinates.length == 3)
            entry = AddonId.from(coordinates[0], coordinates[1], coordinates[2]);
         else if (coordinates.length == 2)
            entry = AddonId.from(coordinates[0], coordinates[1]);
         else if (coordinates.length == 1)
            entry = AddonId.from(coordinates[0], UUID.randomUUID().toString());
         else
            entry = AddonId.from(UUID.randomUUID().toString(), UUID.randomUUID().toString());

         deployedAddons.put(deployment, entry);
      }
      return deployedAddons.get(deployment);
   }

   @Override
   public Class<ForgeContainerConfiguration> getConfigurationClass()
   {
      return ForgeContainerConfiguration.class;
   }

   @Override
   public ProtocolDescription getDefaultProtocol()
   {
      return new ForgeProtocolDescription();
   }

   @Override
   public void setup(ForgeContainerConfiguration configuration)
   {
      this.configuration = configuration;
   }

   @Override
   public void start() throws LifecycleException
   {
      try
      {
         this.addonDir = OperatingSystemUtils.createTempDir();
      }
      catch (IllegalStateException e)
      {
         throw new LifecycleException("Failed to create temporary addon directory", e);
      }

      try
      {
         System.out.println("Furnace test harness is initializing with addon dir [" + addonDir + "]");
         initContainer();
         startContainer();
      }
      catch (Exception e)
      {
         throw new LifecycleException("Could not start Furnace runnable.", e);
      }
   }

   private void startContainer()
   {
      waitForConfigurationRescan(new Callable<Void>()
      {
         @Override
         public Void call() throws Exception
         {
            thread.start();
            return null;
         }
      });
   }

   private void initContainer()
   {
      runnable = new ForgeRunnable(ClassLoader.getSystemClassLoader());
      furnaceHolder.setFurnace(runnable.furnace);
      thread = new Thread(runnable, "Arquillian Furnace Runtime");
      repository = (MutableAddonRepository) runnable.furnace.addRepository(AddonRepositoryMode.MUTABLE, addonDir);
   }

   @Override
   public void stop() throws LifecycleException
   {
      stopContainer();
      Files.delete(addonDir, true);
   }

   private void stopContainer()
   {
      this.repository = null;
      this.deployedAddons.clear();
      this.deploymentRepositories.clear();
      this.runnable.stop();
      this.thread = null;
   }

   @Override
   public void undeploy(Archive<?> archive) throws DeploymentException
   {
      undeploying = true;
      AddonId addonToUndeploy = getAddonEntry(deploymentInstance.get());
      AddonRegistry registry = runnable.getForge().getAddonRegistry();
      System.out.println("Furnace test harness is undeploying [" + addonToUndeploy + "] ... ");

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

   @Override
   public void undeploy(Descriptor descriptor) throws DeploymentException
   {
      throw new UnsupportedOperationException("Descriptors not supported by Furnace");
   }

   private class ForgeRunnable implements Runnable
   {
      private final Furnace furnace;
      private final ClassLoader loader;

      public ForgeRunnable(ClassLoader loader)
      {
         this.furnace = new FurnaceImpl();
         this.loader = loader;
      }

      public Furnace getForge()
      {
         return furnace;
      }

      @Override
      public void run()
      {
         try
         {
            ClassLoaders.executeIn(loader, new Callable<Object>()
            {
               @Override
               public Object call() throws Exception
               {
                  System.setProperty("org.jboss.forge.furnace.test", "true");

                  furnace.setServerMode(true);
                  furnace.start(loader);

                  SecurityActions.cleanupThreadLocals(thread);
                  return null;
               }
            });
         }
         catch (Exception e)
         {
            throw new RuntimeException("Failed to start Furnace container.", e);
         }
      }

      public void stop()
      {
         furnace.stop();
      }
   }

   @Override
   public String toString()
   {
      String result = "Furnace: " + runnable.furnace.hashCode() + "\nStatus: " + runnable.furnace.getStatus() + "\n\n";
      for (AddonRepository repository : runnable.furnace.getRepositories())
      {
         result += repository + "\n";
      }
      result += "\n" + runnable.furnace;
      return result;
   }
}
