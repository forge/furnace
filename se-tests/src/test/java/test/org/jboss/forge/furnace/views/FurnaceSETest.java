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

import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.manager.AddonManager;
import org.jboss.forge.furnace.manager.impl.AddonManagerImpl;
import org.jboss.forge.furnace.manager.impl.request.ConfigurationScanListener;
import org.jboss.forge.furnace.manager.maven.MavenContainer;
import org.jboss.forge.furnace.manager.maven.addon.MavenAddonDependencyResolver;
import org.jboss.forge.furnace.manager.spi.AddonDependencyResolver;
import org.jboss.forge.furnace.repositories.AddonRepositoryMode;
import org.jboss.forge.furnace.se.FurnaceFactory;
import org.jboss.forge.furnace.spi.ContainerLifecycleListener;
import org.jboss.forge.furnace.spi.ListenerRegistration;
import org.jboss.forge.furnace.util.Addons;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class FurnaceSETest
{
   File repodir1;

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
      repodir1 = File.createTempFile("forge", "repo");
      repodir1.deleteOnExit();
   }

   @Test
   public void testAddonsLoadAPIClassesFromBootpathJAR() throws IOException, Exception
   {
      Furnace furnace = FurnaceFactory.getInstance();

      furnace.addRepository(AddonRepositoryMode.MUTABLE, repodir1);

      AddonDependencyResolver resolver = new MavenAddonDependencyResolver();
      AddonManager manager = new AddonManagerImpl(furnace, resolver);

      AddonId no_dep = AddonId.from("test:no_dep", "1.0.0.Final");
      AddonId one_dep = AddonId.from("test:one_dep", "1.0.0.Final");

      manager.install(no_dep).perform();
      manager.install(one_dep).perform();

      ConfigurationScanListener listener = new ConfigurationScanListener();
      ListenerRegistration<ContainerLifecycleListener> registration = furnace.addContainerLifecycleListener(listener);

      furnace.startAsync();

      while (!listener.isConfigurationScanned())
         Thread.sleep(100);

      registration.removeListener();

      Addon projectsAddon = furnace.getAddonRegistry().getAddon(no_dep);
      Addons.waitUntilStarted(projectsAddon, 10, TimeUnit.SECONDS);

      ClassLoader addonClassLoader = projectsAddon.getClassLoader().loadClass(Addon.class.getName()).getClassLoader();
      ClassLoader appClassLoader = Addon.class.getClassLoader();
      Assert.assertNotEquals(appClassLoader, addonClassLoader);

      Assert.assertTrue(projectsAddon.getStatus().isStarted());
      furnace.stop();
   }

}
