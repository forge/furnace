package org.jboss.forge.furnace.impl.graph;

import org.jboss.forge.furnace.addons.AddonImpl;
import org.jboss.forge.furnace.addons.AddonLifecycleManager;
import org.jboss.forge.furnace.addons.AddonLoader;
import org.jgrapht.event.TraversalListenerAdapter;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.traverse.DepthFirstIterator;

public class MasterGraphChangeHandler
{
   private AddonLifecycleManager manager;
   private MasterGraph currentGraph;
   private MasterGraph graph;

   public MasterGraphChangeHandler(AddonLifecycleManager manager, MasterGraph currentGraph, MasterGraph graph)
   {
      this.manager = manager;
      this.currentGraph = currentGraph;
      this.graph = graph;
   }

   public void hotSwapChanges(AddonLoader loader)
   {
      startupIncremental(manager, loader);
   }

   private void startupIncremental(final AddonLifecycleManager manager, final AddonLoader loader)
   {
      DepthFirstIterator<AddonVertex, AddonDependencyEdge> iterator = new DepthFirstIterator<AddonVertex, AddonDependencyEdge>(
               graph.getGraph());

      iterator.addTraversalListener(new TraversalListenerAdapter<AddonVertex, AddonDependencyEdge>()
      {
         @Override
         public void vertexTraversed(VertexTraversalEvent<AddonVertex> event)
         {
            AddonVertex vertex = event.getVertex();
            AddonImpl addon = null;
            if (addon == null)
            {
               addon = loader.loadAddon(vertex.getViews(), vertex.getAddonId());

               if (addon != null && !addon.getStatus().isMissing())
               {
                  addon.setViews(vertex.getViews());
                  System.out.println("Queueing [" + addon + "] for startup.");
                  manager.startAddon(addon);
               }
               else
               {
                  System.out.println("Null/missing addon detected.");
               }
            }

         };
      });

      while (iterator.hasNext())
         iterator.next();
   }

}
