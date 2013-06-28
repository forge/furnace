package org.jboss.forge.furnace.impl.graph;

import java.util.Iterator;

import org.jgrapht.DirectedGraph;
import org.jgrapht.event.TraversalListenerAdapter;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.traverse.DepthFirstIterator;

public class PrintGraphTraversalListener extends TraversalListenerAdapter<AddonVertex, AddonDependencyEdge>
{
   private static final String MISSING = "MISSING";
   int count = 0;
   private final StringBuilder builder;
   private DirectedGraph<AddonVertex, AddonDependencyEdge> graph;
   private int depth;
   private boolean parentComplete;

   public PrintGraphTraversalListener(DirectedGraph<AddonVertex, AddonDependencyEdge> graph, StringBuilder builder)
   {
      this(graph, builder, 0, false);
   }

   public PrintGraphTraversalListener(DirectedGraph<AddonVertex, AddonDependencyEdge> graph, StringBuilder builder,
            int depth, boolean parentComplete)
   {
      this.graph = graph;
      this.builder = builder;
      this.depth = depth;
      this.parentComplete = parentComplete;
   }

   @Override
   public void vertexTraversed(VertexTraversalEvent<AddonVertex> e)
   {
      AddonVertex vertex = e.getVertex();

      Iterator<AddonDependencyEdge> dependencyIterator = graph.outgoingEdgesOf(e.getVertex()).iterator();
      if (dependencyIterator.hasNext() || depth == 0)
      {
         if (depth == 0)
         {
            builder.append("\n");
            builder.append(count++).append(": ").append(vertex.getName()).append(":")
                     .append(vertex.getVersion() == null ? MISSING : vertex.getVersion())
                     .append("\n");
         }

         while (dependencyIterator.hasNext())
         {
            AddonDependencyEdge edge = dependencyIterator.next();
            AddonVertex dependency = graph.getEdgeTarget(edge);

            indent();
            if (dependencyIterator.hasNext())
               builder.append("  +- ");
            else
               builder.append("  \\- ");

            builder.append(dependency.getName()).append(":").append(edge.getVersionRange()).append(" -> ");
            builder.append(dependency.getVersion() == null ? MISSING : dependency.getVersion());

            if (edge.isExported())
            {
               builder.append(" (E) ");
            }
            builder.append("\n");

            DepthFirstIterator<AddonVertex, AddonDependencyEdge> iterator = new DepthFirstIterator<AddonVertex, AddonDependencyEdge>(
                     graph, dependency);

            iterator.addTraversalListener(new PrintGraphTraversalListener(graph, builder, depth + 1,
                     dependencyIterator.hasNext()));

            while (iterator.hasNext())
               iterator.next();
         }
      }
   }

   private void indent()
   {
      for (int i = 0; i < depth; i++)
      {
         builder.append("  ");
         if (i < depth && parentComplete)
            builder.append("|");
         else
            builder.append(" ");
      }
   }
}