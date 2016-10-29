/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.impl.modules.providers;

import java.util.HashSet;
import java.util.Set;

import org.jboss.modules.ModuleIdentifier;

/**
 * Support Nashorn classes in Furnace
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class NashornJDKClasspathSpec extends AbstractModuleSpecProvider
{
   public static final ModuleIdentifier ID = ModuleIdentifier.create("jdk.nashorn");

   public static Set<String> paths = new HashSet<String>();

   static
   {
      paths.add("jdk/nashorn/api/scripting");
      paths.add("jdk/nashorn/api/resources");
      paths.add("jdk/nashorn/internal");
      paths.add("jdk/nashorn/internal/codegen");
      paths.add("jdk/nashorn/internal/codegen/types");
      paths.add("jdk/nashorn/internal/ir");
      paths.add("jdk/nashorn/internal/ir/annotations");
      paths.add("jdk/nashorn/internal/ir/debug");
      paths.add("jdk/nashorn/internal/ir/visitor");
      paths.add("jdk/nashorn/internal/lookup");
      paths.add("jdk/nashorn/internal/objects");
      paths.add("jdk/nashorn/internal/objects/annotations");
      paths.add("jdk/nashorn/internal/parser");
      paths.add("jdk/nashorn/internal/runtime");
      paths.add("jdk/nashorn/internal/runtime/arrays");
      paths.add("jdk/nashorn/internal/runtime/events");
      paths.add("jdk/nashorn/internal/runtime/linker");
      paths.add("jdk/nashorn/internal/runtime/logging");
      paths.add("jdk/nashorn/internal/runtime/options");
      paths.add("jdk/nashorn/internal/runtime/regexp");
      paths.add("jdk/nashorn/internal/runtime/regexp/joni");
      paths.add("jdk/nashorn/internal/runtime/regexp/joni/ast");
      paths.add("jdk/nashorn/internal/runtime/regexp/joni/constants");
      paths.add("jdk/nashorn/internal/runtime/regexp/joni/encoding");
      paths.add("jdk/nashorn/internal/runtime/regexp/joni/exception");
      paths.add("jdk/nashorn/internal/runtime/resources");
      paths.add("jdk/nashorn/internal/runtime/resources/fx");
      paths.add("jdk/nashorn/internal/scripts");
      paths.add("jdk/nashorn/tools");
      paths.add("jdk/nashorn/tools/resources");
   }

   @Override
   public ModuleIdentifier getId()
   {
      return ID;
   }

   @Override
   protected Set<String> getPaths()
   {
      return paths;
   }
}
