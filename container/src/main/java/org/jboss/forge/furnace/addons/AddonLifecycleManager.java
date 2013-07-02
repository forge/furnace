/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.addons;

import java.util.Collection;
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
import org.jboss.forge.furnace.impl.graph.AddonDependencyEdge;
import org.jboss.forge.furnace.impl.graph.AddonVertex;
import org.jboss.forge.furnace.impl.graph.CompleteAddonGraph;
import org.jboss.forge.furnace.impl.graph.OptimizedAddonGraph;
import org.jboss.forge.furnace.lock.LockManager;
import org.jboss.forge.furnace.lock.LockMode;
import org.jboss.forge.furnace.repositories.AddonRepository;
import org.jboss.forge.furnace.util.AddonFilters;
import org.jboss.forge.furnace.util.Assert;
import org.jboss.forge.furnace.util.Callables;
import org.jboss.forge.furnace.util.Sets;
import org.jboss.forge.furnace.versions.EmptyVersion;
import org.jgrapht.event.TraversalListenerAdapter;
import org.jgrapht.traverse.DepthFirstIterator;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class AddonLifecycleManager implements AddonView
{
   private static final Logger logger = Logger.getLogger(AddonLifecycleManager.class.getName());

   private final AtomicInteger starting = new AtomicInteger(-1);
   private final ExecutorService executor = Executors.newCachedThreadPool();

   private FurnaceImpl furnace;
   private final LockManager lock;

   private Set<Addon> addons = Sets.getConcurrentSet();
   private AddonLoader loader;

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
            for (AddonView view : views)
            {
               if (starting.get() == -1)
                  starting.set(0);

               CompleteAddonGraph graph = new CompleteAddonGraph(view.getRepositories());
               OptimizedAddonGraph optimizedGraph = new OptimizedAddonGraph(view.getRepositories(),
                        graph.getGraph());

               System.out.println(" ------------ DEPEDENCY SETS ------------ ");
               System.out.println(graph);
               System.out.println(" ------------ REALTIME GRAPH ------------ ");
               System.out.println(optimizedGraph);

               doStart(view, optimizedGraph);
            }

            return null;
         }

         private void doStart(final AddonView view, OptimizedAddonGraph optimizedGraph)
         {
            DepthFirstIterator<AddonVertex, AddonDependencyEdge> iterator = new DepthFirstIterator<AddonVertex, AddonDependencyEdge>(
                     optimizedGraph.getGraph());

            for (Addon addon : getAddons())
            {
               Callables.call(new StopAddonCallable(addon));
            }
            addons.clear();

            iterator.addTraversalListener(new TraversalListenerAdapter<AddonVertex, AddonDependencyEdge>()
            {
               public void vertexTraversed(org.jgrapht.event.VertexTraversalEvent<AddonVertex> event)
               {
                  AddonVertex vertex = event.getVertex();
                  if (!(vertex.getVersion() instanceof EmptyVersion))
                  {
                     AddonId addonId = vertex.getAddonId();

                     AddonImpl addon = getAddonLoader().loadAddon(view, addonId);

                     if (addon != null && !addon.getStatus().isStarted())
                        Callables.call(new StartEnabledAddonCallable(furnace, executor, starting, addon));
                     else if (addon == null)
                        System.out.println("WRONG!");
                  }
                  else
                     System.out.println("WRONG!");
               };
            });

            while (iterator.hasNext())
               iterator.next();
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
      if (!addons.isEmpty())
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

   @Override
   public void dispose()
   {
      throw new UnsupportedOperationException("Cannot dispose the root AddonView. Call Furnace.stop() instead.");
   }

   public void dispose(AddonView view)
   {
      furnace.disposeAddonView(view);
   }

}
