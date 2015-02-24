package org.jboss.forge.arquillian.archive;

import org.jboss.forge.furnace.repositories.AddonRepository;

/**
 * Designates an object as relating to a specific {@link AddonRepository} location.
 * 
 * @author <a href="lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public interface RepositoryLocationAware<ORIGIN>
{
   /**
    * Gets the {@link AddonRepository} path to which this object is related. (Relative to the Java temp directory.)
    */
   String getAddonRepository();

   /**
    * Sets the {@link AddonRepository} path to which this object is related. (Relative to the Java temp directory.)
    */
   ORIGIN setAddonRepository(String repository);
}
