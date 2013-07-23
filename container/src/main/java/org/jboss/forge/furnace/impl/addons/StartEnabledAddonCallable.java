package org.jboss.forge.furnace.impl.addons;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.Addon;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class StartEnabledAddonCallable implements Callable<Void>
{
   private Furnace furnace;
   private AddonLifecycleManager lifecycleManager;
   private AddonStateManager stateManager;
   private ExecutorService executor;
   private AtomicInteger starting;
   private Addon addon;

   public StartEnabledAddonCallable(Furnace furnace,
            AddonLifecycleManager lifecycleManager,
            AddonStateManager stateManager,
            ExecutorService executor,
            AtomicInteger starting,
            Addon toStart)
   {
      this.furnace = furnace;
      this.lifecycleManager = lifecycleManager;
      this.stateManager = stateManager;
      this.executor = executor;
      this.starting = starting;
      this.addon = toStart;
   }

   @Override
   public Void call()
   {
      if (stateManager.canBeStarted(addon))
      {
         if (executor.isShutdown())
         {
            throw new IllegalStateException("Cannot start additional addons once Shutdown has been initiated.");
         }

         Future<Void> result = null;
         if (stateManager.getRunnableOf(addon) == null)
         {
            starting.incrementAndGet();
            AddonRunnable runnable = new AddonRunnable(furnace, lifecycleManager, stateManager, addon);
            result = executor.submit(runnable, null);
            stateManager.setHandles(addon, result, runnable);
         }
      }
      return null;
   }

   @Override
   public String toString()
   {
      return addon.toString();
   }
}
