package org.jboss.forge.furnace.addons;

import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonDependency;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.forge.furnace.versions.Version;

public class MissingAddonDependencyImpl implements AddonDependency
{

   private AddonDependencyEntry entry;

   public MissingAddonDependencyImpl(AddonDependencyEntry entry)
   {
      this.entry = entry;
   }

   @Override
   public Addon getDependent()
   {
      return null;
   }

   @Override
   public Addon getDependency()
   {
      return null;
   }

   @Override
   public Version getDependencyVersion()
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
      return "MissingAddonDependencyImpl [entry=" + entry + "]";
   }

}
