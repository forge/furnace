/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.proxy.test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.forge.furnace.proxy.ClassLoaderInterceptor;
import org.jboss.forge.furnace.proxy.ForgeProxy;
import org.jboss.forge.furnace.proxy.Proxies;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.Proxy;
import javassist.util.proxy.ProxyObject;
import org.junit.Assert;
import org.junit.Test;

public class ProxiesTest
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
         return new MockType();
      }

      @Override
      public Object getHandler() throws Exception
      {
         return this;
      }
   };

   public class MemberClass
   {
   }

   @Test
   public void testNestedProxy() throws Exception
   {
      Object object = Proxies.enhance(MockType.class, handler);
      Proxies.enhance(object.getClass(), handler);
   }

   @Test(expected = Exception.class)
   public void testCannotProxyMemberClass() throws Exception
   {
      Proxies.enhance(MemberClass.class, handler);
   }

   @Test
   public void testUnwrapProxyTypes()
   {
      BeanWithSuperClass enhancedObj = Proxies.enhance(BeanWithSuperClass.class, new ForgeProxy()
      {
         private final Object bean = new BeanWithSuperClass();

         @Override
         public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable
         {
            return proceed.invoke(self, args);
         }

         @Override
         public Object getDelegate()
         {
            return bean;
         }

         @Override
         public Object getHandler() throws Exception
         {
            return this;
         }
      });
      Assert.assertNotEquals(BeanWithSuperClass.class.getName(), enhancedObj.getClass().getName());
      Class<?> result = Proxies.unwrapProxyTypes(enhancedObj.getClass());
      Assert.assertEquals(BeanWithSuperClass.class, result);
   }

   @Test
   public void testUnwrapProxyClassName()
   {
      Bean enhancedObj = Proxies.enhance(Bean.class, new ForgeProxy()
      {
         @Override
         public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable
         {
            return null;
         }

         @Override
         public Object getDelegate()
         {
            return null;
         }

         @Override
         public Object getHandler() throws Exception
         {
            return this;
         }
      });
      Assert.assertNotEquals(Bean.class.getName(), enhancedObj.getClass().getName());
      String result = Proxies.unwrapProxyClassName(enhancedObj.getClass());
      Assert.assertEquals(Bean.class.getName(), result);
   }

   @Test
   public void testAreEquivalent()
   {
      Bean enhancedObj = Proxies.enhance(Bean.class, new ForgeProxy()
      {

         @Override
         public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable
         {
            return proceed.invoke(self, args);
         }

         @Override
         public Object getDelegate()
         {
            return null;
         }

         @Override
         public Object getHandler() throws Exception
         {
            return this;
         }
      });
      enhancedObj.setAtt("String");
      Bean bean2 = new Bean();
      bean2.setAtt("String");

      Assert.assertTrue(Proxies.areEquivalent(enhancedObj, bean2));
   }

   @Test
   public void testEqualsAndHashCode()
   {
      Bean bean1 = new Bean();
      String attributeValue = "String";
      bean1.setAtt(attributeValue);
      Bean enhancedObj = Proxies.enhance(Bean.class, new ClassLoaderInterceptor(Bean.class.getClassLoader(), bean1));
      enhancedObj.setAtt(attributeValue);

      Bean bean2 = new Bean();
      bean2.setAtt(attributeValue);

      Assert.assertTrue(enhancedObj.equals(bean2));
   }

   @Test
   public void testIsInstance()
   {
      Bean enhancedObj = Proxies.enhance(Bean.class, new ForgeProxy()
      {

         @Override
         public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable
         {
            return proceed.invoke(self, args);
         }

         @Override
         public Object getDelegate() throws Exception
         {
            return null;
         }

         @Override
         public Object getHandler() throws Exception
         {
            return this;
         }
      });
      Assert.assertTrue(Proxies.isInstance(Bean.class, enhancedObj));
   }

   @Test
   public void testIsInstantiable() throws Exception
   {
      Assert.assertFalse(Proxies.isInstantiable(TypeWithNonDefaultConstructor.class));
      Assert.assertTrue(Proxies.isInstantiable(Bean.class));
   }

   @Test
   public void testIsLanguageType() throws Exception
   {
      Assert.assertTrue(Proxies.isLanguageType(Object[].class));
      Assert.assertTrue(Proxies.isLanguageType(byte[].class));
      Assert.assertTrue(Proxies.isLanguageType(float[].class));
      Assert.assertTrue(Proxies.isLanguageType(char[].class));
      Assert.assertTrue(Proxies.isLanguageType(double[].class));
      Assert.assertTrue(Proxies.isLanguageType(int[].class));
      Assert.assertTrue(Proxies.isLanguageType(long[].class));
      Assert.assertTrue(Proxies.isLanguageType(short[].class));
      Assert.assertTrue(Proxies.isLanguageType(boolean[].class));
      Assert.assertTrue(Proxies.isLanguageType(Object.class));
      Assert.assertTrue(Proxies.isLanguageType(InputStream.class));
      Assert.assertTrue(Proxies.isLanguageType(Runnable.class));
      Assert.assertTrue(Proxies.isLanguageType(String.class));
      Assert.assertTrue(Proxies.isLanguageType(Class.class));
      Assert.assertTrue(Proxies.isLanguageType(ClassLoader.class));
      Assert.assertTrue(Proxies.isLanguageType(BigDecimal.class));
      Assert.assertTrue(Proxies.isLanguageType(List.class));
      Assert.assertTrue(Proxies.isLanguageType(Set.class));
      Assert.assertTrue(Proxies.isLanguageType(Iterable.class));
      Assert.assertTrue(Proxies.isLanguageType(Map.class));
   }

   @Test
   public void testCertainLanguageTypesRequireProxying() throws Exception
   {
      Assert.assertTrue(Proxies.isPassthroughType(Object[].class));
      Assert.assertTrue(Proxies.isPassthroughType(byte[].class));
      Assert.assertTrue(Proxies.isPassthroughType(float[].class));
      Assert.assertTrue(Proxies.isPassthroughType(char[].class));
      Assert.assertTrue(Proxies.isPassthroughType(double[].class));
      Assert.assertTrue(Proxies.isPassthroughType(int[].class));
      Assert.assertTrue(Proxies.isPassthroughType(long[].class));
      Assert.assertTrue(Proxies.isPassthroughType(short[].class));
      Assert.assertTrue(Proxies.isPassthroughType(boolean[].class));
      Assert.assertTrue(Proxies.isPassthroughType(Object.class));
      Assert.assertTrue(Proxies.isPassthroughType(InputStream.class));
      Assert.assertTrue(Proxies.isPassthroughType(Runnable.class));
      Assert.assertTrue(Proxies.isPassthroughType(String.class));
      Assert.assertTrue(Proxies.isPassthroughType(Class.class));
      Assert.assertTrue(Proxies.isPassthroughType(ClassLoader.class));
      Assert.assertFalse(Proxies.isPassthroughType(BigDecimal.class));
      Assert.assertFalse(Proxies.isPassthroughType(List.class));
      Assert.assertFalse(Proxies.isPassthroughType(Set.class));
      Assert.assertFalse(Proxies.isPassthroughType(Iterable.class));
      Assert.assertFalse(Proxies.isPassthroughType(Map.class));
   }

   @Test
   public void testIsProxyType() throws Exception
   {
      Assert.assertTrue(Proxies.isProxyType(new Proxy()
      {
         @Override
         public void setHandler(MethodHandler mi)
         {
         }
      }.getClass()));
   }

   @Test
   public void testIsProxyObjectType() throws Exception
   {
      Assert.assertTrue(Proxies.isProxyType(new ProxyObject()
      {
         @Override
         public void setHandler(MethodHandler mi)
         {
         }

         @Override
         public MethodHandler getHandler()
         {
            return null;
         }
      }.getClass()));
   }

   @Test(expected = IllegalArgumentException.class)
   public void testEnhanceInvalidArguments1() throws Exception
   {
      Proxies.enhance(null, handler);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testEnhanceInvalidArguments2() throws Exception
   {
      Proxies.enhance(ProxiesTest.class, null);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testEnhanceInvalidArguments3() throws Exception
   {
      Proxies.enhance(null, null);
   }

   @Test
   public void testEnhanceNullPassthrough() throws Exception
   {
      Assert.assertNull(Proxies.enhance(getClass().getClassLoader(), null, handler));
   }

   @Test(expected = IllegalArgumentException.class)
   public void testEnhanceInvalidArguments4() throws Exception
   {
      Proxies.enhance(null, null, handler);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testEnhanceInvalidArguments5() throws Exception
   {
      Proxies.enhance(getClass().getClassLoader(), null, null);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testEnhanceInvalidArguments6() throws Exception
   {
      Proxies.enhance(getClass().getClassLoader(), new ProxiesTest(), null);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testEnhanceInvalidArguments7() throws Exception
   {
      Proxies.enhance(null, new ProxiesTest(), handler);
   }

   @Test
   public void testUnwrapProxyTypesNullClassLoaderList() throws Exception
   {
      Proxies.unwrapProxyTypes(getClass(), (ClassLoader[]) null);
   }

   @Test
   public void testUnwrapProxyTypesNullType() throws Exception
   {
      Proxies.unwrapProxyTypes(null, getClass().getClassLoader());
   }

}
