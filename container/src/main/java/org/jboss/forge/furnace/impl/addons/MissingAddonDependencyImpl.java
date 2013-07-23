package org.jboss.forge.furnace.impl.addons;

import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonDependency;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;

public class MissingAddonDependencyImpl implements AddonDependency
{

   private AddonDependencyEntry entry;

   public MissingAddonDependencyImpl(AddonDependencyEntry entry)
   {
      this.entry = entry;
   }

   @Override
   public Addon getDependency()
   {
      return null;
   }

   @Override
   public boolean isExported()
   {
      return entry.isExported();
   }

   @Override
   public boolean isOptional()
   {
      return entry.isOptional();
   }

   public String getMissingAddonName()
   {
      return entry.getName();
   }

   @Override
   public String toString()
   {
      return "Missing: " + entry + "";
   }

}
