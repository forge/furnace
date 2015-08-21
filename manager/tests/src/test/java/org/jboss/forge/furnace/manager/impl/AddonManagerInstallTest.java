/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.manager.impl;

import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.isA;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.impl.util.Files;
import org.jboss.forge.furnace.manager.AddonManager;
import org.jboss.forge.furnace.manager.maven.MavenContainer;
import org.jboss.forge.furnace.manager.maven.addon.MavenAddonDependencyResolver;
import org.jboss.forge.furnace.manager.request.AddonActionRequest;
import org.jboss.forge.furnace.manager.request.DeployRequest;
import org.jboss.forge.furnace.manager.request.InstallRequest;
import org.jboss.forge.furnace.manager.request.UpdateRequest;
import org.jboss.forge.furnace.manager.spi.AddonDependencyResolver;
import org.jboss.forge.furnace.manager.spi.AddonInfo;
import org.jboss.forge.furnace.repositories.AddonRepositoryMode;
import org.jboss.forge.furnace.util.OperatingSystemUtils;
import org.jboss.forge.furnace.versions.SingleVersion;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class AddonManagerInstallTest
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
   public void setUp() throws IOException
   {
      furnace = ServiceLoader.load(Furnace.class).iterator().next();
      resolver = new MavenAddonDependencyResolver();
      repository = OperatingSystemUtils.createTempDir();
      furnace.addRepository(AddonRepositoryMode.MUTABLE, repository);
      addonManager = new AddonManagerImpl(furnace, resolver);
   }

   @After
   public void tearDown()
   {
      if (repository != null && !Files.delete(repository, true))
      {
         System.err.println("Could not delete " + repository);
      }
   }

   @Test
   public void testAddonInstallNoDependencyWithEmptyRepository() throws IOException
   {
      AddonId addon = AddonId.from("test:no_dep", "1.0.0.Final");
      InstallRequest install = addonManager.install(addon);
      List<? extends AddonActionRequest> actions = install.getActions();
      Assert.assertEquals(1, actions.size());
      Assert.assertThat(actions.get(0), instanceOf(DeployRequest.class));
   }

   @Test
   public void testAddonInstallNoDependencyWithAddonAlreadyInstalled() throws IOException
   {
      AddonId addon = AddonId.from("test:no_dep", "1.0.0.Final");
      InstallRequest install = addonManager.install(addon);
      install.perform();
      install = addonManager.install(addon);
      Assert.assertTrue(install.getActions().isEmpty());
   }

   @Test
   public void testAddonInstallSnapshot() throws IOException
   {
      AddonId addon = AddonId.from("test:no_dep", "1.1.2-SNAPSHOT");
      InstallRequest install = addonManager.install(addon);
      Assert.assertEquals(1, install.getActions().size());
      install.perform();
      install = addonManager.install(addon);
      List<? extends AddonActionRequest> actions = install.getActions();
      Assert.assertEquals(1, actions.size());
      Assert.assertThat(actions.get(0), instanceOf(UpdateRequest.class));
   }

   @Test
   public void testAddonUpdate() throws IOException
   {
      AddonId addon = AddonId.from("test:one_dep", "1.0.0.Final");
      InstallRequest install = addonManager.install(addon);
      Assert.assertEquals(2, install.getActions().size());
   }

   @SuppressWarnings("unchecked")
   @Test
   public void testInstallTwoDeps() throws IOException
   {
      AddonId addon = AddonId.from("test:one_dep", "1.0.0.Final");
      InstallRequest install = addonManager.install(addon);
      List<?> actions = install.getActions();
      Assert.assertEquals(2, actions.size());
      Assert.assertThat((List<DeployRequest>) actions, everyItem(isA(DeployRequest.class)));
   }

   @SuppressWarnings("unchecked")
   @Test
   public void testParentExclusion() throws IOException
   {
      AddonId addon = AddonId.from("test:no_dep_one_lib_excluding_indirect_lib", "1.0.0.Final");
      InstallRequest install = addonManager.install(addon);
      List<? extends AddonActionRequest> actions = install.getActions();
      Assert.assertEquals(1, actions.size());
      Assert.assertThat((List<DeployRequest>) actions, everyItem(isA(DeployRequest.class)));
      DeployRequest deployRequest = (DeployRequest) actions.get(0);
      AddonInfo addonInfo = deployRequest.getRequestedAddonInfo();
      Set<File> resources = addonInfo.getResources();
      Assert.assertEquals("It should have three resources", 3, resources.size());
   }

   @Test
   public void testAddonInstallAPIVersionNoDependencyWithEmptyRepository() throws IOException
   {
      AddonId addon = AddonId.from("test:no_dep", "1.0.0.Final");
      InstallRequest install = addonManager.install(addon);
      List<? extends AddonActionRequest> actions = install.getActions();
      Assert.assertEquals(1, actions.size());
      Assert.assertThat(actions.get(0), instanceOf(DeployRequest.class));
      Assert.assertEquals(SingleVersion.valueOf("2.4.1.Final"), actions.get(0).getRequestedAddonInfo().getAddon()
               .getApiVersion());
   }

}
