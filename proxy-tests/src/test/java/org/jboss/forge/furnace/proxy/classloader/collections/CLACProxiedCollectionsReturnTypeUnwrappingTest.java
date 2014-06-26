/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.proxy.classloader.collections;

import java.util.Arrays;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.forge.arquillian.services.LocalServices;
import org.jboss.forge.classloader.mock.collections.Profile;
import org.jboss.forge.classloader.mock.collections.ProfileFactory;
import org.jboss.forge.classloader.mock.collections.ProfileManager;
import org.jboss.forge.classloader.mock.collections.ProfileManagerImpl;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.proxy.ClassLoaderAdapterBuilder;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.forge.furnace.util.Sets;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class CLACProxiedCollectionsReturnTypeUnwrappingTest
{
   @Deployment(order = 3)
   public static ForgeArchive getDeployment()
   {
      ForgeArchive archive = ShrinkWrap.create(ForgeArchive.class)
               .addBeansXML()
               .addClasses(Profile.class, ProfileManager.class, ProfileFactory.class)
               .addAsLocalServices(CLACProxiedCollectionsReturnTypeUnwrappingTest.class);

      return archive;
   }

   @Deployment(name = "dep1,1", testable = false, order = 2)
   public static ForgeArchive getDeploymentDep1()
   {
      ForgeArchive archive = ShrinkWrap.create(ForgeArchive.class)
               .addClasses()
               .addAsAddonDependencies(AddonDependencyEntry.create("dep2", "2"))
               .addBeansXML();

      return archive;
   }

   @Deployment(name = "dep2,2", testable = false, order = 2)
   public static ForgeArchive getDeploymentDep2()
   {
      ForgeArchive archive = ShrinkWrap.create(ForgeArchive.class)
               .addClasses(Profile.class, ProfileManager.class, ProfileManagerImpl.class, ProfileFactory.class)
               .addBeansXML();

      return archive;
   }

   @Test
   public void testCollectionsReturnUnwrappedResultsIfUnwrappedTypeIsCompatible() throws Exception
   {
      AddonRegistry registry = LocalServices.getFurnace(getClass().getClassLoader())
               .getAddonRegistry();
      ClassLoader thisLoader = CLACProxiedCollectionsReturnTypeUnwrappingTest.class.getClassLoader();
      ClassLoader dep1Loader = registry.getAddon(AddonId.from("dep1", "1")).getClassLoader();
      ClassLoader dep2Loader = registry.getAddon(AddonId.from("dep2", "2")).getClassLoader();

      Class<?> foreignProfileType = dep1Loader.loadClass(ProfileFactory.class.getName());
      Object delegate = foreignProfileType.newInstance();
      ProfileFactory factory = (ProfileFactory) ClassLoaderAdapterBuilder.callingLoader(thisLoader)
               .delegateLoader(dep1Loader).whitelist(Sets.toSet(Arrays.asList(dep1Loader, dep2Loader)))
               .enhance(delegate);

      Profile profile = factory.createProfile();

      ProfileManager manager = factory.createProfileManager();

      manager.setProfileListCallGet(Arrays.asList(profile));
   }

}
