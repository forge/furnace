package org.jboss.forge.furnace.impl.graph;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.forge.furnace.repositories.AddonRepository;
import org.jgrapht.DirectedGraph;
import org.jgrapht.event.TraversalListenerAdapter;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.traverse.DepthFirstIterator;

public class OptimizedAddonGraph extends AddonGraph<OptimizedAddonGraph>
{
   DirectedGraph<AddonVertex, AddonDependencyEdge> graph = new SimpleDirectedGraph<AddonVertex, AddonDependencyEdge>(
            AddonDependencyEdge.class);

   public OptimizedAddonGraph(List<AddonRepository> repositories, final DirectedGraph<AddonVertex, AddonDependencyEdge> completeGraph)
   {
      for (final AtomicInteger degree = new AtomicInteger(0); degree.get() < completeGraph.vertexSet().size(); degree
               .incrementAndGet())
      {
         DepthFirstIterator<AddonVertex, AddonDependencyEdge> iterator = new DepthFirstIterator<AddonVertex, AddonDependencyEdge>(
                  completeGraph);
         iterator.addTraversalListener(new TraversalListenerAdapter<AddonVertex, AddonDependencyEdge>()
         {
            public void vertexTraversed(org.jgrapht.event.VertexTraversalEvent<AddonVertex> event)
            {
               AddonVertex vertex = event.getVertex();
               if (degree.get() == completeGraph.inDegreeOf(vertex))
               {
                  System.out.println("DEGREE:" + degree.get() + " - " + vertex);
               }
            };
         });

         while (iterator.hasNext())
            iterator.next();
      }
   }

   @Override
   public DirectedGraph<AddonVertex, AddonDependencyEdge> getGraph()
   {
      return graph;
   }

}
