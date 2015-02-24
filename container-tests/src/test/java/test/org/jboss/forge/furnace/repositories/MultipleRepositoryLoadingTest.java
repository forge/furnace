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
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.forge.arquillian.services.LocalServices;
import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.forge.furnace.util.Assert;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class MultipleRepositoryLoadingTest
{
   @Deployment(order = 0)
   @DeployToRepository("1")
   public static AddonArchive getDeployment()
   {
      AddonArchive archive = ShrinkWrap
               .create(AddonArchive.class)
               .addAsAddonDependencies(
                        AddonDependencyEntry.create("dep1", "1"),
                        AddonDependencyEntry.create("dep2", "2")
               );

      archive.addAsLocalServices(MultipleRepositoryLoadingTest.class);

      return archive;
   }

   @DeployToRepository("2")
   @Deployment(name = "dep1,1", testable = false, order = 5)
   public static AddonArchive getDeployment1()
   {
      return ShrinkWrap.create(AddonArchive.class).addBeansXML();
   }

   @DeployToRepository("2")
   @Deployment(name = "dep2,2", testable = false, order = 1)
   public static AddonArchive getDeployment2()
   {
      return ShrinkWrap.create(AddonArchive.class).addBeansXML();
   }

   @Test
   public void testAddonDependenciesLoadedAcrossRepositories() throws Exception
   {
      Furnace furnace = LocalServices.getFurnace(getClass().getClassLoader());
      Assert.notNull(furnace, "Furnace instance was null");
   }
}
