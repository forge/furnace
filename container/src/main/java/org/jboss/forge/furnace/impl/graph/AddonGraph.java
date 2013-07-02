package org.jboss.forge.furnace.impl.graph;

import org.jboss.forge.furnace.versions.Version;
import org.jgrapht.DirectedGraph;
import org.jgrapht.traverse.DepthFirstIterator;

public abstract class AddonGraph<T extends AddonGraph<T>>
{
   public abstract DirectedGraph<AddonVertex, AddonDependencyEdge> getGraph();

   protected AddonVertex getVertex(String name, Version version)
   {
      AddonVertex temp = new AddonVertex(name, version);

      AddonVertex result = null;
      for (AddonVertex vertex : getGraph().vertexSet())
      {
         if (temp.equals(vertex))
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
      enhanceNewVertex(vertex);
   }

   protected abstract void enhanceNewVertex(AddonVertex vertex);

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
