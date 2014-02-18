/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.classloader;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.LazyLoader;

import org.jboss.forge.classloader.mock.MockService;
import org.jboss.forge.classloader.mock.Result;
import org.jboss.forge.furnace.proxy.ClassLoaderAdapterBuilder;
import org.jboss.forge.furnace.proxy.Proxies;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class ClassLoaderAdapterProxiedTest
{

   @Test
   public void testAdaptProxiedType() throws Exception
   {
      ClassLoader loader = MockService.class.getClassLoader();
      final MockService internal = new MockService();

      MockService delegate = (MockService) Enhancer.create(MockService.class, new LazyLoader()
      {
         @Override
         public Object loadObject() throws Exception
         {
            return internal;
         }
      });

      MockService adapter = ClassLoaderAdapterBuilder.callingLoader(loader).delegateLoader(loader)
               .enhance(delegate, MockService.class);

      Assert.assertNotEquals(internal.getResult(), adapter.getResult());
      Assert.assertEquals(internal.getResult().getValue(), adapter.getResult().getValue());
   }

   @Test
   public void testAdaptProxiedResult() throws Exception
   {
      ClassLoader loader = MockService.class.getClassLoader();
      final MockService internal = new MockService();

      MockService delegate = (MockService) Enhancer.create(MockService.class, new LazyLoader()
      {
         @Override
         public Object loadObject() throws Exception
         {
            return internal;
         }
      });

      MockService adapter = ClassLoaderAdapterBuilder.callingLoader(loader).delegateLoader(loader)
               .enhance(delegate, MockService.class);

      Assert.assertNotEquals(internal.getResult(), adapter.getResultEnhanced());
   }

   @Test
   public void testAdaptProxiedResultReturnTypeObject() throws Exception
   {
      ClassLoader loader = MockService.class.getClassLoader();
      final MockService internal = new MockService();

      MockService delegate = (MockService) Enhancer.create(MockService.class, new LazyLoader()
      {
         @Override
         public Object loadObject() throws Exception
         {
            return internal;
         }
      });

      MockService adapter = ClassLoaderAdapterBuilder.callingLoader(loader).delegateLoader(loader)
               .enhance(delegate, MockService.class);

      Assert.assertNotEquals(internal.getResult(), adapter.getResultEnhancedReturnTypeObject());
   }

   @Test(expected = ClassCastException.class)
   public void testCannotAdaptFinalResultReturnType() throws Exception
   {
      ClassLoader loader = MockService.class.getClassLoader();
      final MockService internal = new MockService();

      MockService delegate = (MockService) Enhancer.create(MockService.class, new LazyLoader()
      {
         @Override
         public Object loadObject() throws Exception
         {
            return internal;
         }
      });

      MockService adapter = ClassLoaderAdapterBuilder.callingLoader(loader).delegateLoader(loader)
               .enhance(delegate, MockService.class);
      adapter.getResultFinalReturnType();
   }

   @Test
   public void testCanAdaptFinalResultReturnTypeObject() throws Exception
   {
      ClassLoader loader = MockService.class.getClassLoader();
      final MockService internal = new MockService();

      MockService delegate = (MockService) Enhancer.create(MockService.class, new LazyLoader()
      {
         @Override
         public Object loadObject() throws Exception
         {
            return internal;
         }
      });

      MockService adapter = ClassLoaderAdapterBuilder.callingLoader(loader).delegateLoader(loader)
               .enhance(delegate, MockService.class);

      Assert.assertNotEquals(internal.getResultFinalReturnTypeObject(), adapter.getResultFinalReturnTypeObject());
      Assert.assertEquals(((Result) internal.getResultFinalReturnTypeObject()).getValue(),
               ((Result) adapter.getResultFinalReturnTypeObject()).getValue());
   }

   @Test
   public void testAdaptFinalResult() throws Exception
   {
      ClassLoader loader = MockService.class.getClassLoader();
      final MockService internal = new MockService();

      MockService delegate = (MockService) Enhancer.create(MockService.class, new LazyLoader()
      {
         @Override
         public Object loadObject() throws Exception
         {
            return internal;
         }
      });

      MockService adapter = ClassLoaderAdapterBuilder.callingLoader(loader).delegateLoader(loader)
               .enhance(delegate, MockService.class);

      Assert.assertNotEquals(internal.getResultInterfaceFinalImpl(), adapter.getResultInterfaceFinalImpl());
      Object unwrapped = Proxies.unwrap(adapter);
      Assert.assertNotSame(adapter, unwrapped);
   }

   @Test
   public void testNullValuesAsMethodParameters() throws Exception
   {
      ClassLoader loader = MockService.class.getClassLoader();
      final MockService internal = new MockService();

      MockService delegate = (MockService) Enhancer.create(MockService.class, new LazyLoader()
      {
         @Override
         public Object loadObject() throws Exception
         {
            return internal;
         }
      });
      MockService adapter = ClassLoaderAdapterBuilder.callingLoader(loader).delegateLoader(loader)
               .enhance(delegate, MockService.class);
      Assert.assertNull(internal.echo(null));
      Assert.assertNull(adapter.echo(null));
   }
}