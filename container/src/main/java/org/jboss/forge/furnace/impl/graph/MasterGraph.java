package org.jboss.forge.furnace.impl.graph;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.jboss.forge.furnace.addons.AddonView;
import org.jboss.forge.furnace.util.Streams;
import org.jboss.forge.furnace.versions.SingleVersion;
import org.jboss.forge.furnace.versions.Version;
import org.jgrapht.DirectedGraph;
import org.jgrapht.event.TraversalListenerAdapter;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.IntegerNameProvider;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.traverse.DepthFirstIterator;

public class MasterGraph
{
   private DirectedGraph<AddonVertex, AddonDependencyEdge> graph = new SimpleDirectedGraph<AddonVertex, AddonDependencyEdge>(
            AddonDependencyEdge.class);

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
         for (AddonVertex vertex : other.getGraph().vertexSet())
         {
            mergeVertex(other, vertex);
         }
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

   private AddonVertex mergeVertex(final OptimizedAddonGraph other, final AddonVertex otherVertex)
   {
      AddonVertex result = null;
      Set<AddonVertex> localVertices = getVertices(otherVertex.getName(), otherVertex.getVersion());
      AddonView view = other.getAddonView();
      if (localVertices.isEmpty())
      {
         result = new AddonVertex(otherVertex, view);
         graph.addVertex(result);
         copySubtree(result, other, otherVertex);
      }
      else
      {
         boolean exists = false;
         for (AddonVertex localVertex : localVertices)
         {
            if (isSubtreeEquivalent(localVertex, other.getGraph(), otherVertex))
            {
               exists = true;
               if (!localVertex.getViews().contains(view))
               {
                  result = new AddonVertex(localVertex, view);
                  replaceVertex(localVertex, result);
               }
               else
               {
                  result = localVertex;
               }
            }
         }

         if (!exists)
         {
            result = new AddonVertex(otherVertex, view);
            graph.addVertex(result);
            copySubtree(result, other, otherVertex);
         }
      }
      return result;
   }

   public Set<AddonVertex> getVertices(String name, Version version)
   {
      Set<AddonVertex> result = new HashSet<AddonVertex>();
      for (AddonVertex vertex : getGraph().vertexSet())
      {
         String vertexName = vertex.getName();
         // FIXME some weird CLAC javassist issue requiring this unwrapping?
         Version vertexVersion = new SingleVersion(vertex.getVersion().toString());
         if (vertexName.equals(name)
                  && new SingleVersion(version.toString())
                           .compareTo(new SingleVersion(vertexVersion.toString())) == 0)
         {
            result.add(vertex);
         }
      }
      return result;
   }

   public boolean isSubtreeEquivalent(AddonVertex localVertex,
            DirectedGraph<AddonVertex, AddonDependencyEdge> otherGraph, AddonVertex otherVertex)
   {
      Set<AddonDependencyEdge> otherOutgoing = otherGraph.outgoingEdgesOf(otherVertex);
      Set<AddonDependencyEdge> localOutgoing = graph.outgoingEdgesOf(localVertex);

      if (otherOutgoing.size() == localOutgoing.size())
      {
         for (AddonDependencyEdge otherEdge : otherOutgoing)
         {
            AddonVertex otherTarget = graph.getEdgeTarget(otherEdge);
            Set<AddonVertex> localCandidates = getVertices(otherTarget.getName(), otherTarget.getVersion());

            boolean found = false;
            if (!localCandidates.isEmpty())
            {
               for (AddonVertex candidate : localCandidates)
               {
                  AddonDependencyEdge localEdge = graph.getEdge(localVertex, candidate);
                  if (localEdge != null && isSubtreeEquivalent(candidate, otherGraph, otherTarget))
                  {
                     found = true;
                     break;
                  }
               }
            }

            if (!found)
               return false;
         }
         return true;
      }
      return false;
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

   private void copySubtree(AddonVertex localVertex, OptimizedAddonGraph other, AddonVertex vertex)
   {
      Set<AddonDependencyEdge> outgoing = other.getGraph().outgoingEdgesOf(vertex);
      for (AddonDependencyEdge edge : outgoing)
      {
         AddonVertex target = other.getGraph().getEdgeTarget(edge);
         AddonVertex localTarget = mergeVertex(other, target);
         graph.addEdge(localVertex, localTarget, new AddonDependencyEdge(edge.getVersionRange(), edge.isExported()));
      }
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

   public void toDOT(File file)
   {
      FileWriter fw = null;
      try
      {
         DOTExporter<AddonVertex, AddonDependencyEdge> exporter = new DOTExporter<AddonVertex, AddonDependencyEdge>(
                  new IntegerNameProvider<AddonVertex>(),
                  new AddonVertexNameProvider(),
                  new AddonDependencyEdgeNameProvider());

         fw = new FileWriter(file);
         exporter.export(fw, graph);
         fw.flush();
      }
      catch (IOException e)
      {
         e.printStackTrace();
      }
      finally
      {
         Streams.closeQuietly(fw);
      }
   }

}
