/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package test.org.jboss.forge.furnace.classpath;

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
public class XATransactionLookupTest
{
   @Deployment
   public static AddonArchive getDeployment()
   {
      AddonArchive archive = ShrinkWrap.create(AddonArchive.class)
               .addAsLocalServices(XATransactionLookupTest.class);

      return archive;
   }

   @Test
   public void testGetJDKProvidedXATypes() throws Exception
   {
      assumeThat(System.getProperty("java.version"), startsWith("1.8"));
      try
      {
         getClass().getClassLoader().loadClass("javax.transaction.xa.XAResource");
         getClass().getClassLoader().loadClass("javax.transaction.TransactionRequiredException");
      }
      catch (Exception e)
      {
         Assert.fail("Could not load required transaction classes." + e.getMessage());
      }
   }

   @Test
   public void testXATypeInstantiations()
   {
      assumeThat(System.getProperty("java.version"), startsWith("1.8"));
      Assert.assertNotNull(new javax.transaction.xa.XAException());
      Assert.assertNotNull(new javax.transaction.TransactionRequiredException());
   }

}
