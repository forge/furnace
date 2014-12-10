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
import org.jboss.forge.furnace.addons.AddonId;
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
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class MultipleRepositoryTest
{
   File repodir1;
   File repodir2;

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
      Furnace furnace = FurnaceFactory.getInstance();
      AddonRepository left = furnace.addRepository(AddonRepositoryMode.MUTABLE, repodir1);
      AddonRepository right = furnace.addRepository(AddonRepositoryMode.MUTABLE, repodir2);

      AddonDependencyResolver resolver = new MavenAddonDependencyResolver();
      AddonManager manager = new AddonManagerImpl(furnace, resolver);

      AddonId no_dep = AddonId.from("test:no_dep", "1.0.0.Final");
      AddonId one_dep = AddonId.from("test:one_dep", "1.0.0.Final");
      AddonId one_dep_a = AddonId.from("test:one_dep_a", "1.0.0.Final");

      Assert.assertFalse(left.isDeployed(one_dep_a));
      Assert.assertFalse(left.isDeployed(no_dep));
      Assert.assertFalse(left.isDeployed(one_dep));
      Assert.assertFalse(right.isDeployed(one_dep_a));
      Assert.assertFalse(right.isDeployed(no_dep));
      Assert.assertFalse(right.isDeployed(one_dep));

      manager.install(no_dep, left).perform();
      manager.deploy(one_dep, left).perform();
      manager.deploy(one_dep_a, right).perform();

      Assert.assertFalse(left.isDeployed(one_dep_a));
      Assert.assertFalse(right.isDeployed(one_dep));
      Assert.assertFalse(right.isDeployed(no_dep));
      Assert.assertTrue(left.isDeployed(one_dep));
      Assert.assertTrue(left.isDeployed(one_dep));
      Assert.assertTrue(right.isDeployed(one_dep_a));

      ConfigurationScanListener listener = new ConfigurationScanListener();
      ListenerRegistration<ContainerLifecycleListener> registration = furnace.addContainerLifecycleListener(listener);

      furnace.startAsync();

      while (!listener.isConfigurationScanned())
         Thread.sleep(100);

      Addons.waitUntilStarted(furnace.getAddonRegistry().getAddon(one_dep_a), 10, TimeUnit.SECONDS);

      registration.removeListener();

      furnace.stop();
   }

   @Test
   public void testAddonsDontFailIfDuplicatedInOtherRepositories() throws IOException, Exception
   {
      Furnace furnace = FurnaceFactory.getInstance();
      AddonRepository left = furnace.addRepository(AddonRepositoryMode.MUTABLE, repodir1);
      AddonRepository right = furnace.addRepository(AddonRepositoryMode.MUTABLE, repodir2);

      AddonDependencyResolver resolver = new MavenAddonDependencyResolver();
      AddonManager manager = new AddonManagerImpl(furnace, resolver);

      AddonId no_dep = AddonId.from("test:no_dep", "1.0.0.Final");
      AddonId one_dep = AddonId.from("test:one_dep", "1.0.0.Final");
      AddonId one_dep_a = AddonId.from("test:one_dep_a", "1.0.0.Final");

      Assert.assertFalse(left.isDeployed(one_dep_a));
      Assert.assertFalse(left.isDeployed(no_dep));
      Assert.assertFalse(left.isDeployed(one_dep));
      Assert.assertFalse(right.isDeployed(one_dep_a));
      Assert.assertFalse(right.isDeployed(no_dep));
      Assert.assertFalse(right.isDeployed(one_dep));

      manager.install(no_dep, left).perform();
      manager.deploy(one_dep, left).perform();
      manager.deploy(one_dep_a, left).perform();
      manager.deploy(one_dep_a, right).perform();

      Assert.assertFalse(right.isDeployed(no_dep));
      Assert.assertFalse(right.isDeployed(one_dep));
      Assert.assertTrue(left.isDeployed(one_dep_a));
      Assert.assertTrue(left.isDeployed(one_dep));
      Assert.assertTrue(left.isDeployed(one_dep_a));
      Assert.assertTrue(right.isDeployed(one_dep_a));

      ConfigurationScanListener listener = new ConfigurationScanListener();
      ListenerRegistration<ContainerLifecycleListener> registration = furnace.addContainerLifecycleListener(listener);

      furnace.startAsync();

      while (!listener.isConfigurationScanned())
         Thread.sleep(100);

      registration.removeListener();

      Addons.waitUntilStarted(furnace.getAddonRegistry().getAddon(one_dep_a), 10, TimeUnit.SECONDS);
      Addons.waitUntilStarted(furnace.getAddonRegistry().getAddon(no_dep), 10, TimeUnit.SECONDS);
      Addons.waitUntilStarted(furnace.getAddonRegistry().getAddon(one_dep), 10, TimeUnit.SECONDS);

      System.out.println("Getting instances.");
      //FIXME Mocked addons should contain these classes. Use reflection to avoid compile-time dependency ?
//      ExportedInstance<ConverterFactory> instance = furnace.getAddonRegistry()
//               .getExportedInstance(ConverterFactory.class);
//      ConverterFactory factory = instance.get();
//
//      factory.getConverter(File.class,
//               furnace.getAddonRegistry().getAddon(one_dep_a).getClassLoader()
//                        .loadClass("org.jboss.forge.addon.resource.DirectoryResource"));

      furnace.stop();
   }

   @Test
   public void testAddTwoRepositoriesToSameLocationIsIdempotent() throws IOException
   {
      Furnace forge = FurnaceFactory.getInstance();
      AddonRepository repo1 = forge.addRepository(AddonRepositoryMode.MUTABLE, repodir1);
      AddonRepository repo2 = forge.addRepository(AddonRepositoryMode.MUTABLE, repodir1);
      Assert.assertEquals(repo1, repo2);
   }

}
