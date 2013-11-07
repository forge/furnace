package org.jboss.forge.furnace.impl.graph;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.addons.AddonView;
import org.jboss.forge.furnace.util.Assert;
import org.jboss.forge.furnace.versions.Version;

public class AddonVertex
{
   /*
    * Key fields must be immutable in order for JGraphT to function properly
    */
   private final String name;
   private final Version version;
   private final Set<AddonView> views = new HashSet<AddonView>();

   /*
    * Mutable fields
    */
   private Addon addon;
   private boolean dirty;

   public AddonVertex(String name, Version version)
   {
      Assert.notNull(name, "Name must not be null.");
      Assert.notNull(version, "Version must not be null.");
      this.name = name;
      this.version = version;
   }

   public AddonVertex(AddonVertex source, AddonView view)
   {
      this.name = source.name;
      this.version = source.version;
      this.views.addAll(source.getViews());
      this.views.add(view);
      this.addon = source.addon;
      this.dirty = source.dirty;
   }

   public Addon getAddon()
   {
      return addon;
   }

   public void setAddon(Addon addon)
   {
      this.addon = addon;
   }

   public String getName()
   {
      return name;
   }

   public Version getVersion()
   {
      return version;
   }

   public void setDirty(boolean dirty)
   {
      this.dirty = dirty;
   }

   public boolean isDirty()
   {
      return dirty;
   }

   public Set<AddonView> getViews()
   {
      return Collections.unmodifiableSet(views);
   }

   public AddonId getAddonId()
   {
      return AddonId.from(name, getVersion());
   }

   @Override
   public int hashCode()
   {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + ((version == null) ? 0 : version.hashCode());
      result = prime * result + ((views == null) ? 0 : views.hashCode());
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
      if (views == null)
      {
         if (other.views != null)
            return false;
      }
      else if (!views.equals(other.views))
         return false;
      return true;
   }

   @Override
   public String toString()
   {
      return "[" + name + "," + version + "]";
   }
}
