/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.impl.addons;

import java.util.List;

import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.versions.Version;

/**
 * Implementations should handle the state of the installed and deployed add-ons
 * 
 * @author <a href="bsideup@gmail.com">Sergei Egorov</a>
 * @see AddonRepositoryImpl
 */
public interface AddonRepositoryStateStrategy
{
   /**
    * Returns <code>true</code> if the given {@link AddonId} is enabled in this {@link AddonRepositoryStateStrategy};
    * otherwise, returns <code>false</code>.
    */
   public boolean isEnabled(final AddonId addon);

   /**
    * Returns a {@link List} of {@link AddonId} instances for all {@link Addon}s in this repository.
    */
   List<AddonId> listAll();

   /**
    * Returns a {@link List} of {@link AddonId} instances for all enabled {@link Addon}s in this repository.
    */
   public List<AddonId> listEnabled();

   /**
    * Returns a {@link List} of {@link AddonId} instances for all enabled {@link Addon}s in this repository that are API
    * compatible with the given {@link Version}.
    */
   public List<AddonId> listEnabledCompatibleWithVersion(final Version version);

   /**
    * Returns the runtime change version of this {@link AddonRepositoryStateStrategy}.
    */
   public int getVersion();
}
