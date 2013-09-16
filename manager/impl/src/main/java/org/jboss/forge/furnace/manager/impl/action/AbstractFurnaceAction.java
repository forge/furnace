/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.manager.impl.action;

import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.manager.request.FurnaceAction;
import org.jboss.forge.furnace.manager.request.FurnaceIsolationType;
import org.jboss.forge.furnace.spi.ContainerLifecycleListener;
import org.jboss.forge.furnace.spi.ListenerRegistration;
import org.jboss.forge.furnace.util.Assert;

/**
 * Default implementation of {@link FurnaceAction}.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public abstract class AbstractFurnaceAction implements FurnaceAction
{
   protected final Furnace furnace;

   public AbstractFurnaceAction(Furnace furnace)
   {
      Assert.notNull(furnace, "Furnace must not be null.");
      this.furnace = furnace;
   }

   public abstract void execute();

   @Override
   public final void perform()
   {
      perform(FurnaceIsolationType.CONFIGURATION_RELOAD);
   }

   @Override
   public void perform(FurnaceIsolationType type)
   {
      switch (type)
      {
      case CONFIGURATION_RELOAD:
         executeAndWaitForReload();
         break;
      case NONE:
         execute();
      default:
         break;
      }
   }

   private void executeAndWaitForReload()
   {
      ConfigurationScanListener listener = new ConfigurationScanListener();
      ListenerRegistration<ContainerLifecycleListener> reg = furnace.addContainerLifecycleListener(listener);
      try
      {
         execute();
         if (!furnace.getStatus().isStopped())
         {
            while (furnace.getStatus().isStarting() || !listener.isConfigurationScanned())
            {
               try
               {
                  Thread.sleep(100);
               }
               catch (InterruptedException e)
               {
                  throw new RuntimeException(e);
               }
            }
         }
      }
      finally
      {
         reg.removeListener();
      }
   }

}
