/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package test.org.jboss.forge.furnace.api;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.forge.arquillian.services.LocalServices;
import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.spi.ExportedInstance;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class ExportedInstanceApiTest
{
   @Deployment(order = 0)
   public static ForgeArchive getDeployment()
   {
      ForgeArchive archive = ShrinkWrap.create(ForgeArchive.class)
               .addAsAddonDependencies(
               );

      archive.addAsLocalServices(ExportedInstanceApiTest.class);

      return archive;
   }

   @Test
   public void testExportedInstanceExposesServiceTypeAndSourceAddon() throws Exception
   {
      Furnace furnace = LocalServices.getFurnace(getClass().getClassLoader());
      Assert.assertNotNull(furnace);
      AddonRegistry registry = furnace.getAddonRegistry();
      boolean found = false;
      for (Addon addon : registry.getAddons())
      {
         ExportedInstance<ExportedInstanceApiTest> instance = addon.getServiceRegistry()
                  .getExportedInstance(ExportedInstanceApiTest.class);
         if (instance != null)
         {
            found = true;
            Assert.assertEquals(ExportedInstanceApiTest.class, instance.getActualType());
            Assert.assertEquals(addon, instance.getSourceAddon());
            break;
         }
      }
      Assert.assertTrue("Could not locate service in any addon.", found);
   }
}
