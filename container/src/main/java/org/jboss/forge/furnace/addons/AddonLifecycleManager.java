/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.addons;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.forge.furnace.FurnaceImpl;
import org.jboss.forge.furnace.impl.graph.CompleteAddonGraph;
import org.jboss.forge.furnace.impl.graph.MasterGraph;
import org.jboss.forge.furnace.impl.graph.MasterGraphChangeHandler;
import org.jboss.forge.furnace.impl.graph.OptimizedAddonGraph;
import org.jboss.forge.furnace.lock.LockManager;
import org.jboss.forge.furnace.lock.LockMode;
import org.jboss.forge.furnace.repositories.AddonRepository;
import org.jboss.forge.furnace.util.AddonFilters;
import org.jboss.forge.furnace.util.Assert;
import org.jboss.forge.furnace.util.Callables;
import org.jboss.forge.furnace.util.Sets;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class AddonLifecycleManager
{
   private static final Logger logger = Logger.getLogger(AddonLifecycleManager.class.getName());

   private final AtomicInteger starting = new AtomicInteger(-1);
   private final ExecutorService executor = Executors.newCachedThreadPool();

   private FurnaceImpl furnace;
   private final LockManager lock;

   private Set<Addon> addons = Sets.getConcurrentSet();
   private AddonLoader loader;

   private MasterGraph currentGraph;

   public AddonLifecycleManager(FurnaceImpl furnace)
   {
      Assert.notNull(furnace, "Furnace instance must not be null.");

      this.furnace = furnace;
      this.lock = furnace.getLockManager();

      logger.log(Level.FINE, "Instantiated AddonRTegistryImpl: " + this);
   }

   private AddonLoader getAddonLoader()
   {
      if (loader == null)
         loader = new AddonLoader(furnace, this);
      return loader;
   }

   public void add(Addon addon)
   {
      addons.add(addon);
   }

   public Addon getAddon(final AddonView view, final AddonId id)
   {
      Assert.notNull(id, "AddonId must not be null.");
      return lock.performLocked(LockMode.READ, new Callable<Addon>()
      {
         private Addon result;

         @Override
         public Addon call() throws Exception
         {
            for (Addon addon : getAddons(view))
            {
               if (id.equals(addon.getId()))
               {
                  result = addon;
                  break;
               }
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

            for (Addon addon : addons)
            {
               Set<AddonView> views = ((AddonImpl) addon).getViews();
               if (views.contains(view) && filter.accept(addon))
                  result.add(addon);
            }

            return result;
         }
      });
   }

   @Override
   public String toString()
   {
      StringBuilder builder = new StringBuilder();

      Iterator<Addon> addonsIterator = addons.iterator();
      while (addonsIterator.hasNext())
      {
         Addon addon = addonsIterator.next();
         builder.append("- ").append(addon.toString());
         if (addonsIterator.hasNext())
            builder.append("\n");
      }

      return builder.toString();
   }

   public void forceUpdate(final Set<AddonView> views)
   {
      lock.performLocked(LockMode.WRITE, new Callable<Void>()
      {

         @Override
         public Void call() throws Exception
         {
            MasterGraph master = new MasterGraph();

            int i = 0;
            for (AddonView view : views)
            {
               if (starting.get() == -1)
                  starting.set(0);

               OptimizedAddonGraph graph = new OptimizedAddonGraph(view,
                        new CompleteAddonGraph(view.getRepositories()).getGraph());

               master.merge(graph);

               System.out.println(" ------------ MASTER GRAPH v" + i++ + "------------ ");
               System.out.println(master);
            }

            MasterGraph last = currentGraph;
            currentGraph = master;

            for (Addon addon : addons)
            {
               Callables.call(new StopAddonCallable(addon));
            }
            addons.clear();

            new MasterGraphChangeHandler(AddonLifecycleManager.this, last, master)
                     .hotSwapChanges(getAddonLoader());

            return null;
         }
      });
   }

   public void stopAll()
   {
      lock.performLocked(LockMode.WRITE, new Callable<Void>()
      {
         @Override
         public Void call() throws Exception
         {
            for (Addon addon : addons)
            {
               if (addon instanceof AddonImpl)
               {
                  new StopAddonCallable(addon).call();
               }
            }

            List<Runnable> waiting = executor.shutdownNow();
            if (waiting != null && !waiting.isEmpty())
               logger.info("(" + waiting.size() + ") addons were aborted while loading.");
            starting.set(-1);
            return null;
         }
      });
   }

   private Set<AddonId> getAllEnabled(Set<AddonRepository> repositories)
   {
      Set<AddonId> result = new HashSet<AddonId>();
      for (AddonRepository repository : repositories)
      {
         for (AddonId enabled : repository.listEnabled())
         {
            result.add(enabled);
         }
      }
      return result;
   }

   public void finishedStarting(AddonImpl addon)
   {
      starting.decrementAndGet();
   }

   /**
    * Returns <code>true</code> if there are currently any Addons being started.
    */
   public boolean isStartingAddons(Set<AddonView> views)
   {
      if (starting.get() == -1)
         return false;

      return starting.get() > 0;
   }

   public Set<AddonRepository> getRepositories()
   {
      return Collections.unmodifiableSet(new LinkedHashSet<AddonRepository>(furnace.getRepositories()));
   }

   public AddonId resolve(AddonView view, final String name)
   {
      Assert.notNull(name, "Addon name must not be null.");
      AddonId result = null;
      for (AddonId id : getAllEnabled(view.getRepositories()))
      {
         if (name.equals(id.getName()) && (result == null || id.getVersion().compareTo(result.getVersion()) >= 0))
            result = id;
      }

      return result;
   }

   public void dispose(AddonView view)
   {
      furnace.disposeAddonView(view);
   }

   public void startAddon(Addon addon)
   {
      Assert.notNull(addon, "Addon to start must not be null.");
      Callables.call(new StartEnabledAddonCallable(furnace, executor, starting, (AddonImpl) addon));
   }

   public AddonView getRootView()
   {
      return furnace.getAddonRegistry();
   }

}
