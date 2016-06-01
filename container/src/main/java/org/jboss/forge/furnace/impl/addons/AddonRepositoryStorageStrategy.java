/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.impl.addons;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;

/**
 * Implementations should provide a strategy for handling {@link Addon} storage of addons
 * 
 * @author <a href="bsideup@gmail.com">Sergei Egorov</a>
 * @see AddonRepositoryImpl
 */
public interface AddonRepositoryStorageStrategy
{
   /**
    * Get the base installation directory of the given {@link AddonId}.
    */
   public File getAddonBaseDir(AddonId addon);

   /**
    * Get the runtime {@link Addon} dependencies of the given {@link AddonId}.
    */
   public Set<AddonDependencyEntry> getAddonDependencies(AddonId addon);

   /**
    * Get the addon descriptor file for the given {@link AddonId}.
    */
   public File getAddonDescriptor(AddonId addon);

   /**
    * Get a list of all local resources for the given {@link AddonId}.
    */
   public List<File> getAddonResources(AddonId addon);

   /**
    * Returns <code>true</code> if the given {@link AddonId} is deployed in this {@link AddonRepositoryStorageStrategy};
    * otherwise, returns <code>false</code>.
    */
   public boolean isDeployed(AddonId addon);

}
