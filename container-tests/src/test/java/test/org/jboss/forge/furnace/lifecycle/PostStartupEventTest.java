/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package test.org.jboss.forge.furnace.lifecycle;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.forge.arquillian.services.LocalServices;
import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class PostStartupEventTest
{
   @Deployment(order = 1)
   public static ForgeArchive getDeployment()
   {
      ForgeArchive archive = ShrinkWrap.create(ForgeArchive.class)
               .addAsAddonDependencies(
                        AddonDependencyEntry.create("dep1")
               );

      archive.addAsLocalServices(PostStartupEventTest.class);

      return archive;
   }

   @Deployment(name = "dep1,1", testable = false, order = 0)
   public static ForgeArchive getDeployment1()
   {
      ForgeArchive archive = ShrinkWrap.create(ForgeArchive.class)
               .addClass(RecordingEventManager.class)
               .addAsLocalServices(RecordingEventManager.class);
      return archive;
   }

   @Test
   public void testPostStartupIsCalled() throws Exception
   {
      Furnace furnace = LocalServices.getFurnace(getClass().getClassLoader());
      AddonRegistry registry = furnace.getAddonRegistry();
      RecordingEventManager manager = registry.getServices(RecordingEventManager.class).get();
      Assert.assertEquals(1, manager.getPostStartupCount());
   }
}
