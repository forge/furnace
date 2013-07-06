package org.jboss.forge.furnace.impl.graph;

import java.util.Set;

import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.addons.AddonLifecycleManager;
import org.jboss.forge.furnace.addons.AddonView;
import org.jgrapht.DirectedGraph;
import org.jgrapht.event.TraversalListenerAdapter;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.traverse.DepthFirstIterator;

public class MasterGraphChangeHandler
{
   private AddonLifecycleManager lifecycleManager;
   private MasterGraph lastMasterGraph;
   private MasterGraph graph;

   public MasterGraphChangeHandler(AddonLifecycleManager lifefycleManager,
            MasterGraph currentGraph, MasterGraph graph)
   {
      this.lifecycleManager = lifefycleManager;
      this.lastMasterGraph = currentGraph;
      this.graph = graph;
   }

   public void hotSwapChanges()
   {
      initGraph();
      markDirty();
      stopDirty();
      stopRemoved();
      loadAddons();
      startupIncremental();
      clearDirtyStatus();
   }

   private void initGraph()
   {
      DepthFirstIterator<AddonVertex, AddonDependencyEdge> iterator = new DepthFirstIterator<AddonVertex, AddonDependencyEdge>(
               graph.getGraph());

      iterator.addTraversalListener(new TraversalListenerAdapter<AddonVertex, AddonDependencyEdge>()
      {
         @Override
         public void vertexFinished(VertexTraversalEvent<AddonVertex> event)
         {
            AddonVertex vertex = event.getVertex();
            AddonView view = vertex.getViews().iterator().next();
            AddonId addonId = vertex.getAddonId();
            Addon addon = lifecycleManager.getAddon(view, addonId);
            vertex.setAddon(addon);
         };
      });

      while (iterator.hasNext())
         iterator.next();
   }

   private void markDirty()
   {
      DepthFirstIterator<AddonVertex, AddonDependencyEdge> iterator = new DepthFirstIterator<AddonVertex, AddonDependencyEdge>(
               graph.getGraph());

      iterator.addTraversalListener(new TraversalListenerAdapter<AddonVertex, AddonDependencyEdge>()
      {
         @Override
         public void vertexFinished(VertexTraversalEvent<AddonVertex> event)
         {
            // If this vertex is missing or any dependency was missing (is dirty), then this is dirty also
            AddonVertex vertex = event.getVertex();
            Addon addon = vertex.getAddon();
            if (addon.getStatus().isMissing() || addon.getStatus().isFailed())
            {
               vertex.setDirty(true);
            }

            for (AddonDependencyEdge edge : graph.getGraph().outgoingEdgesOf(vertex))
            {
               AddonVertex target = graph.getGraph().getEdgeTarget(edge);
               if (target.isDirty())
                  vertex.setDirty(true);
            }

            if (!isSubgraphEquivalent(vertex))
            {
               /*
                * If the dependency set of this addon has changed since the last graph, then it is dirty
                */
               vertex.setDirty(true);
            }
         };

         private boolean isSubgraphEquivalent(AddonVertex vertex)
         {
            boolean result = true;
            if (lastMasterGraph != null)
            {
               AddonVertex oldVertex = lastMasterGraph.getVertex(vertex.getName(), vertex.getVersion());
               if (oldVertex != null)
               {
                  DirectedGraph<AddonVertex, AddonDependencyEdge> lastGraph = lastMasterGraph.getGraph();
                  Set<AddonDependencyEdge> outgoing = lastGraph.outgoingEdgesOf(oldVertex);

                  for (AddonDependencyEdge lastEdge : outgoing)
                  {
                     AddonVertex lastTarget = lastGraph.getEdgeTarget(lastEdge);
                     AddonVertex target = graph.getVertex(lastTarget.getName(), lastTarget.getVersion());
                     AddonDependencyEdge edge = graph.getGraph().getEdge(vertex, target);
                     if (edge == null || !isSubgraphEquivalent(target))
                     {
                        result = false;
                        break;
                     }
                  }
               }
            }
            return result;
         }
      });

      while (iterator.hasNext())
         iterator.next();
   }

   private void stopDirty()
   {
      DepthFirstIterator<AddonVertex, AddonDependencyEdge> iterator = new DepthFirstIterator<AddonVertex, AddonDependencyEdge>(
               graph.getGraph());

      iterator.addTraversalListener(new TraversalListenerAdapter<AddonVertex, AddonDependencyEdge>()
      {
         @Override
         public void vertexTraversed(VertexTraversalEvent<AddonVertex> event)
         {
            if (event.getVertex().isDirty())
               lifecycleManager.stopAddon(event.getVertex().getAddon());
         };
      });

      while (iterator.hasNext())
         iterator.next();
   }

   private void stopRemoved()
   {
      if (lastMasterGraph != null)
      {
         DepthFirstIterator<AddonVertex, AddonDependencyEdge> iterator = new DepthFirstIterator<AddonVertex, AddonDependencyEdge>(
                  lastMasterGraph.getGraph());

         iterator.addTraversalListener(new TraversalListenerAdapter<AddonVertex, AddonDependencyEdge>()
         {
            @Override
            public void vertexFinished(VertexTraversalEvent<AddonVertex> event)
            {
               AddonVertex lastVertex = event.getVertex();
               if (graph.getVertex(lastVertex.getName(), lastVertex.getVersion()) == null)
                  lifecycleManager.stopAddon(lastVertex.getAddon());
            };
         });

         while (iterator.hasNext())
            iterator.next();
      }
   }

   private void loadAddons()
   {
      DepthFirstIterator<AddonVertex, AddonDependencyEdge> iterator = new DepthFirstIterator<AddonVertex, AddonDependencyEdge>(
               graph.getGraph());

      iterator.addTraversalListener(new TraversalListenerAdapter<AddonVertex, AddonDependencyEdge>()
      {
         @Override
         public void vertexFinished(VertexTraversalEvent<AddonVertex> event)
         {
            Addon addon = event.getVertex().getAddon();
            if (addon.getStatus().isMissing())
               lifecycleManager.loadAddon(addon);
         };
      });

      while (iterator.hasNext())
         iterator.next();
   }

   private void startupIncremental()
   {
      DepthFirstIterator<AddonVertex, AddonDependencyEdge> iterator = new DepthFirstIterator<AddonVertex, AddonDependencyEdge>(
               graph.getGraph());

      iterator.addTraversalListener(new TraversalListenerAdapter<AddonVertex, AddonDependencyEdge>()
      {
         @Override
         public void vertexFinished(VertexTraversalEvent<AddonVertex> event)
         {
            Addon addon = event.getVertex().getAddon();
            if (addon.getStatus().isLoaded())
               lifecycleManager.startAddon(addon);
         };
      });

      while (iterator.hasNext())
         iterator.next();
   }

   private void clearDirtyStatus()
   {
      DepthFirstIterator<AddonVertex, AddonDependencyEdge> iterator = new DepthFirstIterator<AddonVertex, AddonDependencyEdge>(
               graph.getGraph());

      iterator.addTraversalListener(new TraversalListenerAdapter<AddonVertex, AddonDependencyEdge>()
      {
         @Override
         public void vertexFinished(VertexTraversalEvent<AddonVertex> event)
         {
            event.getVertex().setDirty(false);
         };
      });

      while (iterator.hasNext())
         iterator.next();
   }
}
