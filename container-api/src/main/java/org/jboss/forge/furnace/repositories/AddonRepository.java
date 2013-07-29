/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.repositories;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.versions.Version;

/**
 * Used to perform {@link Addon} installation/registration operations.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * @author <a href="mailto:koen.aers@gmail.com">Koen Aers</a>
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public interface AddonRepository
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
    * Get the root directory of this {@link AddonRepository}.
    */
   public File getRootDirectory();

   /**
    * Returns <code>true</code> if the given {@link AddonId} is deployed in this {@link AddonRepository}; otherwise,
    * returns <code>false</code>.
    */
   public boolean isDeployed(AddonId addon);

   /**
    * Returns <code>true</code> if the given {@link AddonId} is enabled in this {@link AddonRepository}; otherwise,
    * returns <code>false</code>.
    */
   public boolean isEnabled(final AddonId addon);

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
    * Returns the last modified date of this {@link AddonRepository}.
    */
   public Date getLastModified();

   /**
    * Returns the runtime change version of this {@link AddonRepository}.
    */
   public int getVersion();
}
