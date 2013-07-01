package org.jboss.forge.furnace.impl;

import java.util.Collection;

import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonFilter;
import org.jboss.forge.furnace.repositories.AddonRepository;

/**
 * An {@link AddonFilter} that filters on the origin repository of the given addon.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public final class AddonRepositoryFilter implements AddonFilter
{
   private Collection<AddonRepository> repositories;

   public AddonRepositoryFilter(Collection<AddonRepository> repositories)
   {
      this.repositories = repositories;
   }

   @Override
   public boolean accept(Addon addon)
   {
      for (AddonRepository repository : repositories)
      {
         if (repository.equals(addon.getRepository()))
         {
            return true;
         }
      }
      return false;
   }
}