/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package test.org.jboss.forge.furnace.classpath;

import java.io.ByteArrayInputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.annotation.XmlRootElement;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assume.assumeThat;

@RunWith(Arquillian.class)
public class JAXBLookupTest
{
   @Deployment
   public static AddonArchive getDeployment()
   {
      AddonArchive archive = ShrinkWrap.create(AddonArchive.class)
               .addClass(TestRoot.class)
               .addAsLocalServices(JAXBLookupTest.class);

      return archive;
   }

   @Test
   public void testGetJDKProvidedJAXBImpl() throws Exception
   {
      assumeThat(System.getProperty("java.version"), startsWith("1.8"));
      try
      {
         getClass().getClassLoader().loadClass("com.sun.xml.internal.bind.v2.ContextFactory");
      }
      catch (Exception e)
      {
         Assert.fail("Could not load required Factory class." + e.getMessage());
      }
   }

   @Test
   public void testJAXBLookup() throws Exception
   {
      assumeThat(System.getProperty("java.version"), startsWith("1.8"));
      Assert.assertNotNull(JAXBContext.newInstance(TestRoot.class).createUnmarshaller()
               .unmarshal(new ByteArrayInputStream("<testRoot/>".getBytes())));
   }

   @XmlRootElement
   public static class TestRoot
   {

   }
}
