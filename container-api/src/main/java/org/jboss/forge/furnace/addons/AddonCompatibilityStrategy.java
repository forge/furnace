/**
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.addons;

import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.versions.Version;

/**
 * A {@link AddonCompatibilityStrategy} handles if an {@link Addon} should be loaded based on it's {@link Version}, for
 * example.
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public interface AddonCompatibilityStrategy
{
   /**
    * Implementations should check if the given {@link AddonId} is compatible with the given {@link Furnace} instance
    * 
    * @return <code>true</code> if the {@link AddonId} is compatible with the given {@link Furnace} instance
    */
   boolean isCompatible(Furnace furnace, AddonId addonId);
}
