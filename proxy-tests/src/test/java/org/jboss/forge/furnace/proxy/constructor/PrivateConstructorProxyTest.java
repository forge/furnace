/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.proxy.constructor;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.forge.arquillian.services.LocalServices;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.proxy.Proxies;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.forge.furnace.services.Imported;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class PrivateConstructorProxyTest
{
   @Deployment(order = 1)
   public static ForgeArchive getDeployment()
   {
      ForgeArchive archive = ShrinkWrap.create(ForgeArchive.class)
               .addAsLocalServices(PrivateConstructorProxyTest.class)
               .addAsAddonDependencies(AddonDependencyEntry.create("dep"));

      return archive;
   }

   @Deployment(name = "dep,1", testable = false, order = 0)
   public static ForgeArchive getDeploymentDep1()
   {
      ForgeArchive archive = ShrinkWrap.create(ForgeArchive.class)
               .addClasses(PrivateConstructorObject.class)
               .addAsLocalServices(PrivateConstructorObject.class)
               .addBeansXML();

      return archive;
   }

   @Test
   public void testPrivateConstructorProxyCanBeInstantiated() throws Exception
   {
      AddonRegistry registry = LocalServices.getFurnace(getClass().getClassLoader())
               .getAddonRegistry();

      Imported<PrivateConstructorObject> imported = registry.getServices(PrivateConstructorObject.class);
      PrivateConstructorObject c = imported.get();
      Assert.assertNotNull(c);
      Assert.assertEquals("value", c.lowercase("VALUE"));
      Assert.assertTrue(Proxies.isForgeProxy(c));
   }
}
