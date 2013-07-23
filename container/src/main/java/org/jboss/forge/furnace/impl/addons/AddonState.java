package org.jboss.forge.furnace.impl.addons;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;

import org.jboss.forge.furnace.addons.AddonDependency;
import org.jboss.forge.furnace.impl.util.NullFuture;
import org.jboss.forge.furnace.repositories.AddonRepository;
import org.jboss.forge.furnace.services.ServiceRegistry;
import org.jboss.forge.furnace.util.Assert;

public class AddonState
{
   private Future<Void> future = new NullFuture<Void>(null);
   private Set<AddonDependency> dependencies = new HashSet<AddonDependency>();
   private Set<AddonDependency> missingDependencies = new HashSet<AddonDependency>();
   private AddonRepository repository;
   private ServiceRegistry registry = new NullServiceRegistry();
   private AddonRunnable runnable;
   private ClassLoader loader;

   public AddonState(Set<AddonDependency> dependencies, AddonRepository repository,
            ClassLoader loader)
   {
      Assert.notNull(dependencies, "Addon dependency set must not be null.");
      Assert.notNull(repository, "Addon repository must not be null.");
      Assert.notNull(loader, "Addon ClassLoader must not be null.");

      this.dependencies = dependencies;
      this.repository = repository;
      this.loader = loader;
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
      return loader;
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
      this.registry = (registry != null ? registry : new NullServiceRegistry());
   }

}
