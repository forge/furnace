/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.addons;

import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.forge.furnace.addons.Addon;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class StopAddonCallable implements Callable<Void>
{
   private static final Logger logger = Logger.getLogger(StopAddonCallable.class.getName());

   private AddonImpl addon;

   public StopAddonCallable(Addon addon)
   {
      super();
      if (addon instanceof AddonImpl)
         this.addon = (AddonImpl) addon;
   }

   @Override
   public Void call() throws Exception
   {
      if (addon != null)
      {
         AddonRunnable runnable = ((AddonImpl) addon).getRunnable();
         try
         {
            if (runnable != null)
            {
               runnable.shutdown();
            }
         }
         catch (Exception e)
         {
            logger.log(Level.WARNING, "Failed to shut down addon " + addon, e);
         }
         finally
         {
            addon.cancelFuture();
            addon.reset();
         }
      }
      return null;
   }

}
