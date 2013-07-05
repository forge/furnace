package org.jboss.forge.furnace.impl.graph;

import java.util.Set;

import org.jboss.forge.furnace.versions.Version;
import org.jgrapht.DirectedGraph;
import org.jgrapht.Graphs;
import org.jgrapht.event.TraversalListenerAdapter;
import org.jgrapht.event.VertexTraversalEvent;
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
      if (other.getGraph().vertexSet().isEmpty())
         return;

      if (graph.vertexSet().isEmpty())
      {
         if (!Graphs.addGraph(graph, other.getGraph()))
            throw new IllegalStateException("Error while copying graphs.");
      }
      else
      {
         DepthFirstIterator<AddonVertex, AddonDependencyEdge> iterator = new DepthFirstIterator<AddonVertex, AddonDependencyEdge>(
                  other.getGraph());

         iterator.addTraversalListener(new TraversalListenerAdapter<AddonVertex, AddonDependencyEdge>()
         {
            @Override
            public void vertexTraversed(VertexTraversalEvent<AddonVertex> event)
            {
               mergeVertex(other, event.getVertex());
            };
         });

         while (iterator.hasNext())
            iterator.next();
      }
   }

   private AddonVertex mergeVertex(final OptimizedAddonGraph other, AddonVertex vertex)
   {
      AddonVertex localVertex = getVertex(vertex.getName(), vertex.getVersion());
      if (localVertex == null)
      {
         addLocalVertex(vertex);
         localVertex = vertex;
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
      return localVertex;
   }

   private void copySubtree(OptimizedAddonGraph other, AddonVertex vertex)
   {
      Set<AddonDependencyEdge> outgoing = other.getGraph().outgoingEdgesOf(vertex);
      for (AddonDependencyEdge edge : outgoing)
      {
         AddonVertex target = other.getGraph().getEdgeTarget(edge);
         AddonVertex localTarget = mergeVertex(other, target);
         graph.addEdge(vertex, localTarget, new AddonDependencyEdge(edge.getVersionRange(), edge.isExported()));
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

   @Override
   public AddonVertex getVertex(String name, Version version)
   {
      return super.getVertex(name, version);
   }

}
