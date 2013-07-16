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

import org.jboss.forge.addon.manager.AddonManager;
import org.jboss.forge.addon.manager.impl.AddonManagerImpl;
import org.jboss.forge.addon.manager.impl.request.ConfigurationScanListener;
import org.jboss.forge.addon.manager.spi.AddonDependencyResolver;
import org.jboss.forge.addon.maven.addon.MavenAddonDependencyResolver;
import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.repositories.AddonRepositoryMode;
import org.jboss.forge.furnace.se.FurnaceFactory;
import org.jboss.forge.furnace.spi.ContainerLifecycleListener;
import org.jboss.forge.furnace.spi.ListenerRegistration;
import org.jboss.forge.furnace.util.Addons;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class FurnaceSETest
{
   File repodir1;

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
      AddonManager manager = new AddonManagerImpl(furnace, resolver, false);

      AddonId projects = AddonId.from("org.jboss.forge.addon:projects", "2.0.0-SNAPSHOT");
      AddonId maven = AddonId.from("org.jboss.forge.addon:maven", "2.0.0-SNAPSHOT");

      manager.install(projects).perform();
      manager.install(maven).perform();

      ConfigurationScanListener listener = new ConfigurationScanListener();
      ListenerRegistration<ContainerLifecycleListener> registration = furnace.addContainerLifecycleListener(listener);

      furnace.startAsync();

      while (!listener.isConfigurationScanned())
         Thread.sleep(100);

      registration.removeListener();

      Addon projectsAddon = furnace.getAddonRegistry().getAddon(projects);
      Addons.waitUntilStarted(projectsAddon, 10, TimeUnit.SECONDS);

      ClassLoader addonClassLoader = projectsAddon.getClassLoader().loadClass(Addon.class.getName()).getClassLoader();
      ClassLoader appClassLoader = Addon.class.getClassLoader();
      Assert.assertNotEquals(appClassLoader, addonClassLoader);

      Assert.assertTrue(projectsAddon.getStatus().isStarted());
      furnace.stop();
   }

}
