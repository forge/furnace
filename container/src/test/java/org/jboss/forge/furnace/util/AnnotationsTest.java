package org.jboss.forge.furnace.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

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