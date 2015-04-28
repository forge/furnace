/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.arquillian;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.deployment.Deployment;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.forge.arquillian.archive.AddonArchiveBase;
import org.jboss.forge.arquillian.archive.AddonDependencyAware;
import org.jboss.forge.arquillian.archive.AddonDeploymentArchive;
import org.jboss.forge.arquillian.archive.RepositoryLocationAware;
import org.jboss.forge.arquillian.impl.ShrinkWrapUtil;
import org.jboss.forge.arquillian.protocol.FurnaceHolder;
import org.jboss.forge.arquillian.protocol.FurnaceProtocolDescription;
import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.impl.FurnaceImpl;
import org.jboss.forge.furnace.impl.util.Files;
import org.jboss.forge.furnace.manager.AddonManager;
import org.jboss.forge.furnace.manager.impl.AddonManagerImpl;
import org.jboss.forge.furnace.manager.maven.MavenContainer;
import org.jboss.forge.furnace.manager.maven.addon.MavenAddonDependencyResolver;
import org.jboss.forge.furnace.manager.spi.AddonDependencyResolver;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
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
public class FurnaceDeployableContainer implements DeployableContainer<FurnaceContainerConfiguration>
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
   private FurnaceContainerConfiguration configuration;

   private static String originalUserSettings;
   private static String originalLocalRepository;
   private static String originalGlobalSettings;

   static
   {
      originalUserSettings = System.getProperty(MavenContainer.ALT_USER_SETTINGS_XML_LOCATION);
      originalLocalRepository = System.getProperty(MavenContainer.ALT_LOCAL_REPOSITORY_LOCATION);
      originalGlobalSettings = System.getProperty(MavenContainer.ALT_GLOBAL_SETTINGS_XML_LOCATION);
   }

   @Override
   public ProtocolMetaData deploy(final Archive<?> archive) throws DeploymentException
   {
      try
      {
         resetMavenSettings();
         Deployment deployment = deploymentInstance.get();
         final AddonId addonToDeploy = getAddonEntry(deployment);

         if (undeploying)
         {
            System.out.println("Cleaning test runtime.");
            undeploying = false;
            cleanup();
         }

         if (archive instanceof AddonDeploymentArchive)
         {
            AddonDeploymentArchive deploymentArchive = (AddonDeploymentArchive) archive;
            AddonDependencyResolver resolver = new MavenAddonDependencyResolver(configuration.getClassifier());
            AddonManager addonManager = new AddonManagerImpl(runnable.furnace, resolver);

            AddonRepository target = selectTargetRepository(deploymentArchive);

            for (DeploymentListener listener : deploymentArchive.getDeploymentListeners())
            {
               listener.preDeploy(furnaceHolder.getFurnace(), archive);
            }

            addonManager.install(deploymentArchive.getAddonId(), target).perform();
            waitForDeploymentCompletion(deployment, addonToDeploy,
                     deploymentArchive.getDeploymentTimeoutQuantity(),
                     deploymentArchive.getDeploymentTimeoutUnit());

            for (DeploymentListener listener : deploymentArchive.getDeploymentListeners())
            {
               listener.postDeploy(furnaceHolder.getFurnace(), archive);
            }
         }
         else if (archive instanceof AddonArchiveBase<?>)
         {
            final MutableAddonRepository target = selectTargetRepository((AddonArchiveBase<?>) archive);

            waitForConfigurationRescan(new Callable<Void>()
            {
               @Override
               public Void call() throws Exception
               {
                  deployToRepository(archive, target, addonToDeploy);
                  return null;
               }
            });

            waitForDeploymentCompletion(deployment, addonToDeploy,
                     ((AddonArchiveBase<?>) archive).getDeploymentTimeoutQuantity(),
                     ((AddonArchiveBase<?>) archive).getDeploymentTimeoutUnit());
         }
         else
         {
            throw new IllegalArgumentException(
                     "Invalid Archive type. Ensure that your @Deployment method returns type 'AddonArchive'.");
         }

         return new ProtocolMetaData().addContext(furnaceHolder);
      }
      catch (Exception e)
      {
         throw new DeploymentException(e.getMessage(), e);
      }
   }

   private void resetMavenSettings()
   {
      if (originalUserSettings != null)
         System.setProperty(MavenContainer.ALT_USER_SETTINGS_XML_LOCATION, originalUserSettings);
      else
         System.clearProperty(MavenContainer.ALT_USER_SETTINGS_XML_LOCATION);

      if (originalLocalRepository != null)
         System.setProperty(MavenContainer.ALT_LOCAL_REPOSITORY_LOCATION, originalLocalRepository);
      else
         System.clearProperty(MavenContainer.ALT_LOCAL_REPOSITORY_LOCATION);

      if (originalGlobalSettings != null)
         System.setProperty(MavenContainer.ALT_GLOBAL_SETTINGS_XML_LOCATION, originalGlobalSettings);
      else
         System.clearProperty(MavenContainer.ALT_GLOBAL_SETTINGS_XML_LOCATION);
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

   private MutableAddonRepository selectTargetRepository(RepositoryLocationAware<?> archive)
   {
      MutableAddonRepository target = repository;
      if (archive instanceof RepositoryLocationAware<?>
               && ((RepositoryLocationAware<?>) archive).getAddonRepository() != null)
      {
         final String repositoryName = ((RepositoryLocationAware<?>) archive).getAddonRepository();
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

   private void waitForDeploymentCompletion(Deployment deployment, final AddonId addonToDeploy, int quantity,
            TimeUnit unit)
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
         Addons.waitUntilStartedOrMissing(addon, quantity, unit);
      }
      catch (Exception e)
      {
         deployment.deployedWithError(e);
         throw new DeploymentException("AddonDependency " + addonToDeploy + " failed to deploy.", e);
      }
      if (addon.getStatus().isFailed())
      {
         DeploymentException e = new DeploymentException("AddonDependency " + addonToDeploy + " failed to deploy.");
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

      if (archive instanceof AddonDependencyAware)
      {
         repository.deploy(addonToDeploy,
                  ((AddonDependencyAware<?>) archive).getAddonDependencies(),
                  Collections.<File> emptyList());
      }
      else
      {
         repository.deploy(addonToDeploy,
                  Collections.<AddonDependencyEntry> emptyList(),
                  Collections.<File> emptyList());
      }

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
   public Class<FurnaceContainerConfiguration> getConfigurationClass()
   {
      return FurnaceContainerConfiguration.class;
   }

   @Override
   public ProtocolDescription getDefaultProtocol()
   {
      return new FurnaceProtocolDescription();
   }

   @Override
   public void setup(FurnaceContainerConfiguration configuration)
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
         if (archive instanceof AddonDeploymentArchive)
         {
            for (DeploymentListener listener : ((AddonDeploymentArchive) archive).getDeploymentListeners())
            {
               listener.preUndeploy(furnaceHolder.getFurnace(), archive);
            }
         }

         Addon addonToStop = registry.getAddon(addonToUndeploy);
         if (addonToStop.getStatus().isLoaded())
            ((MutableAddonRepository) addonToStop.getRepository()).disable(addonToUndeploy);
         Addons.waitUntilStopped(addonToStop);

         if (archive instanceof AddonDeploymentArchive)
         {
            for (DeploymentListener listener : ((AddonDeploymentArchive) archive).getDeploymentListeners())
            {
               listener.postUndeploy(furnaceHolder.getFurnace(), archive);
            }
         }
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
                  System.setProperty(FurnaceImpl.TEST_MODE_PROPERTY, "true");

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
