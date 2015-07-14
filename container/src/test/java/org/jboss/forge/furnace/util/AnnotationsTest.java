/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.util;

import static org.hamcrest.CoreMatchers.notNullValue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class AnnotationsTest
{
   @Retention(RetentionPolicy.RUNTIME)
   public @interface MockAnnotation
   {

   }

   @Retention(RetentionPolicy.RUNTIME)
   @Target(ElementType.METHOD)
   public @interface MockMethodAnnotation
   {

   }

   public interface NestedInterface
   {
      @MockAnnotation
      @MockMethodAnnotation
      public void method();
   }

   @Test
   public void testMethodAnnotation() throws Exception
   {
      MockMethodAnnotation annotation = Annotations.getAnnotation(NestedInterface.class.getMethod("method"),
               MockMethodAnnotation.class);
      Assert.assertThat(annotation, notNullValue());
   }

   @Test
   public void testInheritFromInterface() throws Exception
   {
      Assert.assertTrue(Annotations.isAnnotationPresent(InheritsRemote.class, MockAnnotation.class));
   }

   @Test
   public void testInheritFromInterfaceInheritingRemote() throws Exception
   {
      Assert.assertTrue(Annotations
               .isAnnotationPresent(InheritsRemoteFromExtendedInterface.class, MockAnnotation.class));
   }

   @Test
   public void testInheritFromSuperclass() throws Exception
   {
      Assert.assertTrue(Annotations.isAnnotationPresent(SuperClassAnnotatedWithRemote.class, MockAnnotation.class));
   }

   @Test
   public void testInheritFromSuperclassInheritingRemote() throws Exception
   {
      Assert.assertTrue(Annotations.isAnnotationPresent(InheritsRemoteFromSuperClassInheriting.class,
               MockAnnotation.class));
   }

   public class InheritsRemote implements AnnotatedWithRemote
   {

   }

   @MockAnnotation
   public interface AnnotatedWithRemote
   {

   }

   public class InheritsRemoteFromExtendedInterface implements ExtendsRemoteInterface
   {

   }

   public interface ExtendsRemoteInterface extends AnnotatedWithRemote
   {

   }

   @MockAnnotation
   public class SuperClassAnnotatedWithRemote
   {

   }

   public class InheritsRemoteFromSuperClassInheriting extends SuperClassInheritsFromInterface
   {

   }

   public class SuperClassInheritsFromInterface implements AnnotatedWithRemote
   {

   }
}