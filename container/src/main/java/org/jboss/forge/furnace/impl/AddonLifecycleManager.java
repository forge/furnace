/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.impl;

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

import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonFilter;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.addons.AddonView;
import org.jboss.forge.furnace.addons.StartEnabledAddonCallable;
import org.jboss.forge.furnace.addons.StopAddonCallable;
import org.jboss.forge.furnace.impl.graph.CompleteAddonGraph;
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
public class AddonLifecycleManager implements AddonView
{
   private static final Logger logger = Logger.getLogger(AddonLifecycleManager.class.getName());

   private final AtomicInteger starting = new AtomicInteger(-1);
   private final ExecutorService executor = Executors.newCachedThreadPool();

   private Furnace furnace;
   private final LockManager lock;

   private Set<Addon> addons = Sets.getConcurrentSet();
   private AddonLoader loader;

   public AddonLifecycleManager(Furnace furnace)
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

   public void add(AddonImpl addon)
   {
      this.addons.add(addon);
   }

   @Override
   public Addon getAddon(final AddonId id)
   {
      Assert.notNull(id, "AddonId must not be null.");
      return lock.performLocked(LockMode.READ, new Callable<Addon>()
      {
         private Addon result;

         @Override
         public Addon call() throws Exception
         {
            for (Addon addon : getAddons())
            {
               if (id.equals(addon.getId()))
               {
                  result = addon;
                  break;
               }
            }

            if (result == null)
            {
               result = new AddonImpl(lock, id);
               addons.add(result);
            }

            return result;
         }
      });
   }

   @Override
   public Set<Addon> getAddons()
   {
      return getAddons(AddonFilters.all());
   }

   @Override
   public Set<Addon> getAddons(final AddonFilter filter)
   {
      return lock.performLocked(LockMode.READ, new Callable<Set<Addon>>()
      {
         @Override
         public Set<Addon> call() throws Exception
         {
            HashSet<Addon> result = new HashSet<Addon>();

            for (Addon addon : addons)
            {
               if (filter.accept(addon))
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

      Iterator<Addon> iterator = addons.iterator();
      while (iterator.hasNext())
      {
         Addon addon = iterator.next();
         builder.append(addon.toString());
         if (iterator.hasNext())
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
            Set<AddonId> allEnabled = new HashSet<AddonId>();
            for (AddonView view : views)
            {
               if (starting.get() == -1)
                  starting.set(0);

               Set<AddonId> enabled = getAllEnabled(view.getRepositories());
               allEnabled.addAll(enabled);

               // CompleteAddonGraph graph = new CompleteAddonGraph(furnace.getRepositories());
               // OptimizedAddonGraph optimizedGraph = new OptimizedAddonGraph(furnace.getRepositories(),
               // graph.getGraph());

               for (AddonId id : enabled)
               {
                  try
                  {
                     AddonImpl addon = getAddonLoader().loadAddon(view, id);
//                     Callables.call(new StartEnabledAddonCallable(furnace, executor, starting, addon));
                  }
                  catch (Exception e)
                  {
                     e.printStackTrace();
                  }
               }

               // System.out.println(" ------------ DUMPING GRAPHS ------------ ");
               // System.out.println(graph);
               // System.out.println(optimizedGraph);
            }

            for (Addon addon : getAddons())
            {
               if (allEnabled.contains(addon.getId()))
               {
                  Callables.call(new StopAddonCallable(addon));
               }
            }

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

      /*
       * Force a full configuration rescan.
       */
      forceUpdate(views);
      return starting.get() > 0;
   }

   @Override
   public Set<AddonRepository> getRepositories()
   {
      return Collections.unmodifiableSet(new LinkedHashSet<AddonRepository>(furnace.getRepositories()));
   }

   public AddonId resolve(AddonView view, final String name)
   {
      Set<Addon> addons = view.getAddons(new AddonFilter()
      {
         @Override
         public boolean accept(Addon addon)
         {
            return name.equals(addon.getId().getName());
         }
      });

      AddonId result = null;
      if (addons.isEmpty())
      {
         for (Addon addon : addons)
         {
            AddonId id = addon.getId();
            if (result == null || id.getVersion().compareTo(result.getVersion()) >= 0)
               result = id;
         }
      }

      return result;
   }

}
