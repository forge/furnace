package org.jboss.forge.furnace.impl.graph;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonLifecycleManager;
import org.jboss.forge.furnace.addons.AddonLoader;
import org.jboss.forge.furnace.addons.AddonView;
import org.jboss.forge.furnace.addons.StopAddonCallable;
import org.jboss.forge.furnace.util.Callables;
import org.jgrapht.event.TraversalListenerAdapter;
import org.jgrapht.traverse.DepthFirstIterator;

public class MasterGraphChangeHandler
{
   private AddonLifecycleManager manager;
   private Map<AddonView, Set<Addon>> addons;
   private MasterGraph currentGraph;
   private MasterGraph graph;

   public MasterGraphChangeHandler(AddonLifecycleManager manager, Map<AddonView, Set<Addon>> addons,
            MasterGraph currentGraph, MasterGraph graph)
   {
      this.manager = manager;
      this.addons = addons;
      this.currentGraph = currentGraph;
      this.graph = graph;
   }

   public void hotSwapChanges(AddonLoader loader)
   {
      if (currentGraph == null)
      {
         startAll(loader);
      }
      else
      {
         for (Entry<AddonView, Set<Addon>> entry : addons.entrySet())
         {
            AddonView view = entry.getKey();
            Set<Addon> addons = entry.getValue();

            // shut down only dirty addons

            for (Addon addon : addons)
            {
               Callables.call(new StopAddonCallable(addon));
            }
            addons.clear();

            startupView(loader, view);
         }
      }
   }

   private void startAll(final AddonLoader loader)
   {
      startupView(loader, null);
   }

   private void startupView(final AddonLoader loader, final AddonView view)
   {
      DepthFirstIterator<AddonVertex, AddonDependencyEdge> iterator = new DepthFirstIterator<AddonVertex, AddonDependencyEdge>(
               graph.getGraph());

      iterator.addTraversalListener(new TraversalListenerAdapter<AddonVertex, AddonDependencyEdge>()
      {
         public void vertexTraversed(org.jgrapht.event.VertexTraversalEvent<AddonVertex> event)
         {
            AddonVertex vertex = event.getVertex();
            Set<AddonView> owningViews = vertex.getViews();
            if (view == null || owningViews.contains(view) || view == manager)
            {
               Addon addon = null;
               for (AddonView owningView : owningViews)
               {
                  if (addon == null)
                     addon = loader.loadAddon(owningView, vertex.getAddonId());

                  manager.add(owningView, addon);
               }
               manager.startAddon(addon);
            }
         };
      });

      while (iterator.hasNext())
         iterator.next();
   }

}
