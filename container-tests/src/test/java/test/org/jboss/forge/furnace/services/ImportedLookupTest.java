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
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.exception.ContainerException;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.forge.furnace.services.Imported;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import test.org.jboss.forge.furnace.mocks.services.MockService;
import test.org.jboss.forge.furnace.mocks.services.MockServiceConsumer;
import test.org.jboss.forge.furnace.mocks.services.MockServicePayload;

@RunWith(Arquillian.class)
public class ImportedLookupTest
{
   @Deployment(order = 3)
   public static ForgeArchive getDeployment()
   {
      ForgeArchive archive = ShrinkWrap.create(ForgeArchive.class)
               .addClasses(MockService.class, MockServiceConsumer.class)
               .addAsLocalServices(ImportedLookupTest.class);

      return archive;
   }

   @Deployment(name = "dep1,1", testable = false, order = 2)
   public static ForgeArchive getDeploymentDep1()
   {
      ForgeArchive archive = ShrinkWrap.create(ForgeArchive.class)
               .addClasses(MockServiceConsumer.class, MockService.class)
               .addAsAddonDependencies(
                        AddonDependencyEntry.create("dep3")
               );

      return archive;
   }

   @Deployment(name = "dep3,1", testable = false, order = 0)
   public static ForgeArchive getDeploymentDep3()
   {
      ForgeArchive archive = ShrinkWrap.create(ForgeArchive.class)
               .addClasses(MockServicePayload.class);

      return archive;
   }

   @Test(expected = ContainerException.class)
   public void testDoesNotResolveNonService() throws Exception
   {
      AddonRegistry registry = LocalServices.getFurnace(getClass().getClassLoader())
               .getAddonRegistry();

      Imported<MockServiceConsumer> importedByName = registry.getServices(MockServiceConsumer.class.getName());
      Assert.assertTrue(importedByName.isUnsatisfied());
      importedByName.get();
   }

}
