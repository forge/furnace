package org.jboss.forge.arquillian.archive;

import org.jboss.forge.furnace.Furnace;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.container.LibraryContainer;
import org.jboss.shrinkwrap.api.container.ResourceContainer;
import org.jboss.shrinkwrap.api.container.ServiceProviderContainer;

/**
 * Intermediate interface for types compatible with the {@link Furnace} test harness.
 * 
 * @author <a href="lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public interface AddonArchiveBase<ORIGIN extends Archive<ORIGIN>> extends Archive<ORIGIN>, LibraryContainer<ORIGIN>,
         ResourceContainer<ORIGIN>, ServiceProviderContainer<ORIGIN>,
         RepositoryLocationAware<ORIGIN>, AddonDependencyAware<ORIGIN>
{
}
