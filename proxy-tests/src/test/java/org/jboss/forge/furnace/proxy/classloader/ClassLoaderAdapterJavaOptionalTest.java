/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.proxy.classloader;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;

import java.util.Optional;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.forge.arquillian.services.LocalServices;
import org.jboss.forge.classloader.mock.MockOptionalService;
import org.jboss.forge.classloader.mock.MockParentInterface1;
import org.jboss.forge.classloader.mock.MockParentInterface2;
import org.jboss.forge.classloader.mock.MockService2;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.proxy.ClassLoaderAdapterBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class ClassLoaderAdapterJavaOptionalTest
{
   @Deployment(order = 3)
   public static AddonArchive getDeployment()
   {
      return ShrinkWrap.create(AddonArchive.class)
               .addClasses(MockOptionalService.class, MockParentInterface1.class,
                        MockParentInterface2.class)
               .addAsLocalServices(ClassLoaderAdapterJavaOptionalTest.class);
   }

   @Deployment(name = "dep,1", testable = false, order = 2)
   public static AddonArchive getDeploymentDep1()
   {
      return ShrinkWrap.create(AddonArchive.class)
               .addClasses(MockOptionalService.class, MockParentInterface1.class, MockParentInterface2.class,
                        MockService2.class);
   }

   @Test
   public void testOptionalProxyAsResult() throws Exception
   {
      AddonRegistry registry = LocalServices.getFurnace(getClass().getClassLoader())
               .getAddonRegistry();
      ClassLoader thisLoader = ClassLoaderAdapterJavaOptionalTest.class.getClassLoader();
      ClassLoader dep1Loader = registry.getAddon(AddonId.from("dep", "1")).getClassLoader();
      Class<?> loadedType = dep1Loader.loadClass(MockOptionalService.class.getName());
      Object delegate = loadedType.newInstance();
      MockOptionalService enhanced = (MockOptionalService) ClassLoaderAdapterBuilder.callingLoader(thisLoader)
               .delegateLoader(dep1Loader).enhance(delegate);
      Assert.assertThat(enhanced.getOptional(), notNullValue());
      MockParentInterface2 mpi = enhanced.getOptional().get();
      Assert.assertEquals("Lincoln", mpi.getResult());
   }

   @Test
   public void testOptionalProxyAsParameter() throws Exception
   {
      AddonRegistry registry = LocalServices.getFurnace(getClass().getClassLoader())
               .getAddonRegistry();
      ClassLoader thisLoader = ClassLoaderAdapterJavaOptionalTest.class.getClassLoader();
      ClassLoader dep1Loader = registry.getAddon(AddonId.from("dep", "1")).getClassLoader();
      Class<?> loadedType = dep1Loader.loadClass(MockOptionalService.class.getName());
      Object delegate = loadedType.newInstance();
      MockOptionalService enhanced = (MockOptionalService) ClassLoaderAdapterBuilder.callingLoader(thisLoader)
               .delegateLoader(dep1Loader).enhance(delegate);
      MockParentInterface1 mpi = new MockParentInterface1()
      {
         @Override
         public Object getResult()
         {
            return "My Result";
         }
      };
      Assert.assertThat(enhanced.getResult(Optional.empty()), nullValue());
      Assert.assertThat(enhanced.getResult(Optional.of(mpi)), equalTo("My Result"));
   }

   @Test
   public void testOptionalStringProxyAsResult() throws Exception
   {
      AddonRegistry registry = LocalServices.getFurnace(getClass().getClassLoader())
               .getAddonRegistry();
      ClassLoader thisLoader = ClassLoaderAdapterJavaOptionalTest.class.getClassLoader();
      ClassLoader dep1Loader = registry.getAddon(AddonId.from("dep", "1")).getClassLoader();
      Class<?> loadedType = dep1Loader.loadClass(MockOptionalService.class.getName());
      Object delegate = loadedType.newInstance();
      MockOptionalService enhanced = (MockOptionalService) ClassLoaderAdapterBuilder.callingLoader(thisLoader)
               .delegateLoader(dep1Loader).enhance(delegate);
      Assert.assertThat(enhanced.getStringOptional(), notNullValue());
      Assert.assertEquals("Lincoln", enhanced.getStringOptional().get());
   }

   @Test
   public void testOptionalStringProxyAsParameter() throws Exception
   {
      AddonRegistry registry = LocalServices.getFurnace(getClass().getClassLoader())
               .getAddonRegistry();
      ClassLoader thisLoader = ClassLoaderAdapterJavaOptionalTest.class.getClassLoader();
      ClassLoader dep1Loader = registry.getAddon(AddonId.from("dep", "1")).getClassLoader();
      Class<?> loadedType = dep1Loader.loadClass(MockOptionalService.class.getName());
      Object delegate = loadedType.newInstance();
      MockOptionalService enhanced = (MockOptionalService) ClassLoaderAdapterBuilder.callingLoader(thisLoader)
               .delegateLoader(dep1Loader).enhance(delegate);
      Assert.assertEquals("My Test", enhanced.getStringOptional(Optional.of("My Test")));
   }
}
