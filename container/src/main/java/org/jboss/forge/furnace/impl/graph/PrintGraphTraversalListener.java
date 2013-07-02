package org.jboss.forge.furnace.impl.graph;

import java.util.Iterator;

import org.jboss.forge.furnace.addons.AddonView;
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
   public void vertexTraversed(VertexTraversalEvent<AddonVertex> event)
   {
      AddonVertex vertex = event.getVertex();

      try
      {
         Iterator<AddonDependencyEdge> dependencyIterator = graph.outgoingEdgesOf(vertex).iterator();
         if (dependencyIterator.hasNext() || depth == 0)
         {
            if (depth == 0)
            {
               builder.append("\n");
               builder.append(count++).append(": ").append(vertex.getName()).append(":")
                        .append(vertex.getVersion() == null ? MISSING : vertex.getVersion());

               appendViews(vertex);

               builder.append("\n");
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
                  builder.append(" (E)");
               }
               appendViews(dependency);
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
      catch (RuntimeException e)
      {
         throw e;
      }
   }

   private void appendViews(AddonVertex dependency)
   {
      builder.append(" - Views[");
      Iterator<AddonView> viewIterator = dependency.getViews().iterator();
      while (viewIterator.hasNext())
      {
         AddonView view = viewIterator.next();
         builder.append(view.getName());

         if (viewIterator.hasNext())
            builder.append(", ");
      }
      builder.append("] ");
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