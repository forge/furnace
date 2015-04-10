/**
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.manager.impl;

import java.io.File;
import java.util.Collections;
import java.util.Set;

import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.manager.spi.AddonInfo;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;

/**
 * A shallow {@link AddonInfo} implementation used when only the {@link AddonId} matters
 * 
 * @author <a href="ggastald@redhat.com">George Gastaldi</a>
 */
class ShallowAddonInfo implements AddonInfo
{
   private final AddonId addonId;

   public ShallowAddonInfo(AddonId addonId)
   {
      this.addonId = addonId;
   }

   @Override
   public AddonId getAddon()
   {
      return addonId;
   }

   @Override
   public Set<AddonId> getRequiredAddons()
   {
      return Collections.emptySet();
   }

   @Override
   public Set<AddonId> getOptionalAddons()
   {
      return Collections.emptySet();
   }

   @Override
   public Set<File> getResources()
   {
      return Collections.emptySet();
   }

   @Override
   public Set<AddonDependencyEntry> getDependencyEntries()
   {
      return Collections.emptySet();
   }
}