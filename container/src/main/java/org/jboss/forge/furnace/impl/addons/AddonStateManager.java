/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.impl.addons;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonDependency;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.addons.AddonView;
import org.jboss.forge.furnace.event.EventManager;
import org.jboss.forge.furnace.impl.graph.AddonVertex;
import org.jboss.forge.furnace.impl.graph.MasterGraph;
import org.jboss.forge.furnace.impl.modules.AddonModuleLoader;
import org.jboss.forge.furnace.lock.LockManager;
import org.jboss.forge.furnace.lock.LockMode;
import org.jboss.forge.furnace.repositories.AddonRepository;
import org.jboss.forge.furnace.spi.ServiceRegistry;
import org.jboss.forge.furnace.util.Assert;

public class AddonStateManager
{
   private final LockManager lock;
   private MasterGraph graph;
   private final Map<Addon, AddonState> states = new HashMap<Addon, AddonState>();
   private AddonModuleLoader loader;

   public AddonStateManager(LockManager lock)
   {
      this.lock = lock;
   }

   public void dispose()
   {
      this.graph = null;
      this.states.clear();
      this.loader = null;
   }

   public void setModuleLoader(AddonModuleLoader loader)
   {
      this.loader = loader;
   }

   public Set<AddonDependency> getDependenciesOf(Addon addon)
   {
      return getState(addon).getDependencies();
   }

   public Set<AddonDependency> getMissingDependenciesOf(Addon addon)
   {
      return getState(addon).getMissingDependencies();
   }

   public ClassLoader getClassLoaderOf(Addon addon)
   {
      return getState(addon).getClassLoader();
   }

   public EventManager getEventManagerOf(Addon addon)
   {
      return getState(addon).getEventManager();
   }

   public Future<Void> getFutureOf(Addon addon)
   {
      return getState(addon).getFuture();
   }

   public AddonRepository getRepositoryOf(Addon addon)
   {
      return getState(addon).getRepository();
   }

   public AddonRunnable getRunnableOf(Addon addon)
   {
      return getState(addon).getRunnable();
   }

   public ServiceRegistry getServiceRegistryOf(Addon addon)
   {
      return getState(addon).getServiceRegistry();
   }

   public Set<AddonView> getViewsOf(final Addon addon)
   {
      return lock.performLocked(LockMode.READ, new Callable<Set<AddonView>>()
      {
         @Override
         public Set<AddonView> call() throws Exception
         {
            Set<AddonView> result = new HashSet<AddonView>();
            for (AddonVertex vertex : getCurrentGraph().getGraph().vertexSet())
            {
               if (addon.equals(vertex.getAddon()))
               {
                  result.addAll(vertex.getViews());
                  break;
               }
            }
            return result;
         }
      });
   }

   private AddonState getState(final Addon addon)
   {
      return lock.performLocked(LockMode.READ, new Callable<AddonState>()
      {
         @Override
         public AddonState call() throws Exception
         {
            AddonState result = states.get(addon);
            if (result == null)
               result = new AddonState();
            return result;
         }
      });
   }

   public void setState(final Addon addon, final AddonState state)
   {
      lock.performLocked(LockMode.WRITE, new Callable<Void>()
      {
         @Override
         public Void call() throws Exception
         {
            states.put(addon, state);
            return null;
         }
      });
   }

   public MasterGraph getCurrentGraph()
   {
      return lock.performLocked(LockMode.READ, new Callable<MasterGraph>()
      {
         @Override
         public MasterGraph call() throws Exception
         {
            return graph != null ? graph : new MasterGraph();
         }
      });
   }

   public void setCurrentGraph(final MasterGraph update)
   {
      lock.performLocked(LockMode.WRITE, new Callable<Void>()
      {
         @Override
         public Void call() throws Exception
         {
            graph = update;
            return null;
         }
      });
   }

   public AddonId resolveAddonId(Set<AddonView> views, String name)
   {
      Assert.notNull(views, "Views must not be null.");
      Assert.isTrue(!views.isEmpty(), "Views must not be empty.");
      Assert.notNull(name, "Addon name must not be null.");

      AddonId result = null;

      AddonView view = views.iterator().next();
      for (AddonId id : getAllEnabled(view.getRepositories()))
      {
         if (name.equals(id.getName()) && (result == null || id.getVersion().compareTo(result.getVersion()) >= 0))
            result = id;
      }

      return result;
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

   public boolean cancel(Addon addon)
   {
      boolean result = false;

      try
      {
         try
         {
            AddonRunnable runnable = getRunnableOf(addon);
            if (runnable != null)
            {
               runnable.shutdown();
            }
         }
         finally
         {
            Future<Void> future = getFutureOf(addon);
            if (future != null && !future.isDone())
               result = future.cancel(true);
            if (future.isDone())
               result = true;
         }
      }
      finally
      {
         if (loader != null)
            loader.releaseAddonModule(addon);
         reset(addon);
      }

      return result;
   }

   private void reset(final Addon addon)
   {
      lock.performLocked(LockMode.WRITE, new Callable<Void>()
      {
         @Override
         public Void call() throws Exception
         {
            states.remove(addon);
            return null;
         }
      });
   }

   public boolean canBeStarted(Addon addon)
   {
      return getRunnableOf(addon) == null
               && addon.getStatus().isLoaded();
   }

   public void setHandles(final Addon addon, final Future<Void> result, final AddonRunnable runnable)
   {
      lock.performLocked(LockMode.WRITE, new Callable<Void>()
      {
         @Override
         public Void call() throws Exception
         {
            getState(addon).setFuture(result);
            getState(addon).setRunnable(runnable);
            return null;
         }
      });
   }

   public void setEventManager(final Addon addon, final EventManager manager)
   {
      lock.performLocked(LockMode.WRITE, new Callable<Void>()
      {
         @Override
         public Void call() throws Exception
         {
            getState(addon).setEventManager(manager);
            return null;
         }
      });
   }

   public void setServiceRegistry(final Addon addon, final ServiceRegistry registry)
   {
      lock.performLocked(LockMode.WRITE, new Callable<Void>()
      {
         @Override
         public Void call() throws Exception
         {
            getState(addon).setServiceRegistry(registry);
            return null;
         }
      });
   }
}
