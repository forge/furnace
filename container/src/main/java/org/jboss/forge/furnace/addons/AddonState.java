package org.jboss.forge.furnace.addons;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;

import org.jboss.forge.furnace.impl.NullServiceRegistry;
import org.jboss.forge.furnace.repositories.AddonRepository;
import org.jboss.forge.furnace.services.ServiceRegistry;
import org.jboss.forge.furnace.util.Assert;
import org.jboss.forge.furnace.util.NullFuture;
import org.jboss.modules.Module;

public class AddonState
{
   private Future<Void> future = new NullFuture<Void>(null);
   private Set<AddonDependency> dependencies = new HashSet<AddonDependency>();
   private Set<AddonDependency> missingDependencies = new HashSet<AddonDependency>();
   private AddonRepository repository;
   private ServiceRegistry registry = new NullServiceRegistry();
   private Module module;
   private AddonRunnable runnable;

   public AddonState(Set<AddonDependency> dependencies, AddonRepository repository,
            Module module)
   {
      Assert.notNull(dependencies, "Addon dependency set must not be null.");
      Assert.notNull(repository, "Addon repository must not be null.");
      Assert.notNull(module, "Addon Module must not be null.");

      this.dependencies = dependencies;
      this.repository = repository;
      this.module = module;
   }

   public AddonState(Set<AddonDependency> missingDependencies)
   {
      Assert.notNull(missingDependencies, "Missing dependency set must not be null.");
      this.missingDependencies = missingDependencies;
   }

   public AddonState()
   {
   }

   public ClassLoader getClassLoader()
   {
      return module != null ? module.getClassLoader() : null;
   }

   public Module getModule()
   {
      return module;
   }

   public Set<AddonDependency> getDependencies()
   {
      return dependencies;
   }

   public Set<AddonDependency> getMissingDependencies()
   {
      return missingDependencies;
   }

   public Future<Void> getFuture()
   {
      return future;
   }

   public void setFuture(Future<Void> future)
   {
      this.future = future;
   }

   public AddonRepository getRepository()
   {
      return repository;
   }

   public AddonRunnable getRunnable()
   {
      return runnable;
   }

   public void setRunnable(AddonRunnable runnable)
   {
      this.runnable = runnable;
   }

   public ServiceRegistry getServiceRegistry()
   {
      return registry;
   }

   public void setServiceRegistry(ServiceRegistry registry)
   {
      this.registry = registry;
   }

}
