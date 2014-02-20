/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package test.org.jboss.forge.furnace.services;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.forge.arquillian.services.LocalServices;
import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.services.Imported;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import test.org.jboss.forge.furnace.mocks.MockImpl1;
import test.org.jboss.forge.furnace.mocks.MockImpl2;
import test.org.jboss.forge.furnace.mocks.MockInterface;

@RunWith(Arquillian.class)
public class ImportedTest
{
   @Deployment
   public static ForgeArchive getDeployment()
   {
      ForgeArchive archive = ShrinkWrap.create(ForgeArchive.class);

      archive.addClass(MockInterface.class);
      archive.addClass(MockImpl1.class);
      archive.addClass(MockImpl2.class);

      archive.addAsLocalServices(MockImpl1.class, MockImpl2.class, ImportedTest.class);

      return archive;
   }

   @Test
   public void testIsAmbiguous() throws Exception
   {
      Furnace furnace = LocalServices.getFurnace(getClass().getClassLoader());
      AddonRegistry registry = furnace.getAddonRegistry();
      Imported<MockInterface> services = registry.getServices(MockInterface.class);
      Assert.assertFalse(services.isUnsatisfied());
      Assert.assertTrue(services.isAmbiguous());
   }
}
