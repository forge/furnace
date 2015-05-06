/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.impl.addons;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Future;

import org.jboss.forge.furnace.addons.AddonDependency;
import org.jboss.forge.furnace.event.EventManager;
import org.jboss.forge.furnace.impl.event.NullEventManager;
import org.jboss.forge.furnace.impl.util.NullFuture;
import org.jboss.forge.furnace.repositories.AddonRepository;
import org.jboss.forge.furnace.spi.ServiceRegistry;
import org.jboss.forge.furnace.util.Assert;

public class AddonState
{
   private Future<Void> future = new NullFuture<>(null);
   private Set<AddonDependency> dependencies = new LinkedHashSet<>();
   private Set<AddonDependency> missingDependencies = new LinkedHashSet<>();
   private AddonRepository repository;
   private ServiceRegistry registry = NullServiceRegistry.INSTANCE;
   private EventManager eventManager = NullEventManager.INSTANCE;
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

   public EventManager getEventManager()
   {
      return eventManager;
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
      this.registry = (registry != null ? registry : NullServiceRegistry.INSTANCE);
   }

   public void setEventManager(EventManager manager)
   {
      this.eventManager = (manager != null ? manager : NullEventManager.INSTANCE);
   }

}
