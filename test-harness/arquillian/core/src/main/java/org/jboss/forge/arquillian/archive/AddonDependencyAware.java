package org.jboss.forge.arquillian.archive;

import java.util.List;

import org.jboss.forge.furnace.addons.AddonDependency;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;

/**
 * Designates an object that can have an {@link AddonDependency} entries.
 * 
 * @author <a href="lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public interface AddonDependencyAware<ORIGIN>
{
   /**
    * Adds the given {@link AddonDependency} instances as addon module dependencies for this object.
    */
   ORIGIN addAsAddonDependencies(AddonDependencyEntry... dependencies);

   /**
    * Get the currently specified {@link AddonDependency} instances for this object.
    */
   List<AddonDependencyEntry> getAddonDependencies();
}
