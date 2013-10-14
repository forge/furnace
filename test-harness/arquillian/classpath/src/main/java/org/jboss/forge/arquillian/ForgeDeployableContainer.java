/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.arquillian;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.deployment.Deployment;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.forge.arquillian.archive.DeploymentTypeSelector;
import org.jboss.forge.arquillian.archive.RepositorySelector;
import org.jboss.forge.arquillian.protocol.ForgeProtocolDescription;
import org.jboss.forge.arquillian.protocol.FurnaceHolder;
import org.jboss.forge.arquillian.util.FurnaceUtil;
import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.impl.FurnaceImpl;
import org.jboss.forge.furnace.impl.util.Files;
import org.jboss.forge.furnace.repositories.AddonRepository;
import org.jboss.forge.furnace.repositories.AddonRepositoryMode;
import org.jboss.forge.furnace.repositories.MutableAddonRepository;
import org.jboss.forge.furnace.util.ClassLoaders;
import org.jboss.forge.furnace.util.OperatingSystemUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class ForgeDeployableContainer implements DeployableContainer<ForgeContainerConfiguration>
{
   @Inject
   private Instance<Deployment> deploymentInstance;

   private FurnaceHolder furnaceHolder = new FurnaceHolder();
   private ForgeRunnable runnable;
   private File addonDir;

   private MutableAddonRepository repository;
   private Map<String, MutableAddonRepository> deploymentRepositories = new ConcurrentHashMap<String, MutableAddonRepository>();

   private Map<Deployment, AddonId> deployedAddons = new HashMap<Deployment, AddonId>();
   private Thread thread;

   private boolean undeploying = false;

   @Override
   public ProtocolMetaData deploy(final Archive<?> archive) throws DeploymentException
   {
      Deployment deployment = deploymentInstance.get();
      final AddonId addonToDeploy = getAddonEntry(deployment);

      if (undeploying)
      {
         undeploying = false;
         cleanup();
      }

      final MutableAddonRepository target = selectTargetRepository(archive);
      DeploymentStrategyType strategy = selectDeploymentStrategy(archive);
      strategy.deploy(runnable.furnace, target, deployment, archive, addonToDeploy);

      return new ProtocolMetaData().addContext(furnaceHolder);
   }

   private DeploymentStrategyType selectDeploymentStrategy(Archive<?> archive)
   {
      Strategy strategyType = ((DeploymentTypeSelector) archive).getDeploymentStrategyType();
      DeploymentStrategyType result = null;
      switch (strategyType)
      {
      case ISOLATED:
         result = new IsolatedDeploymentStrategy();
         break;

      case AGGREGATE:
         result = new AggregateDeploymentStrategy();
         break;

      default:
         break;
      }

      if (furnaceHolder.getDeploymentStrategy() == null)
         furnaceHolder.setDeploymentStrategy(result);

      return furnaceHolder.getDeploymentStrategy();
   }

   private MutableAddonRepository selectTargetRepository(Archive<?> archive)
   {
      MutableAddonRepository target = repository;
      if (archive instanceof RepositorySelector
               && ((RepositorySelector) archive).getAddonRepository() != null)
      {
         String repositoryName = ((RepositorySelector) archive).getAddonRepository();
         if (deploymentRepositories.get(repositoryName) == null)
         {
            stopContainer();
            initContainer();
            for (String name : deploymentRepositories.keySet())
            {
               MutableAddonRepository repository = (MutableAddonRepository) runnable.furnace.addRepository(
                        AddonRepositoryMode.MUTABLE,
                        new File(addonDir, OperatingSystemUtils.getSafeFilename(name)));
               deploymentRepositories.put(name, repository);
            }
            target = (MutableAddonRepository) runnable.furnace.addRepository(AddonRepositoryMode.MUTABLE,
                     new File(addonDir, OperatingSystemUtils.getSafeFilename(repositoryName)));
            deploymentRepositories.put(repositoryName, target);
            startContainer();
         }
         else
            target = deploymentRepositories.get(repositoryName);
      }
      return target;
   }

   private void cleanup()
   {
      try
      {
         deploymentRepositories.clear();
         furnaceHolder.setDeploymentStrategy(null);
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
   }

   @Override
   public void start() throws LifecycleException
   {
      try
      {
         this.addonDir = File.createTempFile("furnace", "test-addon-dir");
         System.out.println("Executing test case with addon dir [" + addonDir + "]");
         initContainer();
         startContainer();
      }
      catch (IOException e)
      {
         throw new LifecycleException("Failed to create temporary addon directory", e);
      }
      catch (Exception e)
      {
         throw new LifecycleException("Could not start Furnace runnable.", e);
      }
   }

   private void startContainer()
   {
      FurnaceUtil.waitForConfigurationRescan(runnable.furnace, new Callable<Void>()
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
      this.runnable.stop();
   }

   @Override
   public void undeploy(Archive<?> archive) throws DeploymentException
   {
      undeploying = true;
      AddonId addonToUndeploy = getAddonEntry(deploymentInstance.get());
      DeploymentStrategyType strategy = selectDeploymentStrategy(archive);
      strategy.undeploy(runnable.getForge(), repository, addonToUndeploy);
   }

   @Override
   public void undeploy(Descriptor descriptor) throws DeploymentException
   {
      throw new UnsupportedOperationException("Descriptors not supported by Furnace");
   }

   private class ForgeRunnable implements Runnable
   {
      private Furnace furnace;
      private ClassLoader loader;

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
                  furnace.setServerMode(true);
                  furnace.start(loader);
                  return furnace;
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
