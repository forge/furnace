package org.jboss.forge.furnace.addons;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.jboss.forge.furnace.impl.graph.AddonVertex;
import org.jboss.forge.furnace.impl.graph.MasterGraph;
import org.jboss.forge.furnace.lock.LockManager;
import org.jboss.forge.furnace.lock.LockMode;
import org.jboss.forge.furnace.modules.AddonModuleLoader;
import org.jboss.forge.furnace.repositories.AddonRepository;
import org.jboss.forge.furnace.services.ServiceRegistry;
import org.jboss.forge.furnace.util.Assert;

public class AddonStateManager
{
   private LockManager lock;
   private MasterGraph graph;
   private Map<Addon, AddonState> states = new HashMap<Addon, AddonState>();

   public AddonStateManager(LockManager lock)
   {
      this.lock = lock;
   }

   public void reset(final Addon addon)
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

   public Set<AddonView> getViewsOf(Addon addon)
   {
      Set<AddonView> result = new HashSet<AddonView>();
      AddonVertex vertex = graph.getVertex(addon.getId().getName(), addon.getId().getVersion());
      if (vertex != null)
         result.addAll(vertex.getViews());
      return result;
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
      return graph;
   }

   public void setCurrentGraph(final MasterGraph graph)
   {
      lock.performLocked(LockMode.WRITE, new Callable<Void>()
      {
         @Override
         public Void call() throws Exception
         {
            AddonStateManager.this.setGraph(graph);
            return null;
         }
      });
   }

   private void setGraph(MasterGraph graph)
   {
      this.graph = graph;
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

      AddonState state = getState(addon);

      Future<Void> future = getFutureOf(addon);
      if (future != null && !future.isDone())
         result = future.cancel(true);

      AddonModuleLoader loader = state.getModuleLoader();
      if (loader != null)
         loader.releaseAddonModule(addon);

      setState(addon, null);

      return result;
   }

   public boolean canBeLoaded(Addon addon)
   {
      return getMissingDependenciesOf(addon).isEmpty();
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
