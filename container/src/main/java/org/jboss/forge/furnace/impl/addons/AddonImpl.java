/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.impl.addons;

import java.util.Set;
import java.util.concurrent.Future;

import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonDependency;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.addons.AddonStatus;
import org.jboss.forge.furnace.impl.util.NullFuture;
import org.jboss.forge.furnace.repositories.AddonRepository;
import org.jboss.forge.furnace.spi.ServiceRegistry;
import org.jboss.forge.furnace.util.Assert;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class AddonImpl implements Addon
{
   private final AddonId id;
   private AddonStateManager manager;

   public AddonImpl(AddonStateManager manager, AddonId id)
   {
      Assert.notNull(manager, "Manager must not be null.");
      Assert.notNull(id, "AddonId must not be null.");

      this.id = id;
      this.manager = manager;
   }

   @Override
   public AddonId getId()
   {
      return id;
   }

   @Override
   public Set<AddonDependency> getDependencies()
   {
      return manager.getDependenciesOf(this);
   }

   @Override
   public ClassLoader getClassLoader()
   {
      return manager.getClassLoaderOf(this);
   }

   @Override
   public Future<Void> getFuture()
   {
      return manager.getFutureOf(this);
   }

   @Override
   public AddonRepository getRepository()
   {
      return manager.getRepositoryOf(this);
   }

   @Override
   public ServiceRegistry getServiceRegistry()
   {
      return manager.getServiceRegistryOf(this);
   }

   @Override
   public AddonStatus getStatus()
   {
      AddonStatus result = AddonStatus.MISSING;

      if (getClassLoader() != null)
         result = AddonStatus.LOADED;

      if (getFuture() != null)
      {
         if (!(getFuture() instanceof NullFuture))
         {
            if (getFuture().isDone())
               result = AddonStatus.STARTED;

            if (getFuture().isCancelled())
               result = AddonStatus.FAILED;
         }
      }

      return result;
   }

   @Override
   public String toString()
   {
      StringBuilder builder = new StringBuilder();
      builder.append(getId().toCoordinates() + " +" + getStatus());
      if (getFuture() == null)
         builder.append(" READY");
      builder.append(" HC: ").append(hashCode());
      return builder.toString();
   }

}
