/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.impl.modules;

import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleSpec;

/**
 * A {@link ModuleSpecProvider} is used by {@link AddonModuleLoader} to provide {@link ModuleSpec} instances based on a
 * given {@link ModuleIdentifier}
 *
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public interface ModuleSpecProvider
{
   /**
    * Returns a {@link ModuleSpec} instance given a {@link ModuleIdentifier}
    * 
    * @param loader the {@link ModuleLoader} used to load the returned {@link ModuleSpec}
    * @param id the {@link ModuleIdentifier} of the loaded {@link ModuleSpec}
    * @return the {@link ModuleSpec} associated with this {@link ModuleIdentifier}, <code>null</code> if the
    *         {@link ModuleIdentifier} does not apply to this {@link ModuleSpecProvider}
    */
   ModuleSpec get(ModuleLoader loader, ModuleIdentifier id);
}
