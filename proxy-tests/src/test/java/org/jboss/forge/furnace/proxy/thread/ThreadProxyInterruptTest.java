/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.proxy.thread;

import java.util.concurrent.atomic.AtomicReference;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.forge.arquillian.services.LocalServices;
import org.jboss.forge.classloader.mock.MockSimpleCountService;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.exception.ContainerException;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class ThreadProxyInterruptTest
{
   @Deployment(order = 1)
   public static ForgeArchive getDeployment()
   {
      ForgeArchive archive = ShrinkWrap.create(ForgeArchive.class)
               .addAsLocalServices(ThreadProxyInterruptTest.class)
               .addAsAddonDependencies(AddonDependencyEntry.create("dep"));

      return archive;
   }

   @Deployment(name = "dep,1", testable = false, order = 0)
   public static ForgeArchive getDeploymentDep1()
   {
      ForgeArchive archive = ShrinkWrap.create(ForgeArchive.class)
               .addClasses(MockSimpleCountService.class)
               .addAsLocalServices(MockSimpleCountService.class);

      return archive;
   }

   @Test
   public void testServiceProxiesCanBeInterrupted() throws Exception
   {
      AddonRegistry registry = LocalServices.getFurnace(getClass().getClassLoader())
               .getAddonRegistry();

      final MockSimpleCountService service = registry.getServices(MockSimpleCountService.class).get();
      final AtomicReference<ContainerException> exception = new AtomicReference<>();
      Thread t = new Thread(new Runnable()
      {
         @Override
         public void run()
         {
            try
            {
               while (true)
               {
                  service.execute();
               }
            }
            catch (ContainerException e)
            {
               exception.set(e);
            }

            if (!Thread.currentThread().isInterrupted())
               throw new RuntimeException("Should have been interrupted at this point.");
         }
      });

      Assert.assertNull(exception.get());

      t.start();
      Thread.sleep(250);
      t.interrupt();
      Thread.sleep(250);

      Assert.assertTrue(service.execute() > 0);
      Assert.assertNotNull(exception.get());
   }
}
