/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.arquillian.util;

import java.util.concurrent.Callable;

import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.manager.impl.request.ConfigurationScanListener;
import org.jboss.forge.furnace.spi.ContainerLifecycleListener;
import org.jboss.forge.furnace.spi.ListenerRegistration;
import org.jboss.forge.furnace.util.Callables;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class FurnaceUtil
{

   public static <T> T waitForConfigurationRescan(Furnace furnace, Callable<T> action)
   {

      ConfigurationScanListener listener = new ConfigurationScanListener();
      ListenerRegistration<ContainerLifecycleListener> registration = furnace
               .addContainerLifecycleListener(listener);

      T result = Callables.call(action);

      while (furnace.getStatus().isStarting() || !listener.isConfigurationScanned())
      {
         try
         {
            Thread.sleep(100);
         }
         catch (InterruptedException e)
         {
            throw new RuntimeException("Sleep interrupted while waiting for configuration rescan.", e);
         }
      }

      registration.removeListener();

      return result;
   }
}
