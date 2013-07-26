package org.jboss.forge.furnace.manager.maven.addon;

import org.sonatype.aether.collection.DependencyCollectionContext;
import org.sonatype.aether.collection.DependencySelector;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.Exclusion;
import org.sonatype.aether.util.graph.selector.StaticDependencySelector;

/**
 * A dependency selector that filters based on their scope and classifier "forge-addon"
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
class AddonDependencySelector implements DependencySelector
{
   private static final String FORGE_ADDON = "forge-addon";
   private final int depth;
   private final Dependency parent;
   private final AddonDependencySelector parentSelector;

   public AddonDependencySelector()
   {
      this.depth = 0;
      this.parent = null;
      this.parentSelector = null;
   }

   public AddonDependencySelector(Dependency parent, AddonDependencySelector parentSelector, int depth)
   {
      this.depth = depth;
      this.parent = parent;
      this.parentSelector = parentSelector;
   }

   @Override
   public boolean selectDependency(Dependency dependency)
   {
      boolean result = false;
      if (!isExcluded(dependency))
      {
         boolean optional = dependency.isOptional();

         if (depth < 1)
            return !optional;

         String scope = dependency.getScope();
         String classifier = dependency.getArtifact().getClassifier();

         if ("test".equals(scope))
            return false;

         result = (FORGE_ADDON.equals(classifier) && depth == 1)
                  || (!FORGE_ADDON.equals(classifier) && !"provided".equals(scope) && !optional);
      }
      return result;
   }

   protected boolean isExcluded(Dependency dependency)
   {
      boolean result = isExcludedFromParent(dependency);
      if (!result && parentSelector != null)
      {
         result = parentSelector.isExcluded(dependency);
      }
      return result;
   }

   private boolean isExcludedFromParent(Dependency dependency)
   {
      boolean result = false;
      if (parent != null && parent.getExclusions().size() > 0)
      {
         for (Exclusion exclusion : parent.getExclusions())
         {
            if (exclusion != null)
            {
               if (exclusion.getArtifactId() != null
                        && exclusion.getArtifactId().equals(dependency.getArtifact().getArtifactId()))
               {
                  if (exclusion.getGroupId() != null
                           && exclusion.getGroupId().equals(dependency.getArtifact().getGroupId()))
                  {
                     result = true;
                     break;
                  }
               }
            }
         }
      }
      return result;
   }

   @Override
   public DependencySelector deriveChildSelector(DependencyCollectionContext context)
   {
      if ((depth > 0) && FORGE_ADDON.equals(context.getDependency().getArtifact().getClassifier()))
      {
         return new StaticDependencySelector(false);
      }
      return new AddonDependencySelector(context.getDependency(), this, depth + 1);
   }

   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
      {
         return true;
      }
      else if (null == obj || !getClass().equals(obj.getClass()))
      {
         return false;
      }

      AddonDependencySelector that = (AddonDependencySelector) obj;
      return depth == that.depth;
   }

   @Override
   public int hashCode()
   {
      int hash = 17;
      hash = hash * 31 + depth;
      return hash;
   }

}
