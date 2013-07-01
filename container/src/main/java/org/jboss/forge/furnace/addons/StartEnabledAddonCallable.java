package org.jboss.forge.furnace.addons;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.impl.AddonImpl;
import org.jboss.forge.furnace.impl.AddonRunnable;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class StartEnabledAddonCallable implements Callable<Void>
{
   private ExecutorService executor;
   private AtomicInteger starting;
   private AddonImpl addon;
   private Furnace furnace;

   public StartEnabledAddonCallable(Furnace furnace, ExecutorService executor, AtomicInteger starting,
            AddonImpl toStart)
   {
      this.furnace = furnace;
      this.executor = executor;
      this.starting = starting;
      this.addon = toStart;
   }

   @Override
   public Void call()
   {
      if (addon.canBeStarted())
      {
         if (executor.isShutdown())
         {
            throw new IllegalStateException("Cannot start additional addons once Shutdown has been initiated.");
         }

         Future<Void> result = null;
         if (addon.getRunnable() == null)
         {
            starting.incrementAndGet();
            AddonRunnable runnable = new AddonRunnable(furnace, addon);
            result = executor.submit(runnable, null);
            addon.setFuture(result);
            addon.setRunnable(runnable);
         }
      }
      return null;
   }

}
