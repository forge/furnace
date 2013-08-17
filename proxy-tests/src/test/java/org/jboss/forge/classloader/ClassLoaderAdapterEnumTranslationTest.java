/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.classloader;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.forge.arquillian.services.LocalServices;
import org.jboss.forge.classloader.mock.SimpleEnum;
import org.jboss.forge.classloader.mock.SimpleEnumFactory;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.proxy.ClassLoaderAdapterBuilder;
import org.jboss.forge.furnace.proxy.Proxies;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class ClassLoaderAdapterEnumTranslationTest
{
   @Deployment(order = 3)
   public static ForgeArchive getDeployment()
   {
      ForgeArchive archive = ShrinkWrap
               .create(ForgeArchive.class)
               .addBeansXML()
               .addClasses(SimpleEnum.class, SimpleEnumFactory.class)
               .addAsLocalServices(ClassLoaderAdapterEnumTranslationTest.class);

      return archive;
   }

   @Deployment(name = "dep,1", testable = false, order = 2)
   public static ForgeArchive getDeploymentDep1()
   {
      ForgeArchive archive = ShrinkWrap.create(ForgeArchive.class)
               .addClasses(SimpleEnum.class, SimpleEnumFactory.class)
               .addBeansXML();

      return archive;
   }

   @Test
   public void testSimpleEnumCollision() throws Exception
   {
      AddonRegistry registry = LocalServices.getFurnace(getClass().getClassLoader())
               .getAddonRegistry();
      ClassLoader thisLoader = ClassLoaderAdapterEnumTranslationTest.class.getClassLoader();
      ClassLoader dep1Loader = registry.getAddon(AddonId.from("dep", "1")).getClassLoader();

      Class<?> foreignType = dep1Loader.loadClass(SimpleEnumFactory.class.getName());
      try
      {
         SimpleEnum local = (SimpleEnum) foreignType.getMethod("getEnum")
                  .invoke(foreignType.newInstance());

         Assert.fail("Should have received a " + ClassCastException.class.getName() + " but got a real object ["
                  + local + "]");
      }
      catch (ClassCastException e)
      {
      }
      catch (Exception e)
      {
         Assert.fail("Should have received a " + ClassCastException.class.getName() + " but was: " + e);
      }

      Object delegate = foreignType.newInstance();
      SimpleEnumFactory enhancedFactory = (SimpleEnumFactory) ClassLoaderAdapterBuilder.callingLoader(thisLoader)
               .delegateLoader(dep1Loader).enhance(delegate);

      Assert.assertTrue(Proxies.isForgeProxy(enhancedFactory));
      SimpleEnum enhancedInstance = enhancedFactory.getEnum();
      Assert.assertFalse(Proxies.isForgeProxy(enhancedInstance));

      enhancedFactory.useEnum(SimpleEnum.STOPPED);
   }
}
