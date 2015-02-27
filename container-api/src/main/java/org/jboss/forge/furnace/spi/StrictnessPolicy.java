/**
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.spi;

import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.AddonId;

/**
 * A {@link StrictnessPolicy} handles if an addon should be loaded based on it's version, for example
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public interface StrictnessPolicy
{
   /**
    * Implementations should check if the given {@link AddonId} is compatible with the given {@link Furnace}
    * 
    * @return true if the addon is compatible, and hence can be loaded by {@link Furnace}
    */
   boolean isCompatible(Furnace furnace, AddonId addonId);
}
