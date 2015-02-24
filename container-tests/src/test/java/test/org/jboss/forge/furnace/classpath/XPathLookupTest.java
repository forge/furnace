/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package test.org.jboss.forge.furnace.classpath;

import javax.xml.xpath.XPathFactory;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class XPathLookupTest
{
   @Deployment
   public static AddonArchive getDeployment()
   {
      AddonArchive archive = ShrinkWrap.create(AddonArchive.class)
               .addAsLocalServices(XPathLookupTest.class);

      return archive;
   }

   @Test
   public void testGetJDKProvidedXPathImpl() throws Exception
   {
      try
      {
         getClass().getClassLoader().loadClass("com.sun.org.apache.xpath.internal.jaxp.XPathFactoryImpl");
         getClass().getClassLoader().loadClass("com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
         getClass().getClassLoader().loadClass("com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl");
         getClass().getClassLoader().loadClass("com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");
         getClass().getClassLoader().loadClass("com.sun.org.apache.xpath.internal.jaxp.XPathFactoryImpl");
         getClass().getClassLoader().loadClass("com.sun.xml.internal.stream.events.XMLEventFactoryImpl");
         getClass().getClassLoader().loadClass("com.sun.xml.internal.stream.XMLInputFactoryImpl");
         getClass().getClassLoader().loadClass("com.sun.xml.internal.stream.XMLOutputFactoryImpl");
         getClass().getClassLoader().loadClass("com.sun.org.apache.xerces.internal.jaxp.datatype.DatatypeFactoryImpl");
         getClass().getClassLoader().loadClass("com.sun.org.apache.xerces.internal.jaxp.validation.XMLSchemaFactory");
         getClass().getClassLoader().loadClass("com.sun.org.apache.xerces.internal.parsers.SAXParser");
      }
      catch (Exception e)
      {
         Assert.fail("Could not load required Factory class." + e.getMessage());
      }
   }

   @Test
   public void testXPathFactoryLookup()
   {
      Assert.assertNotNull(XPathFactory.newInstance().newXPath());
   }

}
