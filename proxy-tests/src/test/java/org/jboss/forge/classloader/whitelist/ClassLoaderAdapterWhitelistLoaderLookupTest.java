/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.classloader.whitelist;

import java.util.Arrays;
import java.util.HashSet;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.forge.arquillian.services.LocalServices;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.proxy.ClassLoaderAdapterBuilder;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.forge.furnace.services.Imported;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class ClassLoaderAdapterWhitelistLoaderLookupTest
{
   @Deployment(order = 3)
   public static ForgeArchive getDeployment()
   {
      ForgeArchive archive = ShrinkWrap.create(ForgeArchive.class)
               .addClasses(MockContextConsumer.class, MockContext.class)
               .addAsLocalServices(ClassLoaderAdapterWhitelistLoaderLookupTest.class);

      return archive;
   }

   @Deployment(name = "dep1,1", testable = false, order = 2)
   public static ForgeArchive getDeploymentDep1()
   {
      ForgeArchive archive = ShrinkWrap.create(ForgeArchive.class)
               .addClasses(MockContextConsumer.class, MockContext.class)
               .addAsLocalServices(MockContextConsumer.class)
               .addAsAddonDependencies(
                        AddonDependencyEntry.create("dep3")
               );

      return archive;
   }

   @Deployment(name = "dep2,1", testable = false, order = 1)
   public static ForgeArchive getDeploymentDep2()
   {
      ForgeArchive archive = ShrinkWrap.create(ForgeArchive.class).addBeansXML();

      return archive;
   }

   @Deployment(name = "dep3,1", testable = false, order = 0)
   public static ForgeArchive getDeploymentDep3()
   {
      ForgeArchive archive = ShrinkWrap.create(ForgeArchive.class)
               .addClasses(MockContextPayload.class);

      return archive;
   }

   @Test
   public void testWhitelistLookupConvertsClassTypes() throws Exception
   {
      ClassLoader thisLoader = ClassLoaderAdapterWhitelistLoaderLookupTest.class.getClassLoader();

      AddonRegistry registry = LocalServices.getFurnace(getClass().getClassLoader())
               .getAddonRegistry();

      ClassLoader loader1 = registry.getAddon(AddonId.from("dep1", "1")).getClassLoader();
      ClassLoader loader2 = registry.getAddon(AddonId.from("dep2", "1")).getClassLoader();
      ClassLoader loader3 = registry.getAddon(AddonId.from("dep3", "1")).getClassLoader();

      AddonRegistry enhancedRegistry = ClassLoaderAdapterBuilder.callingLoader(thisLoader)
               .delegateLoader(loader2)
               .whitelist(new HashSet<>(Arrays.asList(loader1, loader3))).enhance(registry);

      Assert.assertNotSame(MockContextConsumer.class, registry.getServices(MockContextConsumer.class.getName()).get()
               .getClass());

      Imported<MockContextConsumer> importedByName = enhancedRegistry.getServices(MockContextConsumer.class.getName());
      Assert.assertFalse(importedByName.isUnsatisfied());
      MockContextConsumer consumerByName = importedByName.get();
      Assert.assertSame(MockContextConsumer.class, consumerByName.getClass().getSuperclass());

      Imported<MockContextConsumer> importedByClass = enhancedRegistry.getServices(MockContextConsumer.class);
      Assert.assertFalse(importedByClass.isUnsatisfied());
      MockContextConsumer consumerByClass = importedByClass.get();
      Assert.assertNotSame(MockContextConsumer.class, consumerByClass.getClass());
   }

   @Test
   public void testWhitelistLookupConvertsClassReturnTypes() throws Exception
   {
      ClassLoader thisLoader = ClassLoaderAdapterWhitelistLoaderLookupTest.class.getClassLoader();

      AddonRegistry registry = LocalServices.getFurnace(getClass().getClassLoader())
               .getAddonRegistry();

      ClassLoader loader1 = registry.getAddon(AddonId.from("dep1", "1")).getClassLoader();
      ClassLoader loader2 = registry.getAddon(AddonId.from("dep2", "1")).getClassLoader();
      ClassLoader loader3 = registry.getAddon(AddonId.from("dep3", "1")).getClassLoader();

      AddonRegistry enhancedRegistry = ClassLoaderAdapterBuilder.callingLoader(thisLoader)
               .delegateLoader(loader2)
               .whitelist(new HashSet<>(Arrays.asList(loader1, loader3))).enhance(registry);

      Assert.assertNotSame(MockContextConsumer.class, registry.getServices(MockContextConsumer.class.getName()).get()
               .getClass());

      Assert.assertNotSame(MockContextConsumer.class, enhancedRegistry.getServices(MockContextConsumer.class)
               .get().getClass());

      Assert.assertSame(MockContextConsumer.class, enhancedRegistry.getServices(MockContextConsumer.class)
               .get().getNativeClass());
   }

}
