package org.jboss.forge.furnace.impl.graph;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jboss.forge.furnace.repositories.AddonRepository;
import org.jboss.forge.furnace.versions.EmptyVersion;
import org.jboss.forge.furnace.versions.EmptyVersionRange;
import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.event.TraversalListenerAdapter;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.traverse.DepthFirstIterator;

public class OptimizedAddonGraph extends AddonGraph<OptimizedAddonGraph>
{
   DirectedGraph<AddonVertex, AddonDependencyEdge> graph = new SimpleDirectedGraph<AddonVertex, AddonDependencyEdge>(
            AddonDependencyEdge.class);

   public OptimizedAddonGraph(List<AddonRepository> repositories,
            final DirectedGraph<AddonVertex, AddonDependencyEdge> completeGraph)
   {
      DepthFirstIterator<AddonVertex, AddonDependencyEdge> iterator = new DepthFirstIterator<AddonVertex, AddonDependencyEdge>(
               completeGraph);
      iterator.addTraversalListener(new TraversalListenerAdapter<AddonVertex, AddonDependencyEdge>()
      {
         public void vertexTraversed(org.jgrapht.event.VertexTraversalEvent<AddonVertex> event)
         {
            AddonVertex vertex = event.getVertex();
            Set<AddonDependencyEdge> incoming = completeGraph.incomingEdgesOf(vertex);

            AddonVertex localVertex = getOrCreateVertex(vertex.getName(), EmptyVersion.getInstance());
            for (AddonDependencyEdge incomingEdge : incoming)
            {
               AddonVertex source = completeGraph.getEdgeSource(incomingEdge);
               AddonVertex localSource = getOrCreateVertex(source.getName(), EmptyVersion.getInstance());

               graph.addEdge(localSource, localVertex,
                        new AddonDependencyEdge(new EmptyVersionRange(), incomingEdge.isExported()));
            }
         };
      });

      while (iterator.hasNext())
         iterator.next();

      Map<AddonVertex, AddonVertex> replacements = new LinkedHashMap<AddonVertex, AddonVertex>();
      for (AddonVertex localVertex : graph.vertexSet())
      {
         for (AddonVertex vertex : completeGraph.vertexSet())
         {
            if (localVertex.getName().equals(vertex.getName()) && localVertex.getVersion().compareTo(
                     vertex.getVersion()) < 1)
            {
               if (replacements.get(localVertex) == null || replacements.get(localVertex).getVersion().compareTo(
                        vertex.getVersion()) < 1)
               {
                  replacements.put(localVertex, new AddonVertex(localVertex.getName(), vertex.getVersion()));
               }
            }
         }
      }

      for (Entry<AddonVertex, AddonVertex> entry : replacements.entrySet())
      {
         replaceVertex(entry.getKey(), entry.getValue());
      }

      CycleDetector<AddonVertex, AddonDependencyEdge> detector = new CycleDetector<AddonVertex, AddonDependencyEdge>(
               graph);
      if (detector.detectCycles())
      {
         throw new IllegalStateException("Cycle detected in Addon graph: " + detector.findCycles());
      }
   }

   private void replaceVertex(AddonVertex original, AddonVertex replacement)
   {
      Set<AddonDependencyEdge> incoming = graph.incomingEdgesOf(original);
      Set<AddonDependencyEdge> outgoing = graph.outgoingEdgesOf(original);

      graph.addVertex(replacement);

      for (AddonDependencyEdge edge : incoming)
      {
         graph.addEdge(graph.getEdgeSource(edge), replacement,
                  new AddonDependencyEdge(edge.getVersionRange(), edge.isExported()));
      }

      for (AddonDependencyEdge edge : outgoing)
      {
         graph.addEdge(replacement, graph.getEdgeTarget(edge),
                  new AddonDependencyEdge(edge.getVersionRange(), edge.isExported()));
      }

      graph.removeVertex(original);
   }

   @Override
   public DirectedGraph<AddonVertex, AddonDependencyEdge> getGraph()
   {
      return graph;
   }

}
