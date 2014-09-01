/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package test.org.jboss.forge.furnace.classpath;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class SunMiscLookupTest
{
   @Deployment
   public static ForgeArchive getDeployment()
   {
      ForgeArchive archive = ShrinkWrap.create(ForgeArchive.class)
               .addAsLocalServices(SunMiscLookupTest.class);

      return archive;
   }

   @Test
   public void testGetJDKSunMiscSignalLookup() throws Exception
   {
      try
      {
         getClass().getClassLoader().loadClass("sun.misc.Signal");
      }
      catch (Exception e)
      {
         Assert.fail("Could not load required Factory class." + e.getMessage());
      }
   }
   
   @Test
   public void testGetJDKSunNioFsLookup() throws Exception
   {
      try
      {
         getClass().getClassLoader().loadClass("sun.nio.fs.Globs");
      }
      catch (Exception e)
      {
         Assert.fail("Could not load required Factory class." + e.getMessage());
      }
   }

}
