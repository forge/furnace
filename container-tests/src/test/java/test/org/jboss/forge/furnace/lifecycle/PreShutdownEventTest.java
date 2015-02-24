/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package test.org.jboss.forge.furnace.lifecycle;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.forge.arquillian.services.LocalServices;
import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.forge.furnace.repositories.MutableAddonRepository;
import org.jboss.forge.furnace.util.Addons;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import test.org.jboss.forge.furnace.mocks.MockImpl1;
import test.org.jboss.forge.furnace.mocks.MockInterface;

@RunWith(Arquillian.class)
public class PreShutdownEventTest
{
   @Deployment(order = 2)
   public static AddonArchive getDeployment()
   {
      AddonArchive archive = ShrinkWrap.create(AddonArchive.class)
               .addAsAddonDependencies(
                        AddonDependencyEntry.create("dep1")
               );

      archive.addAsLocalServices(PreShutdownEventTest.class);

      return archive;
   }

   @Deployment(name = "dep2,2", testable = false, order = 1)
   public static AddonArchive getDeployment2()
   {
      AddonArchive archive = ShrinkWrap.create(AddonArchive.class)
               .addAsLocalServices(MockImpl1.class)
               .addClasses(MockImpl1.class, MockInterface.class)
               .addBeansXML();
      return archive;
   }

   @Deployment(name = "dep1,1", testable = false, order = 0)
   public static AddonArchive getDeployment1()
   {
      AddonArchive archive = ShrinkWrap.create(AddonArchive.class)
               .addClass(RecordingEventManager.class)
               .addAsLocalServices(RecordingEventManager.class);
      return archive;
   }

   @Test(timeout = 5000)
   public void testPreShutdownIsCalled() throws Exception
   {
      Furnace furnace = LocalServices.getFurnace(getClass().getClassLoader());
      AddonRegistry registry = furnace.getAddonRegistry();
      Addon dep2 = registry.getAddon(AddonId.from("dep2", "2"));
      RecordingEventManager manager = registry.getServices(RecordingEventManager.class).get();
      Assert.assertEquals(3, manager.getPostStartupCount());
      MutableAddonRepository repository = (MutableAddonRepository) furnace.getRepositories().get(0);
      repository.disable(dep2.getId());
      Addons.waitUntilStopped(dep2);
      Assert.assertEquals(1, manager.getPreShutdownCount());
   }
}
