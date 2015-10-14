/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.manager.impl;

import static org.hamcrest.CoreMatchers.instanceOf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.TimeoutException;

import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.impl.util.Files;
import org.jboss.forge.furnace.manager.AddonManager;
import org.jboss.forge.furnace.manager.maven.MavenContainer;
import org.jboss.forge.furnace.manager.maven.addon.MavenAddonDependencyResolver;
import org.jboss.forge.furnace.manager.request.AddonActionRequest;
import org.jboss.forge.furnace.manager.request.DeployRequest;
import org.jboss.forge.furnace.manager.request.InstallRequest;
import org.jboss.forge.furnace.manager.spi.AddonDependencyResolver;
import org.jboss.forge.furnace.repositories.AddonRepositoryMode;
import org.jboss.forge.furnace.util.Addons;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class AddonManagerHotswapTest
{

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

   private Furnace furnace;
   private AddonManager addonManager;
   private AddonDependencyResolver resolver;
   private File repository;

   @Before
   public void setUp() throws IOException, InterruptedException
   {
      furnace = ServiceLoader.load(Furnace.class).iterator().next();
      resolver = new MavenAddonDependencyResolver();
      repository = File.createTempFile("furnace-repo", ".tmp");
      repository.delete();
      repository.mkdir();
      furnace.addRepository(AddonRepositoryMode.MUTABLE, repository);
      furnace.startAsync();
      while (!furnace.getStatus().isStarted())
      {
         Thread.sleep(100);
      }
      addonManager = new AddonManagerImpl(furnace, resolver);
   }

   @After
   public void tearDown()
   {
      furnace.stop();
      if (repository != null && !Files.delete(repository, true))
      {
         System.err.println("Could not delete " + repository);
      }
   }

   @Test(timeout = 5000)
   public void testFurnaceLoadsInstalledAddonFromSameInstance() throws IOException
   {
      Assert.assertEquals(1, furnace.getRepositories().size());
      Assert.assertEquals(0, furnace.getAddonRegistry().getAddons().size());
      Assert.assertEquals(0, furnace.getRepositories().get(0).listEnabled().size());
      AddonId addon = AddonId.from("test:no_dep", "3.0.0.Final");
      InstallRequest install = addonManager.install(addon);
      List<? extends AddonActionRequest> actions = install.getActions();
      Assert.assertEquals(1, actions.size());
      Assert.assertThat(actions.get(0), instanceOf(DeployRequest.class));
      install.perform();
      Assert.assertEquals(1, furnace.getRepositories().get(0).listEnabled().size());
      Assert.assertEquals(1, furnace.getAddonRegistry().getAddons().size());
   }

   @Test(timeout = 20000)
   public void testFurnaceLoadsInstalledAddonFromSeparateInstance() throws IOException, TimeoutException
   {
      Assert.assertEquals(1, furnace.getRepositories().size());
      Assert.assertEquals(0, furnace.getAddonRegistry().getAddons().size());
      Assert.assertEquals(0, furnace.getRepositories().get(0).listEnabled().size());

      Furnace furnace2 = ServiceLoader.load(Furnace.class).iterator().next();
      AddonDependencyResolver resolver = new MavenAddonDependencyResolver();
      furnace2.addRepository(AddonRepositoryMode.MUTABLE, repository);
      AddonManager addonManager = new AddonManagerImpl(furnace2, resolver);

      AddonId addon = AddonId.from("test:no_dep", "3.0.0.Final");
      InstallRequest install = addonManager.install(addon);
      List<? extends AddonActionRequest> actions = install.getActions();
      Assert.assertEquals(1, actions.size());
      Assert.assertThat(actions.get(0), instanceOf(DeployRequest.class));
      install.perform();

      Addons.waitUntilStarted(furnace.getAddonRegistry().getAddon(addon));

      Assert.assertEquals(1, furnace2.getRepositories().get(0).listEnabled().size());
      Assert.assertEquals(1, furnace.getRepositories().get(0).listEnabled().size());
      Assert.assertEquals(1, furnace.getAddonRegistry().getAddons().size());
   }

}
