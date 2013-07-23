package org.jboss.forge.furnace.impl.addons;

import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonFilter;
import org.jboss.forge.furnace.addons.AddonView;

/**
 * An {@link AddonFilter} that filters on the {@link AddonView} containers of the given {@link Addon}.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public final class AddonViewFilter implements AddonFilter
{
   private AddonStateManager manager;
   private AddonView view;

   public AddonViewFilter(AddonStateManager manager, AddonView view)
   {
      this.manager = manager;
      this.view = view;
   }

   @Override
   public boolean accept(Addon addon)
   {
      if (manager.getViewsOf(addon).contains(view))
      {
         return true;
      }
      return false;
   }
}