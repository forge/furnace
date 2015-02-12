/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.impl.addons;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonFilter;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.addons.AddonView;
import org.jboss.forge.furnace.impl.FurnaceImpl;
import org.jboss.forge.furnace.impl.graph.CompleteAddonGraph;
import org.jboss.forge.furnace.impl.graph.MasterGraph;
import org.jboss.forge.furnace.impl.graph.MasterGraphChangeHandler;
import org.jboss.forge.furnace.impl.graph.OptimizedAddonGraph;
import org.jboss.forge.furnace.impl.modules.AddonModuleLoader;
import org.jboss.forge.furnace.lock.LockManager;
import org.jboss.forge.furnace.lock.LockMode;
import org.jboss.forge.furnace.repositories.AddonRepository;
import org.jboss.forge.furnace.util.AddonFilters;
import org.jboss.forge.furnace.util.Assert;
import org.jboss.forge.furnace.util.Callables;
import org.jboss.forge.furnace.util.Sets;
import org.jgrapht.Graph;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class AddonLifecycleManager
{
   private static final Logger logger = Logger.getLogger(AddonLifecycleManager.class.getName());

   private final LockManager lock;
   private final FurnaceImpl furnace;
   private final AddonLoader loader;
   private final AddonStateManager stateManager;

   private final Map<AddonView, Set<Addon>> addonViews = new ConcurrentHashMap<>();
   private final Map<AddonView, Long> views = new ConcurrentHashMap<AddonView, Long>();
   private final AtomicInteger starting = new AtomicInteger(-1);
   private final ExecutorService executor = Executors.newCachedThreadPool();

   private final AddonModuleLoader moduleLoader;

   public AddonLifecycleManager(FurnaceImpl furnace)
   {
      Assert.notNull(furnace, "Furnace instance must not be null.");

      this.furnace = furnace;
      this.lock = furnace.getLockManager();
      this.stateManager = new AddonStateManager(lock);
      this.moduleLoader = new AddonModuleLoader(furnace, this, stateManager);
      this.stateManager.setModuleLoader(moduleLoader);
      this.loader = new AddonLoader(furnace, this, stateManager, moduleLoader);
   }

   public void dispose()
   {
      for (AddonView view : views.keySet())
      {
         view.dispose();
      }
      this.views.clear();
      this.stateManager.dispose();
      this.loader.dispose();
      this.moduleLoader.dispose();
   }

   public long getVersion(AddonView view)
   {
      Long version = views.get(view);
      return version == null ? 0 : version;
   }

   public Addon getAddon(Set<AddonView> views, AddonId id)
   {
      Assert.notNull(views, "Addon view set must not be null.");
      Assert.isTrue(!views.isEmpty(), "Addon view set must not be empty.");
      Assert.notNull(id, "Addon ID must not be null.");

      return getAddon(views.iterator().next(), id);
   }

   public Set<Addon> getOrphanAddons(final AddonId id)
   {
      return lock.performLocked(LockMode.READ, new Callable<Set<Addon>>()
      {
         @Override
         public Set<Addon> call() throws Exception
         {
            Set<Addon> result = new HashSet<Addon>();
            for (Entry<AddonView, Set<Addon>> entry : addonViews.entrySet())
            {
               for (Addon addon : entry.getValue())
               {
                  if (addon.getId().equals(id) && stateManager.getViewsOf(addon).isEmpty())
                  {
                     result.add(addon);
                  }
               }
            }
            return result;
         }
      });
   }

   /**
    * Requires a {@link LockMode#WRITE} lock.
    */
   public Addon getAddon(final AddonView view, final AddonId id)
   {
      Assert.notNull(view, "AddonView must not be null.");
      Assert.notNull(id, "AddonId must not be null.");
      return lock.performLocked(LockMode.WRITE, new Callable<Addon>()
      {
         @Override
         public Addon call() throws Exception
         {
            Addon result = null;

            for (Addon addon : getAddons(view))
            {
               if (id.equals(addon.getId()))
               {
                  result = addon;
                  break;
               }
            }

            if (result == null)
            {
               result = stateManager.getAddonForView(view, id);
               if (result == null)
                  result = new AddonImpl(stateManager, id);

               Set<Addon> addons = _getAddonsForView(view);
               addons.add(result);
            }

            return result;
         }

      });
   }

   public Set<Addon> getAddons(final AddonView view)
   {
      return getAddons(view, AddonFilters.all());
   }

   public Set<Addon> getAddons(final AddonView view, final AddonFilter filter)
   {
      return lock.performLocked(LockMode.READ, new Callable<Set<Addon>>()
      {
         @Override
         public Set<Addon> call() throws Exception
         {
            HashSet<Addon> result = new HashSet<Addon>();

            for (Addon addon : _getAddonsForView(view))
            {
               if (filter.accept(addon))
                  result.add(addon);
            }

            return result;
         }

      });
   }

   private Set<Addon> _getAddonsForView(final AddonView view)
   {
      Set<Addon> addons = addonViews.get(view);
      if (addons == null)
      {
         addons = Sets.getConcurrentSet();
         addonViews.put(view, addons);
      }
      return addons;
   }

   public void forceUpdate()
   {
      lock.performLocked(LockMode.WRITE, new Callable<Void>()
      {

         @Override
         public Void call() throws Exception
         {
            MasterGraph master = new MasterGraph();

            for (AddonView view : views.keySet())
            {
               if (starting.get() == -1)
                  starting.set(0);

               OptimizedAddonGraph graph = new OptimizedAddonGraph(view,
                        new CompleteAddonGraph(view.getRepositories()).getGraph());

               master.merge(graph);

               if (logger.isLoggable(Level.FINE))
               {
                  String graphOutput = master.toString();
                  logger.log(Level.FINE,
                           "\n ------------ VIEW [" + view.getName() + " - " + view.hashCode() + "]------------ "
                                    + (graphOutput.isEmpty() ? "EMPTY" : graphOutput)
                                    + " ------------ END [" + view.getName() + " - " + view.hashCode()
                                    + "]------------ ");
               }
            }

            MasterGraph last = stateManager.getCurrentGraph();
            stateManager.setCurrentGraph(master);

            new MasterGraphChangeHandler(AddonLifecycleManager.this, last, master).hotSwapChanges();

            return null;
         }
      });
   }

   public void loadAddon(Addon addon)
   {
      try
      {
         loader.loadAddon(addon);
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }

   public void stopAddon(Addon addon)
   {
      Callables.call(new StopAddonCallable(stateManager, addon));
      incrementViewVersions(addon);
   }

   private void incrementViewVersions(Addon addon)
   {
      for (Entry<AddonView, Long> entry : views.entrySet())
      {
         if (stateManager.getViewsOf(addon).contains(entry.getKey()))
         {
            entry.setValue(entry.getValue() + 1);
         }
      }
   }

   public void stopAll()
   {
      lock.performLocked(LockMode.WRITE, new Callable<Void>()
      {
         @Override
         public Void call() throws Exception
         {
            for (Entry<AddonView, Set<Addon>> entry : addonViews.entrySet())
            {
               for (Addon addon : entry.getValue())
               {
                  stopAddon(addon);
               }
            }

            List<Runnable> waiting = executor.shutdownNow();
            if (waiting != null && !waiting.isEmpty())
               logger.info("(" + waiting.size() + ") addons were aborted while loading due to forced shutdown.");
            starting.set(-1);
            return null;
         }
      });
   }

   public void finishedStarting(Addon addon)
   {
      starting.decrementAndGet();
      incrementViewVersions(addon);
   }

   /**
    * Returns <code>true</code> if there are currently any Addons being started. (Non-blocking.)
    */
   public boolean isStartingAddons()
   {
      if (starting.get() == -1)
         return false;

      return starting.get() > 0;
   }

   public void startAddon(Addon addon)
   {
      Assert.notNull(addon, "Addon to start must not be null.");
      Callables.call(new StartEnabledAddonCallable(furnace, this, stateManager, executor, starting, addon));
   }

   public AddonView getRootView()
   {
      return furnace.getAddonRegistry();
   }

   public void addView(AddonView view)
   {
      this.views.put(view, 0l);
   }

   public AddonRegistry findView(AddonRepository... repositories)
   {
      AddonRegistry result = null;
      List<AddonRepository> furnaceRepositories = furnace.getRepositories();

      for (AddonView view : views.keySet())
      {
         Set<AddonRepository> viewRepositories = view.getRepositories();
         if (repositories == null || repositories.length == 0)
         {
            if (viewRepositories.containsAll(furnaceRepositories)
                     && furnaceRepositories.containsAll(viewRepositories))
               result = (AddonRegistry) view;
         }
         else if (viewRepositories.containsAll(Arrays.asList(repositories))
                  && Arrays.asList(repositories).containsAll(viewRepositories))
         {
            result = (AddonRegistry) view;
         }

         if (result != null)
            break;
      }
      return result;
   }

   public void removeView(AddonView view)
   {
      if (!views.keySet().contains(view))
         throw new IllegalArgumentException("The given view does not belong to this Furnace instance.");
      views.remove(view);
   }

   @Override
   public String toString()
   {
      StringBuilder builder = new StringBuilder();
      for (AddonView view : addonViews.keySet())
      {
         builder.append("---").append(view.toString()).append("\n");
      }
      return builder.toString();
   }

   /**
    * Return a {@link Graph} representation of the entire system.
    */
   public String toGraph()
   {
      return stateManager.getCurrentGraph().toString();
   }

}
