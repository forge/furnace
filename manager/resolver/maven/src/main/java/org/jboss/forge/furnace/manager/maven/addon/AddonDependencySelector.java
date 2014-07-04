/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.manager.maven.addon;

import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.util.graph.selector.StaticDependencySelector;

/**
 * A dependency selector that filters based on their scope and classifier "forge-addon"
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
class AddonDependencySelector implements DependencySelector
{
   private final String classifier;
   private final int depth;
   private final Dependency parent;
   private final AddonDependencySelector parentSelector;

   public AddonDependencySelector(String classifier)
   {
      this.classifier = classifier;
      this.depth = 0;
      this.parent = null;
      this.parentSelector = null;
   }

   private AddonDependencySelector(AddonDependencySelector parentSelector, Dependency parent, String classifier,
            int depth)
   {
      this.classifier = classifier;
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
         boolean module = this.classifier.equals(dependency.getArtifact().getClassifier());

         String scope = dependency.getScope();

         if (dependency.isOptional() && depth > 1)
            result = false;
         else if ("test".equals(scope))
            result = false;
         else
            result = (module && depth == 1) || (!module && !"provided".equals(scope));
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
      boolean module = this.classifier.equals(context.getDependency().getArtifact().getClassifier());
      if ((depth > 0) && module)
      {
         return new StaticDependencySelector(false);
      }
      return new AddonDependencySelector(this, context.getDependency(), this.classifier, depth + 1);
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
