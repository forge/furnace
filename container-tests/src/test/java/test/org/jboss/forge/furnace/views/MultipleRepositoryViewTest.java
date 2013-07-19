/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package test.org.jboss.forge.furnace.views;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.forge.arquillian.ConfigurationScanListener;
import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.manager.AddonManager;
import org.jboss.forge.furnace.manager.impl.AddonManagerImpl;
import org.jboss.forge.furnace.manager.maven.addon.MavenAddonDependencyResolver;
import org.jboss.forge.furnace.manager.spi.AddonDependencyResolver;
import org.jboss.forge.furnace.repositories.AddonRepository;
import org.jboss.forge.furnace.repositories.AddonRepositoryMode;
import org.jboss.forge.furnace.se.FurnaceFactory;
import org.jboss.forge.furnace.spi.ContainerLifecycleListener;
import org.jboss.forge.furnace.spi.ListenerRegistration;
import org.jboss.forge.furnace.util.Addons;
import org.jboss.forge.furnace.util.Files;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class MultipleRepositoryViewTest
{
   File repodir1;
   File repodir2;

   @Before
   public void init() throws IOException
   {
      repodir1 = File.createTempFile("forge", "repo1");
      repodir1.deleteOnExit();
      repodir2 = File.createTempFile("forge", "repo2");
      repodir2.deleteOnExit();
   }

   @After
   public void teardown()
   {
      Files.delete(repodir1, true);
      Files.delete(repodir2, true);
   }

   @Test
   public void testAddonsSharedIfSubgraphEquivalent() throws IOException, InterruptedException, TimeoutException
   {
      Furnace furnace = FurnaceFactory.getInstance(Furnace.class.getClassLoader());
      AddonRepository left = furnace.addRepository(AddonRepositoryMode.MUTABLE, repodir1);
      AddonRepository right = furnace.addRepository(AddonRepositoryMode.MUTABLE, repodir2);

      AddonDependencyResolver resolver = new MavenAddonDependencyResolver();
      AddonManager manager = new AddonManagerImpl(furnace, resolver, false);

      AddonId facets = AddonId.from("org.jboss.forge.addon:facets", "2.0.0.Alpha5");
      AddonId convert = AddonId.from("org.jboss.forge.addon:convert", "2.0.0.Alpha5");
      AddonId resources = AddonId.from("org.jboss.forge.addon:resources", "2.0.0.Alpha5");

      AddonId facets6 = AddonId.from("org.jboss.forge.addon:facets", "2.0.0.Alpha6");

      Assert.assertFalse(left.isDeployed(resources));
      Assert.assertFalse(left.isDeployed(facets));
      Assert.assertFalse(left.isDeployed(facets6));
      Assert.assertFalse(left.isDeployed(convert));
      Assert.assertFalse(right.isDeployed(resources));
      Assert.assertFalse(right.isDeployed(facets));
      Assert.assertFalse(right.isDeployed(facets6));
      Assert.assertFalse(right.isDeployed(convert));

      manager.install(facets, left).perform();
      manager.deploy(convert, left).perform();

      manager.deploy(resources, right).perform();
      manager.deploy(facets6, right).perform();

      Assert.assertFalse(left.isDeployed(resources));
      Assert.assertFalse(right.isDeployed(convert));
      Assert.assertFalse(right.isDeployed(facets));
      Assert.assertFalse(left.isDeployed(facets6));
      Assert.assertTrue(left.isDeployed(facets));
      Assert.assertTrue(left.isDeployed(convert));
      Assert.assertTrue(right.isDeployed(resources));
      Assert.assertTrue(right.isDeployed(facets6));

      ConfigurationScanListener listener = new ConfigurationScanListener();
      ListenerRegistration<ContainerLifecycleListener> registration = furnace.addContainerLifecycleListener(listener);

      furnace.startAsync();

      while (!listener.isConfigurationScanned())
         Thread.sleep(100);

      AddonRegistry registry = furnace.getAddonRegistry();
      Addons.waitUntilStarted(registry.getAddon(resources), 10, TimeUnit.SECONDS);
      AddonRegistry leftRegistry = furnace.getAddonRegistry(left);

      Assert.assertNotNull(leftRegistry.getAddon(facets));
      Assert.assertTrue(registry.getAddon(facets).getStatus().isMissing());

      Assert.assertNotNull(registry.getAddon(facets6));
      Assert.assertTrue(leftRegistry.getAddon(facets6).getStatus().isMissing());

      registration.removeListener();

      furnace.stop();
   }

   @Test
   public void testAddonsDuplicatedIfSubgraphDiffers() throws IOException, InterruptedException, TimeoutException
   {
      Furnace furnace = FurnaceFactory.getInstance(Furnace.class.getClassLoader());
      AddonRepository left = furnace.addRepository(AddonRepositoryMode.MUTABLE, repodir1);
      AddonRepository right = furnace.addRepository(AddonRepositoryMode.MUTABLE, repodir2);
      AddonDependencyResolver resolver = new MavenAddonDependencyResolver();
      AddonManager manager = new AddonManagerImpl(furnace, resolver, false);

      AddonId facets = AddonId.from("org.jboss.forge.addon:facets", "2.0.0.Alpha5");
      AddonId convert = AddonId.from("org.jboss.forge.addon:convert", "2.0.0.Alpha5");
      AddonId resources = AddonId.from("org.jboss.forge.addon:resources", "2.0.0.Alpha5");
      AddonId dependencies = AddonId.from("org.jboss.forge.addon:dependencies", "2.0.0.Alpha5");

      AddonId convert6 = AddonId.from("org.jboss.forge.addon:convert", "2.0.0.Alpha6");

      Assert.assertFalse(left.isDeployed(dependencies));
      Assert.assertFalse(left.isDeployed(resources));
      Assert.assertFalse(left.isDeployed(facets));
      Assert.assertFalse(left.isDeployed(convert));
      Assert.assertFalse(left.isDeployed(convert6));
      Assert.assertFalse(right.isDeployed(dependencies));
      Assert.assertFalse(right.isDeployed(resources));
      Assert.assertFalse(right.isDeployed(facets));
      Assert.assertFalse(right.isDeployed(convert));
      Assert.assertFalse(right.isDeployed(convert6));

      manager.install(facets, left).perform();
      manager.deploy(convert, left).perform();
      manager.deploy(resources, left).perform();
      manager.deploy(dependencies, left).perform();

      manager.deploy(convert6, right).perform();

      Assert.assertTrue(left.isDeployed(facets));
      Assert.assertTrue(left.isDeployed(convert));
      Assert.assertTrue(left.isDeployed(resources));
      Assert.assertTrue(left.isDeployed(dependencies));
      Assert.assertFalse(left.isDeployed(convert6));

      Assert.assertFalse(right.isDeployed(facets));
      Assert.assertFalse(right.isDeployed(convert));
      Assert.assertFalse(right.isDeployed(resources));
      Assert.assertFalse(right.isDeployed(dependencies));
      Assert.assertTrue(right.isDeployed(convert6));

      ConfigurationScanListener listener = new ConfigurationScanListener();
      ListenerRegistration<ContainerLifecycleListener> registration = furnace.addContainerLifecycleListener(listener);

      furnace.startAsync();

      while (!listener.isConfigurationScanned())
         Thread.sleep(100);

      AddonRegistry registry = furnace.getAddonRegistry();
      Addons.waitUntilStarted(registry.getAddon(resources), 10, TimeUnit.SECONDS);
      AddonRegistry leftRegistry = furnace.getAddonRegistry(left);

      Addon addon = leftRegistry.getAddon(convert);
      Assert.assertNotNull(addon);

      registration.removeListener();

      furnace.stop();
   }

}
