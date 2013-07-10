package org.jboss.forge.furnace.modules.providers;

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

   public static Set<String> paths = new HashSet<String>();

   static
   {
      paths.add("javassist");
      paths.add("javassist/bytecode");
      paths.add("javassist/bytecode/analysis");
      paths.add("javassist/bytecode/annotation");
      paths.add("javassist/bytecode/stackmap");
      paths.add("javassist/compiler");
      paths.add("javassist/compiler/ast");
      paths.add("javassist/convert");
      paths.add("javassist/expr");
      paths.add("javassist/runtime");
      paths.add("javassist/scopedpool");
      paths.add("javassist/tools");
      paths.add("javassist/tools/reflect");
      paths.add("javassist/tools/rmi");
      paths.add("javassist/tools/web");
      paths.add("javassist/util");
      paths.add("javassist/util/proxy");

      paths.add("org/jboss/forge");
      paths.add("org/jboss/forge/furnace");
      paths.add("org/jboss/forge/furnace/addons");
      paths.add("org/jboss/forge/furnace/event");
      paths.add("org/jboss/forge/furnace/exception");
      paths.add("org/jboss/forge/furnace/impl");
      paths.add("org/jboss/forge/furnace/impl/graph");
      paths.add("org/jboss/forge/furnace/lifecycle");
      paths.add("org/jboss/forge/furnace/lock");
      paths.add("org/jboss/forge/furnace/repositories");
      paths.add("org/jboss/forge/furnace/modules");
      paths.add("org/jboss/forge/furnace/modules/providers");
      paths.add("org/jboss/forge/furnace/services");
      paths.add("org/jboss/forge/furnace/spi");
      paths.add("org/jboss/forge/furnace/util");
      paths.add("org/jboss/forge/furnace/util/cdi");
      paths.add("org/jboss/forge/furnace/versions");
      paths.add("org/jboss/forge/parser");
      paths.add("org/jboss/forge/parser/xml");
      paths.add("org/jboss/forge/parser/xml/query");
      paths.add("org/jboss/forge/parser/xml/util");
      paths.add("org/jboss/forge/proxy");
      paths.add("org/jboss/forge/test");

      paths.add("org/jboss/logmanager");
      paths.add("org/jboss/logmanager/config");
      paths.add("org/jboss/logmanager/errormanager");
      paths.add("org/jboss/logmanager/filters");
      paths.add("org/jboss/logmanager/formatters");
      paths.add("org/jboss/logmanager/handlers");

      paths.add("org/jboss/modules");
      paths.add("org/jboss/modules/filter");
      paths.add("org/jboss/modules/log");
      paths.add("org/jboss/modules/management");
      paths.add("org/jboss/modules/ref");

      paths.add("META-INF/services");
   }

   @Override
   protected void configure(ModuleLoader loader, Builder builder)
   {
      builder.addDependency(DependencySpec.createSystemDependencySpec(
               PathFilters.acceptAll(),
               PathFilters.any(Arrays.asList(
                        PathFilters.is("META-INF/services"),
                        PathFilters.is("org/jboss/forge/furnace"),
                        PathFilters.isChildOf("org/jboss/forge/furnace"),
                        PathFilters.is("org/jboss/forge/proxy"),
                        PathFilters.isChildOf("org/jboss/forge/proxy"),
                        PathFilters.is("javassist"), PathFilters.isChildOf("javassist")
                        )),
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
