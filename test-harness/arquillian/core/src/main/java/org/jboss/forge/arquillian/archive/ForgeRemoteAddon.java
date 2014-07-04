/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.arquillian.archive;

import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.shrinkwrap.api.Archive;

/**
 * Archive representing a Furnace AddonDependency deployment.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public interface ForgeRemoteAddon extends Archive<ForgeRemoteAddon>
{
   AddonId getAddonId();

   ForgeRemoteAddon setAddonId(AddonId id);

   String getAddonRepository();

   ForgeRemoteAddon setAddonRepository(String repository);
}
