/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.impl.addons;

import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonDependency;
import org.jboss.forge.furnace.lock.LockManager;
import org.jboss.forge.furnace.util.Assert;

/**
 * An edge in the registered {@link Addon} graph.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class AddonDependencyImpl implements AddonDependency
{
   private boolean exported = false;
   private boolean optional = false;
   private Addon dependency;

   @SuppressWarnings("unused")
   private LockManager lockManager;

   public AddonDependencyImpl(LockManager lockManager, Addon dependency, boolean exported, boolean optional)
   {
      Assert.notNull(lockManager, "LockManager must not be null.");
      Assert.notNull(dependency, "Dependency Addon not be null.");

      this.lockManager = lockManager;
      this.dependency = dependency;
      this.exported = exported;
      this.optional = optional;
   }

   @Override
   public Addon getDependency()
   {
      return dependency;
   }

   @Override
   public boolean isExported()
   {
      return exported;
   }

   @Override
   public boolean isOptional()
   {
      return optional;
   }

   @Override
   public String toString()
   {
      return dependency.getId().toCoordinates();
   }

}