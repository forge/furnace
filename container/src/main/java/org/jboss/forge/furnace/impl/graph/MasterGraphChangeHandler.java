package org.jboss.forge.furnace.impl.graph;

import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.addons.AddonLifecycleManager;
import org.jboss.forge.furnace.addons.AddonView;
import org.jgrapht.event.TraversalListenerAdapter;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.traverse.DepthFirstIterator;

public class MasterGraphChangeHandler
{
   private AddonLifecycleManager lifecycleManager;
   private MasterGraph currentGraph;
   private MasterGraph graph;

   public MasterGraphChangeHandler(AddonLifecycleManager lifefycleManager,
            MasterGraph currentGraph, MasterGraph graph)
   {
      this.lifecycleManager = lifefycleManager;
      this.currentGraph = currentGraph;
      this.graph = graph;
   }

   public void hotSwapChanges()
   {
      initGraph();
      markDirty();
      stopDirty();
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

            if (isSubgraphEquivalent(vertex))
            {
               /*
                * If the dependency set of this addon has changed since the last graph, then it is dirty
                */
               vertex.setDirty(true);
            }
         };

         private boolean isSubgraphEquivalent(AddonVertex vertex)
         {
            if (currentGraph != null)
            {

            }
            return false;
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
         public void vertexFinished(VertexTraversalEvent<AddonVertex> event)
         {
            if (event.getVertex().isDirty())
               lifecycleManager.stopAddon(event.getVertex().getAddon());
         };
      });

      while (iterator.hasNext())
         iterator.next();
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
