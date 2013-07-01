package org.jboss.forge.furnace.addons;

import java.util.Set;

import org.jboss.forge.furnace.repositories.AddonRepository;

/**
 * A view of a set of {@link Addon} instances.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public interface AddonView
{
   /**
    * Get the registered {@link Addon} for the given {@link AddonId} instance. If no such {@link Addon} is currently
    * registered, register it and return the new reference.
    * 
    * @return the registered {@link Addon} (Never null.)
    */
   Addon getAddon(AddonId id);

   /**
    * Get all currently registered {@link Addon} instances.
    * 
    * @return the {@link Set} of {@link Addon} instances. (Never null.)
    */
   Set<Addon> getAddons();

   /**
    * Get all registered {@link Addon} instances matching the given {@link AddonFilter}.
    * 
    * @return the {@link Set} of {@link Addon} instances. (Never null.)
    */
   Set<Addon> getAddons(AddonFilter filter);

   /**
    * Get the unmodifiable {@link Set} of {@link AddonRepository} instances by which this {@link AddonView} is backed.
    */
   Set<AddonRepository> getRepositories();
}
