package org.jboss.forge.arquillian.archive;

import java.util.Set;

import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.shrinkwrap.api.Archive;

/**
 * Archive representing a Furnace AddonDependency deployment.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public interface ForgeRemoteAddon extends Archive<ForgeRemoteAddon>
{
   Set<AddonId> getAddonIds();

   ForgeRemoteAddon setAddonIds(Set<AddonId> ids);

   String getAddonRepository();

   ForgeRemoteAddon setAddonRepository(String repository);
}
