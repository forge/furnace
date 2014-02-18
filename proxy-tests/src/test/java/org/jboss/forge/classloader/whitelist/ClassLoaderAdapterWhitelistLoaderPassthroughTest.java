/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.classloader.whitelist;

import java.util.Arrays;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.forge.arquillian.services.LocalServices;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.proxy.ClassLoaderAdapterBuilder;
import org.jboss.forge.furnace.proxy.Proxies;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class ClassLoaderAdapterWhitelistLoaderPassthroughTest
{
   @Deployment(order = 3)
   public static ForgeArchive getDeployment()
   {
      ForgeArchive archive = ShrinkWrap
               .create(ForgeArchive.class)
               .addBeansXML()
               .addClasses(MockContext.class,
                        MockContextConsumer.class,
                        MockContextPayload.class,
                        MockContextPayloadImpl.class)
               .addAsAddonDependencies(
               )
               .addAsLocalServices(ClassLoaderAdapterWhitelistLoaderPassthroughTest.class);

      return archive;
   }

   @Deployment(name = "dep1,1", testable = false, order = 2)
   public static ForgeArchive getDeploymentDep1()
   {
      ForgeArchive archive = ShrinkWrap.create(ForgeArchive.class)
               .addClasses(MockContextConsumer.class, MockContext.class)
               .addBeansXML()
               .addAsAddonDependencies(
                        AddonDependencyEntry.create("dep3")
               );

      return archive;
   }

   @Deployment(name = "dep2,1", testable = false, order = 1)
   public static ForgeArchive getDeploymentDep2()
   {
      ForgeArchive archive = ShrinkWrap.create(ForgeArchive.class)
               .addClasses(MockContextPayloadImpl.class)
               .addBeansXML()
               .addAsAddonDependencies(
                        AddonDependencyEntry.create("dep3")
               );

      return archive;
   }

   @Deployment(name = "dep3,1", testable = false, order = 0)
   public static ForgeArchive getDeploymentDep3()
   {
      ForgeArchive archive = ShrinkWrap.create(ForgeArchive.class)
               .addClasses(MockContextPayload.class)
               .addBeansXML();

      return archive;
   }

   @Test
   public void testProxyNotPropagatedIfClassLoadersBothInWhitelist() throws Exception
   {
      AddonRegistry registry = LocalServices.getFurnace(getClass().getClassLoader())
               .getAddonRegistry();
      ClassLoader thisLoader = ClassLoaderAdapterWhitelistLoaderPassthroughTest.class.getClassLoader();
      ClassLoader loader1 = registry.getAddon(AddonId.from("dep1", "1")).getClassLoader();
      ClassLoader loader2 = registry.getAddon(AddonId.from("dep2", "1")).getClassLoader();
      ClassLoader loader3 = registry.getAddon(AddonId.from("dep3", "1")).getClassLoader();

      MockContext context = new MockContext();

      Object delegate = loader1.loadClass(MockContextConsumer.class.getName()).newInstance();
      MockContextConsumer enhancedConsumer = (MockContextConsumer) ClassLoaderAdapterBuilder
               .callingLoader(thisLoader).delegateLoader(loader1).whitelist(Arrays.asList(loader1, loader2, loader3))
               .enhance(delegate);

      Object payload = loader2.loadClass(MockContextPayloadImpl.class.getName()).newInstance();
      context.getAttributes().put(MockContextPayload.class.getName(), payload);

      enhancedConsumer.processContext(context);

      Object object = context.getAttributes().get(MockContextPayload.class.getName());
      Assert.assertFalse(Proxies.isForgeProxy(object));
   }
}
