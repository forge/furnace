/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package test.org.jboss.forge.furnace.classpath;

import static org.hamcrest.CoreMatchers.startsWith;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class JavaFXLookupTest
{
   @Deployment
   public static AddonArchive getDeployment()
   {
      AddonArchive archive = ShrinkWrap.create(AddonArchive.class)
               .addAsLocalServices(JavaFXLookupTest.class);

      return archive;
   }

   @Test
   public void testGetJDKProvidedJavaFXImpl() throws Exception
   {
      Assume.assumeThat(System.getProperty("java.version"), startsWith("1.8"));
      Assert.assertNotNull(
               getClass().getClassLoader().loadClass("javafx.animation.Animation"));
   }
}
