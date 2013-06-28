package org.jboss.forge.furnace.modules.providers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jboss.modules.DependencySpec;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleSpec.Builder;
import org.jboss.modules.filter.PathFilters;

public class JGraphTClasspathSpec extends AbstractModuleSpecProvider
{
   public static final ModuleIdentifier ID = ModuleIdentifier.create("org.jgrapht");

   public static Set<String> paths = new HashSet<String>();

   static
   {
      paths.add("org/jgrapht");
      paths.add("org/jgrapht/alg");
      paths.add("org/jgrapht/alg.util");
      paths.add("org/jgrapht/demo");
      paths.add("org/jgrapht/event");
      paths.add("org/jgrapht/experimental");
      paths.add("org/jgrapht/experimental/alg");
      paths.add("org/jgrapht/experimental/alg/color");
      paths.add("org/jgrapht/experimental/dag");
      paths.add("org/jgrapht/experimental/equivalence");
      paths.add("org/jgrapht/experimental/isomorphism");
      paths.add("org/jgrapht/experimental/permutation");
      paths.add("org/jgrapht/experimental/touchgraph");
      paths.add("org/jgrapht/ext");
      paths.add("org/jgrapht/generate");
      paths.add("org/jgrapht/graph");
      paths.add("org/jgrapht/traverse");
      paths.add("org/jgrapht/util");
   }

   @Override
   protected void configure(ModuleLoader loader, Builder builder)
   {
      builder.addDependency(DependencySpec.createSystemDependencySpec(
               PathFilters.acceptAll(),
               PathFilters.any(Arrays.asList(
                        PathFilters.isChildOf("org/jgrapht"),
                        PathFilters.is("org/jgrapht")
                        )),
               systemPaths));
   }

   @Override
   protected ModuleIdentifier getId()
   {
      return ID;
   }

   @Override
   protected Set<String> getPaths()
   {
      return paths;
   }

}
