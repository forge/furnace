package org.jboss.forge.furnace.impl.graph;

import org.jboss.forge.furnace.util.Assert;
import org.jboss.forge.furnace.versions.VersionRange;
import org.jgrapht.graph.DefaultEdge;

public class AddonDependencyEdge extends DefaultEdge
{
   private static final long serialVersionUID = 4801017416034161293L;
   private boolean exported;
   private VersionRange versionRange;

   public AddonDependencyEdge(VersionRange range, boolean exported)
   {
      Assert.notNull(range, "Version range must not be null.");
      this.versionRange = range;
      this.exported = exported;
   }

   public boolean isExported()
   {
      return exported;
   }

   public VersionRange getVersionRange()
   {
      return versionRange;
   }

   @Override
   public String toString()
   {
      return "[exported=" + exported + ", versionRange=" + versionRange + "]";
   }

}
