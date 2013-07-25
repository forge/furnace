/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package test.org.jboss.forge.furnace.repositories;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.DeployToRepository;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.forge.arquillian.services.LocalServices;
import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.forge.furnace.util.Assert;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

@Ignore
@RunWith(Arquillian.class)
public class AddonRepositoryLoadingTest
{
   @Deployment(order = 0)
   @DeployToRepository("1")
   public static ForgeArchive getDeployment()
   {
      ForgeArchive archive = ShrinkWrap
               .create(ForgeArchive.class)
               .addAsAddonDependencies(
                        AddonDependencyEntry.create("dep1", "1"),
                        AddonDependencyEntry.create("dep2", "2"),
                        AddonDependencyEntry.create("dep3", "3"),
                        AddonDependencyEntry.create("dep4", "4"),
                        AddonDependencyEntry.create("dep5", "5")
               );

      archive.addAsLocalServices(AddonRepositoryLoadingTest.class);

      return archive;
   }

   @DeployToRepository("2")
   @Deployment(name = "dep1,1", testable = false, order = 5)
   public static ForgeArchive getDeployment1()
   {
      return ShrinkWrap.create(ForgeArchive.class).addBeansXML();
   }

   @DeployToRepository("2")
   @Deployment(name = "dep2,2", testable = false, order = 1)
   public static ForgeArchive getDeployment2()
   {
      return ShrinkWrap.create(ForgeArchive.class).addBeansXML();
   }

   @DeployToRepository("3")
   @Deployment(name = "dep3,3", testable = false, order = 2)
   public static ForgeArchive getDeployment3()
   {
      return ShrinkWrap.create(ForgeArchive.class).addBeansXML();
   }

   @DeployToRepository("3")
   @Deployment(name = "dep4,4", testable = false, order = 3)
   public static ForgeArchive getDeployment4()
   {
      return ShrinkWrap.create(ForgeArchive.class).addBeansXML();
   }

   @DeployToRepository("3")
   @Deployment(name = "dep5,5", testable = false, order = 4)
   public static ForgeArchive getDeployment5()
   {
      return ShrinkWrap.create(ForgeArchive.class).addBeansXML();
   }

   @Test
   public void testAddonRepositoryIsCorrectInMultiViewEnvironment() throws Exception
   {
      Furnace furnace = LocalServices.getFurnace(Thread.currentThread().getContextClassLoader());
      Assert.notNull(furnace, "Furnace instance was null");
   }
}
