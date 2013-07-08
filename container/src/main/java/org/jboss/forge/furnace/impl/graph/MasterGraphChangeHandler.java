package org.jboss.forge.furnace.impl.graph;

import java.util.Set;

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
      if (lastMasterGraph != null)
      {
         /*
          * Propagate forward any addons that were removed, but still need to be shut down. This prevents duplicate
          * Addon objects from being registered in the lifecycle manager.
          */
         for (AddonVertex last : lastMasterGraph.getGraph().vertexSet())
         {
            boolean found = false;
            Set<AddonVertex> vertices = graph.getGraph().vertexSet();
            for (AddonVertex vertex : vertices)
            {
               if (last.getName().equals(vertex.getName()))
               {
                  for (AddonView view : vertex.getViews())
                  {
                     if (last.getViews().contains(view))
                     {
                        found = true;
                        break;
                     }
                  }
               }

               if (found)
                  break;
            }

            if (!found && !last.getAddon().getStatus().isMissing())
            {
               graph.getGraph().addVertex(last);
               last.setDirty(true);
            }
         }
      }

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

            Addon addon = null;
            if (lastMasterGraph != null)
            {
               for (AddonVertex last : lastMasterGraph.getGraph().vertexSet())
               {
                  if (last.getAddon().getId().equals(addonId) && last.getViews().contains(view))
                  {
                     addon = last.getAddon();
                     break;
                  }
               }

               if (addon == null)
               {

               }
            }

            if (addon == null)
               addon = lifecycleManager.getAddon(view, addonId);

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

            if (lastMasterGraph != null)
            {
               boolean equivalent = false;
               Set<AddonVertex> lastVertices = lastMasterGraph.getVertices(vertex.getName(), vertex.getVersion());
               for (AddonVertex lastVertex : lastVertices)
               {
                  if (graph.isSubtreeEquivalent(vertex, lastMasterGraph.getGraph(), lastVertex))
                  {
                     equivalent = true;
                     break;
                  }
               }

               if (!equivalent)
                  vertex.setDirty(true);
            }
         };
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
               boolean exists = false;
               for (AddonVertex vertex : graph.getVertices(lastVertex.getName(), lastVertex.getVersion()))
               {
                  for (AddonView view : lastVertex.getViews())
                  {
                     if (vertex.getViews().contains(view))
                     {
                        exists = true;
                        break;
                     }
                  }
               }

               if (!exists)
               {
                  lifecycleManager.stopAddon(lastVertex.getAddon());
               }
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
