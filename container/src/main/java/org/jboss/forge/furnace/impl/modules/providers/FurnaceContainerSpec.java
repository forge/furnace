package org.jboss.forge.furnace.impl.modules.providers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jboss.modules.DependencySpec;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleSpec.Builder;
import org.jboss.modules.filter.PathFilters;

public class FurnaceContainerSpec extends AbstractModuleSpecProvider
{
   public static final ModuleIdentifier ID = ModuleIdentifier.create("org.jboss.forge.furnace.api");

   public static Set<String> paths = new HashSet<>();

   static
   {
      paths.add("org/jboss/forge/furnace/proxy/javassist");
      paths.add("org/jboss/forge/furnace/proxy/javassist/bytecode");
      paths.add("org/jboss/forge/furnace/proxy/javassist/bytecode/analysis");
      paths.add("org/jboss/forge/furnace/proxy/javassist/bytecode/annotation");
      paths.add("org/jboss/forge/furnace/proxy/javassist/bytecode/stackmap");
      paths.add("org/jboss/forge/furnace/proxy/javassist/compiler");
      paths.add("org/jboss/forge/furnace/proxy/javassist/compiler/ast");
      paths.add("org/jboss/forge/furnace/proxy/javassist/convert");
      paths.add("org/jboss/forge/furnace/proxy/javassist/expr");
      paths.add("org/jboss/forge/furnace/proxy/javassist/runtime");
      paths.add("org/jboss/forge/furnace/proxy/javassist/scopedpool");
      paths.add("org/jboss/forge/furnace/proxy/javassist/tools");
      paths.add("org/jboss/forge/furnace/proxy/javassist/tools/reflect");
      paths.add("org/jboss/forge/furnace/proxy/javassist/tools/rmi");
      paths.add("org/jboss/forge/furnace/proxy/javassist/tools/web");
      paths.add("org/jboss/forge/furnace/proxy/javassist/util");
      paths.add("org/jboss/forge/furnace/proxy/javassist/util/proxy");

      paths.add("org/jboss/forge/furnace");
      paths.add("org/jboss/forge/furnace/addons");
      paths.add("org/jboss/forge/furnace/event");
      paths.add("org/jboss/forge/furnace/exception");
      paths.add("org/jboss/forge/furnace/lifecycle");
      paths.add("org/jboss/forge/furnace/lock");
      paths.add("org/jboss/forge/furnace/repositories");
      paths.add("org/jboss/forge/furnace/services");
      paths.add("org/jboss/forge/furnace/spi");
      paths.add("org/jboss/forge/furnace/util");
      paths.add("org/jboss/forge/furnace/versions");

      paths.add("org/jboss/forge/furnace/proxy");
   }

   @Override
   protected void configure(ModuleLoader loader, Builder builder)
   {
      builder.addDependency(DependencySpec.createSystemDependencySpec(
               PathFilters.acceptAll(),
               PathFilters.all(
                        PathFilters.not(PathFilters.any(
                                 PathFilters.is("org/jboss/forge/furnace/impl"),
                                 PathFilters.isChildOf("org/jboss/forge/furnace/impl"))),

                        PathFilters.any(Arrays.asList(
                                 PathFilters.is("org/jboss/forge/furnace/proxy/javassist"),
                                 PathFilters.isChildOf("org/jboss/forge/furnace/proxy/javassist"),
                                 PathFilters.is("META-INF/services"),
                                 PathFilters.is("org/jboss/forge/furnace"),
                                 PathFilters.is("org/jboss/forge/furnace/addons"),
                                 PathFilters.is("org/jboss/forge/furnace/event"),
                                 PathFilters.is("org/jboss/forge/furnace/exception"),
                                 PathFilters.is("org/jboss/forge/furnace/lifecycle"),
                                 PathFilters.is("org/jboss/forge/furnace/lock"),
                                 PathFilters.is("org/jboss/forge/furnace/repositories"),
                                 PathFilters.is("org/jboss/forge/furnace/services"),
                                 PathFilters.is("org/jboss/forge/furnace/spi"),
                                 PathFilters.is("org/jboss/forge/furnace/util"),
                                 PathFilters.is("org/jboss/forge/furnace/versions"),
                                 PathFilters.is("org/jboss/forge/furnace/proxy")
                                 ))),
               getPaths()));
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
