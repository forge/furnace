/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.util;

import static org.hamcrest.CoreMatchers.notNullValue;

import org.jboss.forge.furnace.mock.InheritsRemote;
import org.jboss.forge.furnace.mock.InheritsRemoteFromExtendedInterface;
import org.jboss.forge.furnace.mock.InheritsRemoteFromSuperClassInheriting;
import org.jboss.forge.furnace.mock.MockAnnotation;
import org.jboss.forge.furnace.mock.MockMethodAnnotation;
import org.jboss.forge.furnace.mock.SuperClassAnnotatedWithRemote;
import org.jboss.forge.furnace.mock.MockInterface;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class AnnotationsTest
{
   @Test
   public void testMethodAnnotation() throws Exception
   {
      MockMethodAnnotation annotation = Annotations.getAnnotation(MockInterface.class.getMethod("method"),
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
}