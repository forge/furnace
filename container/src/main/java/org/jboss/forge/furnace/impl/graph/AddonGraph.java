/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.impl.graph;

import org.jboss.forge.furnace.versions.Version;
import org.jgrapht.DirectedGraph;
import org.jgrapht.traverse.DepthFirstIterator;

public abstract class AddonGraph<T extends AddonGraph<T>>
{
   public abstract DirectedGraph<AddonVertex, AddonDependencyEdge> getGraph();

   protected AddonVertex getVertex(String name, Version version)
   {
      AddonVertex result = null;
      for (AddonVertex vertex : getGraph().vertexSet())
      {
         if (vertex.getName().equals(name) && version.compareTo(vertex.getVersion()) == 0)
         {
            result = vertex;
            break;
         }
      }
      return result;
   }

   protected AddonVertex getOrCreateVertex(String name, Version version)
   {
      AddonVertex vertex = getVertex(name, version);
      if (vertex == null)
      {
         vertex = new AddonVertex(name, version);
         addLocalVertex(vertex);
      }
      return vertex;
   }

   protected void addLocalVertex(AddonVertex vertex)
   {
      getGraph().addVertex(vertex);
   }

   @Override
   public String toString()
   {
      DepthFirstIterator<AddonVertex, AddonDependencyEdge> iterator = new DepthFirstIterator<AddonVertex, AddonDependencyEdge>(
               getGraph());

      final StringBuilder builder = new StringBuilder();
      iterator.addTraversalListener(new PrintGraphTraversalListener(getGraph(), builder));

      while (iterator.hasNext())
         iterator.next();

      return builder.toString();
   }

}
