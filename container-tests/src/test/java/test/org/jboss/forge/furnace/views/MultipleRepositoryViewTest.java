/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package test.org.jboss.forge.furnace.views;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.forge.arquillian.ConfigurationScanListener;
import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.impl.util.Files;
import org.jboss.forge.furnace.manager.AddonManager;
import org.jboss.forge.furnace.manager.impl.AddonManagerImpl;
import org.jboss.forge.furnace.manager.maven.MavenContainer;
import org.jboss.forge.furnace.manager.maven.addon.MavenAddonDependencyResolver;
import org.jboss.forge.furnace.manager.spi.AddonDependencyResolver;
import org.jboss.forge.furnace.repositories.AddonRepository;
import org.jboss.forge.furnace.repositories.AddonRepositoryMode;
import org.jboss.forge.furnace.se.FurnaceFactory;
import org.jboss.forge.furnace.spi.ContainerLifecycleListener;
import org.jboss.forge.furnace.spi.ListenerRegistration;
import org.jboss.forge.furnace.util.Addons;
import org.jboss.forge.furnace.util.OperatingSystemUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class MultipleRepositoryViewTest
{
   File leftRepo;
   File rightRepo;

   private static String previousUserSettings;
   private static String previousLocalRepository;

   @BeforeClass
   public static void setRemoteRepository() throws IOException
   {
      previousUserSettings = System.setProperty(MavenContainer.ALT_USER_SETTINGS_XML_LOCATION,
               getAbsolutePath("profiles/settings.xml"));
      previousLocalRepository = System.setProperty(MavenContainer.ALT_LOCAL_REPOSITORY_LOCATION,
               "target/the-other-repository");
   }

   private static String getAbsolutePath(String path) throws FileNotFoundException
   {
      URL resource = Thread.currentThread().getContextClassLoader().getResource(path);
      if (resource == null)
         throw new FileNotFoundException(path);
      return resource.getFile();
   }

   @AfterClass
   public static void clearRemoteRepository()
   {
      if (previousUserSettings == null)
      {
         System.clearProperty(MavenContainer.ALT_USER_SETTINGS_XML_LOCATION);
      }
      else
      {
         System.setProperty(MavenContainer.ALT_USER_SETTINGS_XML_LOCATION, previousUserSettings);
      }
      if (previousLocalRepository == null)
      {
         System.clearProperty(MavenContainer.ALT_LOCAL_REPOSITORY_LOCATION);
      }
      else
      {
         System.setProperty(MavenContainer.ALT_LOCAL_REPOSITORY_LOCATION, previousUserSettings);
      }
   }

   @Before
   public void init() throws IOException
   {
      leftRepo = OperatingSystemUtils.createTempDir();
      leftRepo.deleteOnExit();
      rightRepo = OperatingSystemUtils.createTempDir();
      rightRepo.deleteOnExit();
   }

   @After
   public void teardown()
   {
      Files.delete(leftRepo, true);
      Files.delete(rightRepo, true);
   }

   @Test
   public void testAddonsSharedIfSubgraphEquivalent() throws IOException, InterruptedException, TimeoutException
   {
      Furnace furnace = FurnaceFactory.getInstance();
      AddonRepository left = furnace.addRepository(AddonRepositoryMode.MUTABLE, leftRepo);
      AddonRepository right = furnace.addRepository(AddonRepositoryMode.MUTABLE, rightRepo);

      AddonDependencyResolver resolver = new MavenAddonDependencyResolver();
      AddonManager manager = new AddonManagerImpl(furnace, resolver);

      AddonId no_dep = AddonId.from("test:no_dep", "1.0.0.Final");
      AddonId one_dep = AddonId.from("test:one_dep", "1.0.0.Final");
      AddonId one_dep_a = AddonId.from("test:one_dep_a", "1.0.0.Final");

      AddonId no_dep2 = AddonId.from("test:no_dep", "2.0.0.Final");

      Assert.assertFalse(left.isDeployed(one_dep_a));
      Assert.assertFalse(left.isDeployed(no_dep));
      Assert.assertFalse(left.isDeployed(no_dep2));
      Assert.assertFalse(left.isDeployed(one_dep));
      Assert.assertFalse(right.isDeployed(one_dep_a));
      Assert.assertFalse(right.isDeployed(no_dep));
      Assert.assertFalse(right.isDeployed(no_dep2));
      Assert.assertFalse(right.isDeployed(one_dep));

      manager.install(no_dep, left).perform();
      manager.deploy(one_dep, left).perform();

      manager.deploy(one_dep_a, right).perform();
      manager.deploy(no_dep2, right).perform();

      Assert.assertFalse(left.isDeployed(one_dep_a));
      Assert.assertFalse(left.isDeployed(no_dep2));
      Assert.assertTrue(left.isDeployed(no_dep));
      Assert.assertTrue(left.isDeployed(one_dep));
      Assert.assertFalse(right.isDeployed(one_dep));
      Assert.assertFalse(right.isDeployed(no_dep));
      Assert.assertTrue(right.isDeployed(one_dep_a));
      Assert.assertTrue(right.isDeployed(no_dep2));

      ConfigurationScanListener listener = new ConfigurationScanListener();
      ListenerRegistration<ContainerLifecycleListener> registration = furnace.addContainerLifecycleListener(listener);

      furnace.startAsync();

      while (!listener.isConfigurationScanned())
         Thread.sleep(100);

      AddonRegistry registry = furnace.getAddonRegistry();
      Addons.waitUntilStarted(registry.getAddon(one_dep_a), 10, TimeUnit.SECONDS);
      AddonRegistry leftRegistry = furnace.getAddonRegistry(left);
      AddonRegistry rightRegistry = furnace.getAddonRegistry(right);

      Addon leftNoDep = leftRegistry.getAddon(no_dep);
      Addon rightNoDep = rightRegistry.getAddon(no_dep);
      Addon rootNoDep = registry.getAddon(no_dep);
      Assert.assertTrue(leftNoDep.getStatus().isStarted());
      Assert.assertFalse(rightNoDep.getStatus().isStarted()); // not deployed to this repository
      Assert.assertFalse(rootNoDep.getStatus().isStarted()); // there is a newer version

      Addon leftNoDep2 = leftRegistry.getAddon(no_dep2);
      Addon rightNoDep2 = rightRegistry.getAddon(no_dep2);
      Addon rootNoDep2 = registry.getAddon(no_dep2);
      Assert.assertFalse(leftNoDep2.getStatus().isStarted()); // not deployed to this repository
      Assert.assertTrue(rightNoDep2.getStatus().isStarted());
      Assert.assertTrue(rootNoDep2.getStatus().isStarted());

      Addon leftOneDep = leftRegistry.getAddon(one_dep);
      Addon rightOneDep = rightRegistry.getAddon(one_dep);
      Addon rootOneDep = registry.getAddon(one_dep);
      Assert.assertTrue(leftOneDep.getStatus().isStarted());
      Assert.assertFalse(rightOneDep.getStatus().isStarted()); // not deployed to this repository
      Assert.assertTrue(rootOneDep.getStatus().isStarted());

      Addon leftOneDepA = leftRegistry.getAddon(one_dep_a);
      Addon rightOneDepA = rightRegistry.getAddon(one_dep_a);
      Addon rootOneDepA = registry.getAddon(one_dep_a);
      Assert.assertFalse(leftOneDepA.getStatus().isStarted()); // not deployed to this repository
      Assert.assertTrue(rightOneDepA.getStatus().isStarted());
      Assert.assertTrue(rootOneDepA.getStatus().isStarted());

      registration.removeListener();

      furnace.stop();
   }

   @Test
   public void testAddonsDuplicatedIfSubgraphDiffers() throws IOException, InterruptedException, TimeoutException
   {
      Furnace furnace = FurnaceFactory.getInstance();
      AddonRepository left = furnace.addRepository(AddonRepositoryMode.MUTABLE, leftRepo);
      AddonRepository right = furnace.addRepository(AddonRepositoryMode.MUTABLE, rightRepo);
      AddonDependencyResolver resolver = new MavenAddonDependencyResolver();
      AddonManager manager = new AddonManagerImpl(furnace, resolver);

      AddonId no_dep = AddonId.from("test:no_dep", "1.0.0.Final");
      AddonId one_dep = AddonId.from("test:one_dep", "1.0.0.Final");
      AddonId one_dep_a = AddonId.from("test:one_dep_a", "1.0.0.Final");
      AddonId one_dep_lib = AddonId.from("test:one_dep_lib", "1.0.0.Final");

      AddonId one_dep_2 = AddonId.from("test:one_dep", "2.0.0.Final");

      Assert.assertFalse(left.isDeployed(one_dep_lib));
      Assert.assertFalse(left.isDeployed(one_dep_a));
      Assert.assertFalse(left.isDeployed(no_dep));
      Assert.assertFalse(left.isDeployed(one_dep));
      Assert.assertFalse(left.isDeployed(one_dep_2));
      Assert.assertFalse(right.isDeployed(one_dep_lib));
      Assert.assertFalse(right.isDeployed(one_dep_a));
      Assert.assertFalse(right.isDeployed(no_dep));
      Assert.assertFalse(right.isDeployed(one_dep));
      Assert.assertFalse(right.isDeployed(one_dep_2));

      manager.install(no_dep, left).perform();
      manager.deploy(one_dep, left).perform();
      manager.deploy(one_dep_a, left).perform();
      manager.deploy(one_dep_lib, left).perform();

      manager.deploy(one_dep_2, right).perform();

      Assert.assertTrue(left.isDeployed(no_dep));
      Assert.assertTrue(left.isDeployed(one_dep));
      Assert.assertTrue(left.isDeployed(one_dep_a));
      Assert.assertTrue(left.isDeployed(one_dep_lib));
      Assert.assertFalse(left.isDeployed(one_dep_2));

      Assert.assertFalse(right.isDeployed(no_dep));
      Assert.assertFalse(right.isDeployed(one_dep));
      Assert.assertFalse(right.isDeployed(one_dep_a));
      Assert.assertFalse(right.isDeployed(one_dep_lib));
      Assert.assertTrue(right.isDeployed(one_dep_2));

      ConfigurationScanListener listener = new ConfigurationScanListener();
      ListenerRegistration<ContainerLifecycleListener> registration = furnace.addContainerLifecycleListener(listener);

      furnace.startAsync();

      while (!listener.isConfigurationScanned())
         Thread.sleep(100);

      AddonRegistry registry = furnace.getAddonRegistry();
      Addons.waitUntilStarted(registry.getAddon(one_dep_a), 10, TimeUnit.SECONDS);
      AddonRegistry leftRegistry = furnace.getAddonRegistry(left);

      Addon addon = leftRegistry.getAddon(one_dep);
      Assert.assertNotNull(addon);

      registration.removeListener();

      furnace.stop();
   }

}
