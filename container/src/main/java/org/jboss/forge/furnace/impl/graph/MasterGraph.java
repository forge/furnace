package org.jboss.forge.furnace.impl.graph;

import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.event.TraversalListenerAdapter;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.traverse.DepthFirstIterator;

public class MasterGraph extends AddonGraph<MasterGraph>
{
   private DirectedGraph<AddonVertex, AddonDependencyEdge> graph = new SimpleDirectedGraph<AddonVertex, AddonDependencyEdge>(
            AddonDependencyEdge.class);

   @Override
   public DirectedGraph<AddonVertex, AddonDependencyEdge> getGraph()
   {
      return graph;
   }

   public void merge(final OptimizedAddonGraph other)
   {
      DepthFirstIterator<AddonVertex, AddonDependencyEdge> iterator = new DepthFirstIterator<AddonVertex, AddonDependencyEdge>(
               other.getGraph());

      iterator.addTraversalListener(new TraversalListenerAdapter<AddonVertex, AddonDependencyEdge>()
      {
         public void vertexTraversed(org.jgrapht.event.VertexTraversalEvent<AddonVertex> event)
         {
            AddonVertex vertex = event.getVertex();
            mergeVertex(other, vertex);
         };
      });

      while (iterator.hasNext())
         iterator.next();
   }

   private void mergeVertex(final OptimizedAddonGraph other, AddonVertex vertex)
   {
      AddonVertex localVertex = getVertex(vertex.getName(), vertex.getVersion());
      if (localVertex == null)
      {
         addLocalVertex(vertex);
         copySubtree(other, vertex);
      }
      else
      {
         if (isSubtreeEquivalent(localVertex, other, vertex))
         {
            localVertex.addView(other.getAddonView());
         }
         else
         {
            addLocalVertex(vertex);
            copySubtree(other, vertex);
         }
      }
   }

   private void copySubtree(OptimizedAddonGraph other, AddonVertex vertex)
   {
      Set<AddonDependencyEdge> outgoing = other.getGraph().outgoingEdgesOf(vertex);
      for (AddonDependencyEdge edge : outgoing)
      {
         AddonVertex target = other.getGraph().getEdgeTarget(edge);
         mergeVertex(other, target);
         graph.addEdge(vertex, target, new AddonDependencyEdge(edge.getVersionRange(), edge.isExported()));
      }

   }

   private boolean isSubtreeEquivalent(AddonVertex localVertex,
            OptimizedAddonGraph other, AddonVertex otherVertex)
   {
      Set<AddonDependencyEdge> otherOutgoing = other.getGraph().outgoingEdgesOf(otherVertex);

      for (AddonDependencyEdge otherEdge : otherOutgoing)
      {
         AddonVertex otherTarget = graph.getEdgeTarget(otherEdge);
         AddonVertex localTarget = getVertex(otherTarget.getName(), otherTarget.getVersion());

         if (localTarget == null)
         {
            return false;
         }

         AddonDependencyEdge localEdge = graph.getEdge(localVertex, localTarget);
         if (localEdge == null)
         {
            return false;
         }

         if (!isSubtreeEquivalent(localTarget, other, otherTarget))
         {
            return false;
         }
      }
      return true;
   }

   @Override
   protected void enhanceNewVertex(AddonVertex vertex)
   {
      // Intentionally blank.
   }

}
