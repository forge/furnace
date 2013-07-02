package org.jboss.forge.furnace.impl.graph;

import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.util.Assert;
import org.jboss.forge.furnace.versions.Version;

public class AddonVertex
{
   private String name;
   private Version version;

   public AddonVertex(String name, Version version)
   {
      Assert.notNull(name, "Name must not be null.");
      Assert.notNull(version, "Version must not be null.");
      this.name = name;
      this.version = version;
   }

   public String getName()
   {
      return name;
   }

   public Version getVersion()
   {
      return version;
   }

   public void overrideVersion(Version version)
   {
      this.version = version;
   }

   @Override
   public int hashCode()
   {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + ((version == null) ? 0 : version.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      AddonVertex other = (AddonVertex) obj;
      if (name == null)
      {
         if (other.name != null)
            return false;
      }
      else if (!name.equals(other.name))
         return false;
      if (version == null)
      {
         if (other.version != null)
            return false;
      }
      else if (!version.equals(other.version))
         return false;
      return true;
   }

   @Override
   public String toString()
   {
      return "[" + name + "," + version + "]";
   }

   public AddonId getAddonId()
   {
      return AddonId.from(getName(), getVersion().toString());
   }
}
