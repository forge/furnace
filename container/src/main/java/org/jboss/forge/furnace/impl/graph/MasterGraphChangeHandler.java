package org.jboss.forge.furnace.impl.graph;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonLoader;
import org.jboss.forge.furnace.addons.AddonView;
import org.jboss.forge.furnace.addons.StopAddonCallable;
import org.jboss.forge.furnace.util.Callables;
import org.jboss.forge.furnace.versions.EmptyVersion;
import org.jboss.forge.furnace.versions.EmptyVersionRange;
import org.jgrapht.event.TraversalListenerAdapter;
import org.jgrapht.traverse.DepthFirstIterator;

public class MasterGraphChangeHandler
{
   private Map<AddonView, Set<Addon>> addons;
   private MasterGraph currentGraph;
   private MasterGraph graph;

   public MasterGraphChangeHandler(Map<AddonView, Set<Addon>> addons, MasterGraph currentGraph, MasterGraph graph)
   {
      this.addons = addons;
      this.currentGraph = currentGraph;
      this.graph = graph;
   }

   public void hotSwapChanges(AddonLoader loader)
   {
      if (currentGraph == null)
         startAll(loader);

      for (Entry<AddonView, Set<Addon>> entry : addons.entrySet())
      {
         AddonView view = entry.getKey();
         Set<Addon> addons = entry.getValue();
         for (Addon addon : addons)
         {
            Callables.call(new StopAddonCallable(addon));
            addons.clear();
         }

         startupView(view);
      }
   }

   private void startAll(final AddonLoader loader)
   {
      DepthFirstIterator<AddonVertex, AddonDependencyEdge> iterator = new DepthFirstIterator<AddonVertex, AddonDependencyEdge>(
               graph.getGraph());

      iterator.addTraversalListener(new TraversalListenerAdapter<AddonVertex, AddonDependencyEdge>()
      {
         public void vertexTraversed(org.jgrapht.event.VertexTraversalEvent<AddonVertex> event)
         {
            AddonVertex vertex = event.getVertex();
            Set<AddonView> views = vertex.getViews();
            loader.loadAddon(views, vertex.getAddonId());
         };
      });

      while (iterator.hasNext())
         iterator.next();
   }

}
