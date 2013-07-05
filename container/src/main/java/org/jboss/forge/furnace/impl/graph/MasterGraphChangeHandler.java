package org.jboss.forge.furnace.impl.graph;

import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.addons.AddonLifecycleManager;
import org.jboss.forge.furnace.addons.AddonView;
import org.jgrapht.event.TraversalListenerAdapter;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.graph.DirectedSubgraph;
import org.jgrapht.graph.Subgraph;
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
      startupIncremental();
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
            lifecycleManager.loadAddon(addon);

            vertex.setAddon(addon);
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
            AddonVertex vertex = event.getVertex();
            Addon addon = vertex.getAddon();
            lifecycleManager.startAddon(addon);
         };
      });

      while (iterator.hasNext())
         iterator.next();
   }
}
