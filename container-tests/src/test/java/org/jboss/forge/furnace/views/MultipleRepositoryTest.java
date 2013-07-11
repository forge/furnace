/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.views;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.forge.addon.convert.ConverterFactory;
import org.jboss.forge.addon.manager.AddonManager;
import org.jboss.forge.addon.manager.impl.AddonManagerImpl;
import org.jboss.forge.addon.maven.dependencies.FileResourceFactory;
import org.jboss.forge.addon.maven.dependencies.MavenContainer;
import org.jboss.forge.addon.maven.dependencies.MavenDependencyResolver;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.arquillian.ConfigurationScanListener;
import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.repositories.AddonRepository;
import org.jboss.forge.furnace.repositories.AddonRepositoryMode;
import org.jboss.forge.furnace.se.ForgeFactory;
import org.jboss.forge.furnace.services.ExportedInstance;
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
public class MultipleRepositoryTest
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
   public void testAddonsCanReferenceDependenciesInOtherRepositories() throws IOException, InterruptedException,
            TimeoutException
   {
      Furnace furnace = ForgeFactory.getInstance(Furnace.class.getClassLoader());
      AddonRepository left = furnace.addRepository(AddonRepositoryMode.MUTABLE, repodir1);
      AddonRepository right = furnace.addRepository(AddonRepositoryMode.MUTABLE, repodir2);

      MavenDependencyResolver resolver = new MavenDependencyResolver(new FileResourceFactory(),
               new MavenContainer());
      AddonManager manager = new AddonManagerImpl(furnace, resolver);

      AddonId facets = AddonId.from("org.jboss.forge.addon:facets", "2.0.0-SNAPSHOT");
      AddonId convert = AddonId.from("org.jboss.forge.addon:convert", "2.0.0-SNAPSHOT");
      AddonId resources = AddonId.from("org.jboss.forge.addon:resources", "2.0.0-SNAPSHOT");

      Assert.assertFalse(left.isDeployed(resources));
      Assert.assertFalse(left.isDeployed(facets));
      Assert.assertFalse(left.isDeployed(convert));
      Assert.assertFalse(right.isDeployed(resources));
      Assert.assertFalse(right.isDeployed(facets));
      Assert.assertFalse(right.isDeployed(convert));

      manager.install(facets, left).perform();
      manager.install(convert, left).perform();
      manager.install(resources, right).perform();

      Assert.assertFalse(left.isDeployed(resources));
      Assert.assertFalse(right.isDeployed(convert));
      Assert.assertFalse(right.isDeployed(facets));
      Assert.assertTrue(left.isDeployed(convert));
      Assert.assertTrue(left.isDeployed(convert));
      Assert.assertTrue(right.isDeployed(resources));

      ConfigurationScanListener listener = new ConfigurationScanListener();
      ListenerRegistration<ContainerLifecycleListener> registration = furnace.addContainerLifecycleListener(listener);

      furnace.startAsync();

      while (!listener.isConfigurationScanned())
         Thread.sleep(100);

      Addons.waitUntilStarted(furnace.getAddonRegistry().getAddon(resources), 10, TimeUnit.SECONDS);

      registration.removeListener();

      furnace.stop();
   }

   @Test
   public void testAddonsDontFailIfDuplicatedInOtherRepositories() throws IOException, Exception
   {
      Furnace furnace = ForgeFactory.getInstance(Furnace.class.getClassLoader());
      AddonRepository left = furnace.addRepository(AddonRepositoryMode.MUTABLE, repodir1);
      AddonRepository right = furnace.addRepository(AddonRepositoryMode.MUTABLE, repodir2);

      MavenDependencyResolver resolver = new MavenDependencyResolver(new FileResourceFactory(),
               new MavenContainer());
      AddonManager manager = new AddonManagerImpl(furnace, resolver);

      AddonId facets = AddonId.from("org.jboss.forge.addon:facets", "2.0.0-SNAPSHOT");
      AddonId convert = AddonId.from("org.jboss.forge.addon:convert", "2.0.0-SNAPSHOT");
      AddonId resources = AddonId.from("org.jboss.forge.addon:resources", "2.0.0-SNAPSHOT");

      Assert.assertFalse(left.isDeployed(resources));
      Assert.assertFalse(left.isDeployed(facets));
      Assert.assertFalse(left.isDeployed(convert));
      Assert.assertFalse(right.isDeployed(resources));
      Assert.assertFalse(right.isDeployed(facets));
      Assert.assertFalse(right.isDeployed(convert));

      manager.install(facets, left).perform();
      manager.install(convert, left).perform();
      manager.install(resources, left).perform();
      manager.install(resources, right).perform();

      Assert.assertFalse(right.isDeployed(facets));
      Assert.assertFalse(right.isDeployed(convert));
      Assert.assertTrue(left.isDeployed(resources));
      Assert.assertTrue(left.isDeployed(convert));
      Assert.assertTrue(left.isDeployed(resources));
      Assert.assertTrue(right.isDeployed(resources));

      ConfigurationScanListener listener = new ConfigurationScanListener();
      ListenerRegistration<ContainerLifecycleListener> registration = furnace.addContainerLifecycleListener(listener);

      furnace.startAsync();

      while (!listener.isConfigurationScanned())
         Thread.sleep(100);

      registration.removeListener();

      Addons.waitUntilStarted(furnace.getAddonRegistry().getAddon(resources), 10, TimeUnit.SECONDS);
      Addons.waitUntilStarted(furnace.getAddonRegistry().getAddon(facets), 10, TimeUnit.SECONDS);
      Addons.waitUntilStarted(furnace.getAddonRegistry().getAddon(convert), 10, TimeUnit.SECONDS);

      System.out.println("Getting instances.");
      ExportedInstance<ConverterFactory> instance = furnace.getAddonRegistry()
               .getExportedInstance(ConverterFactory.class);
      ConverterFactory factory = instance.get();

      factory.getConverter(File.class,
               furnace.getAddonRegistry().getAddon(resources).getClassLoader()
                        .loadClass(DirectoryResource.class.getName()));

      furnace.stop();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testCannotAddTwoRepositoriesToSameLocation() throws IOException
   {
      Furnace forge = ForgeFactory.getInstance(Furnace.class.getClassLoader());
      forge.addRepository(AddonRepositoryMode.MUTABLE, repodir1);
      forge.addRepository(AddonRepositoryMode.MUTABLE, repodir1);
   }

}
