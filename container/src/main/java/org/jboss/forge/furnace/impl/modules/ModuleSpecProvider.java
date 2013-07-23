package org.jboss.forge.furnace.impl.modules;

import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleSpec;

public interface ModuleSpecProvider
{
   ModuleSpec get(ModuleLoader loader, ModuleIdentifier id);
}
