/**
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.util;

import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.AddonCompatibilityStrategy;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.versions.Versions;

/**
 * The available {@link AddonCompatibilityStrategy} implementations
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public enum AddonCompatibilityStrategies implements AddonCompatibilityStrategy
{
   /**
    * A strict policy allows an addon to be loaded only when the {@link AddonId#getApiVersion()} is compatible with
    * {@link Furnace#getVersion()}
    * 
    * @see Versions#isApiCompatible(org.jboss.forge.furnace.versions.Version, org.jboss.forge.furnace.versions.Version)
    */
   STRICT
   {
      @Override
      public boolean isCompatible(Furnace furnace, AddonId addonId)
      {
         return Versions.isApiCompatible(furnace.getVersion(), addonId.getApiVersion());
      }
   },
   /**
    * A lenient policy allows an addon to be loaded without checking for API compatibility.
    */
   LENIENT
   {
      @Override
      public boolean isCompatible(Furnace furnace, AddonId addonId)
      {
         return true;
      }
   };
}
