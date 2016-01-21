/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.proxy.classloader;

import static org.hamcrest.CoreMatchers.instanceOf;

import java.lang.reflect.Method;
import java.util.Optional;

import org.jboss.forge.classloader.mock.MockOptionalService;
import org.jboss.forge.classloader.mock.MockResult;
import org.jboss.forge.classloader.mock.MockService;
import org.jboss.forge.furnace.proxy.ClassLoaderAdapterBuilder;
import org.jboss.forge.furnace.proxy.ForgeProxy;
import org.jboss.forge.furnace.proxy.Proxies;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class ClassLoaderAdapterCallbackTest
{
   ForgeProxy handler = new ForgeProxy()
   {
      @Override
      public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable
      {
         return null;
      }

      @Override
      public Object getDelegate()
      {
         return new MockService();
      }

      @Override
      public Object getHandler() throws Exception
      {
         return this;
      }
   };

   @Test
   public void testNestedDupicateProxyAdapterCallback() throws Exception
   {
      ClassLoader loader = ClassLoaderAdapterCallbackTest.class.getClassLoader();
      MockService original = new MockService();
      MockService object = ClassLoaderAdapterBuilder.callingLoader(loader).delegateLoader(loader)
               .enhance(original, MockService.class);
      MockService object2 = ClassLoaderAdapterBuilder.callingLoader(loader).delegateLoader(loader)
               .enhance(object, object.getClass());
      Assert.assertNotSame(object, object2);
   }

   @Test
   public void testProxyAdapterCallbackNestedInteraction() throws Exception
   {
      ClassLoader loader = ClassLoaderAdapterCallbackTest.class.getClassLoader();
      MockService original = new MockService();
      MockService object = ClassLoaderAdapterBuilder.callingLoader(loader).delegateLoader(loader)
               .enhance(original, MockService.class);
      MockResult result = object.getResult();
      Assert.assertNotSame(result, original.getResult());
   }

   @Test
   public void testNestedProxyAdapterCallback() throws Exception
   {
      MockService object = Proxies.enhance(MockService.class, handler);
      ClassLoader loader = ClassLoaderAdapterCallbackTest.class.getClassLoader();
      MockService object2 = ClassLoaderAdapterBuilder.callingLoader(loader).delegateLoader(loader)
               .enhance(object, MockService.class);
      Assert.assertNotSame(object, object2);
   }

   @Test
   public void testJavaUtilOptionalProxy() throws Exception
   {
      ClassLoader loader = ClassLoaderAdapterCallbackTest.class.getClassLoader();
      MockOptionalService original = new MockOptionalService();
      MockOptionalService object = ClassLoaderAdapterBuilder.callingLoader(loader).delegateLoader(loader)
               .enhance(original, MockOptionalService.class);
      Assert.assertThat(object.getOptional(), instanceOf(Optional.class));
   }
}