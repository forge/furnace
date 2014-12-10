/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.manager;

import static org.hamcrest.CoreMatchers.hasItems;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.manager.maven.MavenContainer;
import org.jboss.forge.furnace.manager.maven.addon.MavenAddonDependencyResolver;
import org.jboss.forge.furnace.manager.spi.AddonDependencyResolver;
import org.jboss.forge.furnace.manager.spi.AddonInfo;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class AddonDependencyResolverTest
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

   private AddonDependencyResolver resolver;

   @Before
   public void setUp() throws IOException
   {
      resolver = new MavenAddonDependencyResolver();
   }

   @Test
   public void testResolutionInfo() throws Exception
   {
      AddonId addon = AddonId.from("test:one_dep", "1.0.0.Final");
      AddonId addonDep = AddonId.from("test:no_dep", "1.0.0.Final");
      AddonInfo info = resolver.resolveAddonDependencyHierarchy(addon);
      Assert.assertNotNull(info);
      Assert.assertEquals(1, info.getRequiredAddons().size());
      Assert.assertEquals(addonDep, info.getRequiredAddons().iterator().next().getAddon());
      Assert.assertEquals(1, info.getResources().size());
   }

   @Test
   public void testIndirectResolutionInfo() throws Exception
   {
      AddonId addon = AddonId.from("test:indirect_dep", "1.0.0.Final");
      AddonInfo info = resolver.resolveAddonDependencyHierarchy(addon);
      Assert.assertNotNull(info);
      Set<AddonId> requiredAddons = new HashSet<AddonId>();
      for (AddonInfo ai : info.getRequiredAddons())
      {
         requiredAddons.add(ai.getAddon());
      }

      AddonId[] expecteds = new AddonId[] {
               AddonId.from("test:one_dep", "1.0.0.Final")
      };
      Assert.assertEquals(expecteds.length, requiredAddons.size());
      Assert.assertThat(requiredAddons, hasItems(expecteds));
   }

   /**
    * A->B->C <br/>
    * A->D->C
    * 
    * @throws Exception
    */
   @Test
   public void testResolutionTwoDependencies() throws Exception
   {
      AddonId addon = AddonId.from("test:two_deps", "1.0.0.Final");
      AddonInfo info = resolver.resolveAddonDependencyHierarchy(addon);
      Assert.assertNotNull(info);
      Set<AddonId> requiredAddons = new HashSet<AddonId>();
      for (AddonInfo ai : info.getRequiredAddons())
      {
         requiredAddons.add(ai.getAddon());
      }
      AddonId[] expecteds = new AddonId[] {
               AddonId.from("test:one_dep_a", "1.0.0.Final"),
               AddonId.from("test:one_dep", "1.0.0.Final")
      };
      Assert.assertEquals(expecteds.length, requiredAddons.size());
      Assert.assertThat(requiredAddons, hasItems(expecteds));
   }

   @Test
   public void testResolutionInfoLib() throws Exception
   {
      AddonId addon = AddonId.from("test:one_dep_lib", "1.0.0.Final");
      AddonInfo info = resolver.resolveAddonDependencyHierarchy(addon);
      Assert.assertNotNull(info);
      Assert.assertTrue(info.getRequiredAddons().isEmpty());
      Assert.assertEquals(2, info.getResources().size());
   }

   @Test
   public void testResolutionInfoOptionalLib() throws Exception
   {
      AddonId addon = AddonId.from("test:one_optional_dep", "1.0.0.Final");
      AddonInfo info = resolver.resolveAddonDependencyHierarchy(addon);
      Assert.assertNotNull(info);
      Assert.assertTrue(info.getRequiredAddons().isEmpty());
      Assert.assertEquals(2, info.getResources().size());
   }

   @Test
   public void testResolveVersions() throws Exception
   {
      AddonId[] versions = resolver.resolveVersions("test:furnace_api_dep").get();
      Assert.assertNotNull(versions);
      Assert.assertEquals(1, versions.length);
      AddonId sut = versions[0];
      Assert.assertEquals("1.0.0.Final", sut.getVersion().toString());
   }

   @Test
   public void testResolveVersionsSnapshot() throws Exception
   {
      AddonId[] versions = resolver.resolveVersions("test:furnace_api_snapshot").get();
      Assert.assertNotNull(versions);
      Assert.assertEquals(1, versions.length);
      AddonId sut = versions[0];
      Assert.assertEquals("1.0.0-SNAPSHOT", sut.getVersion().toString());
   }

   @Test
   public void testResolveAPIVersion() throws Exception
   {
      AddonId sut = AddonId.from("test:furnace_api_dep", "1.0.0.Final");
      String apiVersion = resolver.resolveAPIVersion(sut).get();
      Assert.assertEquals("2.0.0.Beta3", apiVersion);
   }

   @Test
   public void testResolveIndirectAPIVersion() throws Exception
   {
      AddonId sut = AddonId.from("test:furnace_api_indirect_dep", "1.0.0.Final");
      String apiVersion = resolver.resolveAPIVersion(sut).get();
      Assert.assertEquals("2.0.0.Beta3", apiVersion);
   }

}
