/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.manager.impl;

import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.isA;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.impl.FurnaceImpl;
import org.jboss.forge.furnace.impl.util.Files;
import org.jboss.forge.furnace.manager.maven.MavenContainer;
import org.jboss.forge.furnace.manager.maven.addon.MavenAddonDependencyResolver;
import org.jboss.forge.furnace.manager.request.AddonActionRequest;
import org.jboss.forge.furnace.manager.request.DeployRequest;
import org.jboss.forge.furnace.manager.request.InstallRequest;
import org.jboss.forge.furnace.repositories.AddonRepository;
import org.jboss.forge.furnace.repositories.AddonRepositoryMode;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests that depend on real addons
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 * 
 */
public class AddonManagerRepositoryTest
{

   private Furnace furnace;
   private AddonManagerImpl addonManager;
   private AddonRepository mutable;
   private AddonRepository immutable;

   @BeforeClass
   public static void setRemoteRepository() throws IOException
   {
      System.setProperty(MavenContainer.ALT_USER_SETTINGS_XML_LOCATION, getAbsolutePath("profiles/settings.xml"));
      System.setProperty(MavenContainer.ALT_LOCAL_REPOSITORY_LOCATION, "target/the-other-repository");
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
      System.clearProperty(MavenContainer.ALT_USER_SETTINGS_XML_LOCATION);
      System.clearProperty(MavenContainer.ALT_LOCAL_REPOSITORY_LOCATION);
   }

   @Before
   public void setUp() throws IOException
   {
      furnace = new FurnaceImpl();
      mutable = registerTempRepository(furnace, AddonRepositoryMode.MUTABLE);
      immutable = registerTempRepository(furnace, AddonRepositoryMode.IMMUTABLE);
      addonManager = new AddonManagerImpl(furnace, new MavenAddonDependencyResolver());
   }

   private static AddonRepository registerTempRepository(Furnace furnace, AddonRepositoryMode mode) throws IOException
   {
      File repository = File.createTempFile("furnace-repo", ".tmp");
      repository.delete();
      repository.mkdir();
      return furnace.addRepository(mode, repository);
   }

   /**
    * Hack to deploy addon in an immutable repository
    */
   private static void deployAddonInImmutableRepository(AddonId addonId, AddonRepository repository)
   {
      Furnace furnace = new FurnaceImpl();
      furnace.addRepository(AddonRepositoryMode.MUTABLE, repository.getRootDirectory());
      AddonManagerImpl addonManager = new AddonManagerImpl(furnace, new MavenAddonDependencyResolver());
      addonManager.deploy(addonId).perform();
   }

   @After
   public void tearDown()
   {
      if (immutable != null)
      {
         File rootDirectory = immutable.getRootDirectory();
         if (!Files.delete(rootDirectory, true))
         {
            System.err.println("Could not delete " + rootDirectory);
            rootDirectory.deleteOnExit();
         }
      }
      if (mutable != null)
      {
         File rootDirectory = mutable.getRootDirectory();
         if (!Files.delete(rootDirectory, true))
         {
            System.err.println("Could not delete " + rootDirectory);
            rootDirectory.deleteOnExit();
         }
      }
   }

   @SuppressWarnings("unchecked")
   @Test
   public void testUpdateOnImmutableRepository() throws Exception
   {
      AddonId no_dep = AddonId.from("test:no_dep", "1.0.0.Final");
      AddonId no_dep_newer = AddonId.from("test:no_dep", "1.0.1.Final");

      // Adding an addon in a immutable repo
      deployAddonInImmutableRepository(no_dep, immutable);
      InstallRequest newer = addonManager.install(no_dep_newer);
      List<? extends AddonActionRequest> actions = newer.getActions();
      Assert.assertEquals(1, actions.size());
      Assert.assertThat((List<DeployRequest>) actions, everyItem(isA(DeployRequest.class)));
   }
}
