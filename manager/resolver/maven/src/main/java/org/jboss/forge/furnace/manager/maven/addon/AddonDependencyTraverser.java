/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.manager.maven.addon;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencyTraverser;
import org.eclipse.aether.graph.Dependency;

/**
 * Used on maven resolution
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 * 
 */
public final class AddonDependencyTraverser implements DependencyTraverser
{
   @Override
   public boolean traverseDependency(Dependency dependency)
   {
      Artifact artifact = dependency.getArtifact();
      boolean isForgeAddon = "forge-addon".equals(artifact.getClassifier());
      // We don't want to traverse non-addons optional dependencies
      if (!isForgeAddon && dependency.isOptional())
      {
         return false;
      }
      boolean shouldRecurse = !"test".equals(dependency.getScope());
      return shouldRecurse;
   }

   @Override
   public DependencyTraverser deriveChildTraverser(DependencyCollectionContext context)
   {
      return this;
   }
}
