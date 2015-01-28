/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package test.org.jboss.forge.furnace.lifecycle;

import java.io.File;

import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.repositories.AddonRepositoryMode;
import org.jboss.forge.furnace.se.FurnaceFactory;
import org.jboss.forge.furnace.spi.ContainerLifecycleListener;
import org.jboss.forge.furnace.spi.ListenerRegistration;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class ContainerLifecycleListenerTest
{
   @Test
   public void testContainerStartup() throws Exception
   {
      Furnace furnace = FurnaceFactory.getInstance();
      TestLifecycleListener listener = new TestLifecycleListener();
      ListenerRegistration<ContainerLifecycleListener> registration = furnace.addContainerLifecycleListener(listener);
      File temp = File.createTempFile("addonDir", "sdfsdf");
      temp.deleteOnExit();
      furnace.addRepository(AddonRepositoryMode.IMMUTABLE, temp);

      furnace.startAsync();
      waitUntilStarted(furnace);
      Assert.assertEquals(1, listener.beforeStartTimesCalled);
      Assert.assertEquals(1, listener.afterStartTimesCalled);
      registration.removeListener();
      furnace.stop();
   }

   private void waitUntilStarted(Furnace furnace) throws InterruptedException
   {
      while (!furnace.getStatus().isStarted())
      {
         Thread.sleep(150);
      }
   }
}