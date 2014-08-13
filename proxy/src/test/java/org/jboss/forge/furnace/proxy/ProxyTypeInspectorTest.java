/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.proxy;

import java.io.Serializable;

import org.jboss.forge.furnace.proxy.mock.MockBaseClassExternal;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class ProxyTypeInspectorTest
{
   @Test
   public void testExceptionHierarchyFindsThrowable() throws Exception
   {
      Class<?>[] hierarchy = ProxyTypeInspector.getCompatibleClassHierarchy(getClass().getClassLoader(),
               MockException.class);

      Assert.assertEquals(2, hierarchy.length);
      Assert.assertEquals(Exception.class, hierarchy[0]);
      Assert.assertEquals(Serializable.class, hierarchy[1]);
   }

   @Test
   public void testClassWithInstantiableBaseClass() throws Exception
   {
      Class<?>[] hierarchy = ProxyTypeInspector.getCompatibleClassHierarchy(getClass().getClassLoader(),
               MockExtendsImplementsExternal.class);

      Assert.assertEquals(MockBaseClassExternal.class, hierarchy[0]);
      Assert.assertEquals(MockInterface.class, hierarchy[1]);
      Assert.assertEquals(MockNestedInterface.class, hierarchy[2]);
   }

   @Test
   public void testInnerClassWithNonInstantiableBaseClass() throws Exception
   {
      Class<?>[] hierarchy = ProxyTypeInspector.getCompatibleClassHierarchy(getClass().getClassLoader(),
               MockExtendsImplementsInternal.class);

      Assert.assertEquals(MockInterface.class, hierarchy[0]);
      Assert.assertEquals(MockNestedInterface.class, hierarchy[1]);
   }

   private class MockExtendsImplementsInternal extends MockBaseClass implements MockInterface
   {

   }

   private class MockExtendsImplementsExternal extends MockBaseClassExternal implements MockInterface
   {

   }

   public class MockBaseClass implements MockNestedInterface
   {

   }

   public interface MockInterface
   {

   }

   public interface MockNestedInterface
   {

   }

   public class MockException extends Exception
   {
      private static final long serialVersionUID = -5453820012282246775L;
   }
}
