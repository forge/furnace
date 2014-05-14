/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.manager.maven.addon;

import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencyTraverser;
import org.eclipse.aether.graph.Dependency;

/**
 * Used on maven resolution
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public final class AddonDependencyTraverser implements DependencyTraverser
{
   @SuppressWarnings("unused")
   private final String classifier;

   public AddonDependencyTraverser(String classifier)
   {
      this.classifier = classifier;
   }

   @Override
   public boolean traverseDependency(Dependency dependency)
   {
      return !"test".equals(dependency.getScope());
   }

   @Override
   public DependencyTraverser deriveChildTraverser(DependencyCollectionContext context)
   {
      return this;
   }
}
