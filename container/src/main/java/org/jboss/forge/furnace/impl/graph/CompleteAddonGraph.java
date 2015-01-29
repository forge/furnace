/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.impl.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.impl.addons.AddonRepositoryImpl;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.forge.furnace.repositories.AddonRepository;
import org.jboss.forge.furnace.versions.EmptyVersion;
import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.SimpleDirectedGraph;

public class CompleteAddonGraph extends AddonGraph<CompleteAddonGraph>
{
   private Logger logger = Logger.getLogger(CompleteAddonGraph.class.getName());

   DirectedGraph<AddonVertex, AddonDependencyEdge> graph = new SimpleDirectedGraph<AddonVertex, AddonDependencyEdge>(
            AddonDependencyEdge.class);

   public CompleteAddonGraph(Collection<AddonRepository> repositories)
   {
      Set<AddonId> enabled = getAllEnabledAddonsInAllRepositories(repositories);
      Map<AddonId, Set<AddonDependencyEntry>> dependencyMap = new LinkedHashMap<AddonId, Set<AddonDependencyEntry>>();
      for (AddonId id : enabled)
      {
         for (AddonRepository repository : repositories)
         {
            if (repository.isEnabled(id))
            {
               Set<AddonDependencyEntry> dependencies = repository.getAddonDependencies(id);
               dependencyMap.put(id, dependencies);
            }
         }
      }

      for (Entry<AddonId, Set<AddonDependencyEntry>> entry : dependencyMap.entrySet())
      {
         AddonVertex vertex = getOrCreateVertex(entry.getKey().getName(), entry.getKey().getVersion());

         for (AddonDependencyEntry dependency : entry.getValue())
         {
            boolean satisfied = false;
            for (AddonId id : enabled)
            {
               if (dependency.getName().equals(id.getName()) && dependency.getVersionRange().includes(id.getVersion()))
               {
                  AddonVertex dependencyVertex = getOrCreateVertex(id.getName(), id.getVersion());
                  graph.addEdge(vertex, dependencyVertex, new AddonDependencyEdge(dependency.getVersionRange(),
                           dependency.isExported()));
                  satisfied = true;
               }
            }

            if (!satisfied && !dependency.isOptional())
            {
               AddonVertex missingVertex = new AddonVertex(dependency.getName(), EmptyVersion.getInstance());
               graph.addVertex(missingVertex);
               graph.addEdge(vertex, missingVertex,
                        new AddonDependencyEdge(dependency.getVersionRange(), dependency.isExported()));
            }
         }
      }

      CycleDetector<AddonVertex, AddonDependencyEdge> detector = new CycleDetector<AddonVertex, AddonDependencyEdge>(
               graph);
      if (detector.detectCycles())
      {
         throw new IllegalStateException("Cycle detected in Addon graph: " + detector.findCycles());
      }
   }

   private Set<AddonId> getAllEnabledAddonsInAllRepositories(Collection<AddonRepository> repositories)
   {
      Set<AddonId> result = new HashSet<AddonId>();
      for (AddonRepository repository : repositories)
      {
         List<AddonId> enabled = repository.listEnabled();
         List<AddonId> enabledCompatible = repository.listEnabledCompatibleWithVersion(AddonRepositoryImpl
                  .getRuntimeAPIVersion());

         result.addAll(enabledCompatible);

         List<AddonId> incompatible = new ArrayList<>(enabled);
         incompatible.removeAll(enabledCompatible);
         for (AddonId addon : incompatible)
         {
            if (addon.getApiVersion() != null)
               logger.warning("Addon [" + addon + "] with API version [" + addon.getApiVersion()
                        + "] is incompatible with the current Furnace runtime version ["
                        + AddonRepositoryImpl.getRuntimeAPIVersion() + "] and will not be loaded, from repository ["
                        + repository + "]");
         }
      }
      return result;
   }

   @Override
   public DirectedGraph<AddonVertex, AddonDependencyEdge> getGraph()
   {
      return graph;
   }

}
