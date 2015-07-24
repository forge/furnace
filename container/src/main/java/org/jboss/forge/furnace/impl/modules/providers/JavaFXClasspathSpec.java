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
 * Support JavaFX classes in Furnace
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class JavaFXClasspathSpec extends AbstractModuleSpecProvider
{
   public static final ModuleIdentifier ID = ModuleIdentifier.create("javafx.api");

   public static Set<String> paths = new HashSet<String>();

   static
   {
      paths.add("javafx/animation");
      paths.add("javafx/application");
      paths.add("javafx/beans");
      paths.add("javafx/beans/binding");
      paths.add("javafx/beans/property");
      paths.add("javafx/beans/property/adapter");
      paths.add("javafx/beans/value");
      paths.add("javafx/collections");
      paths.add("javafx/collections/transformation");
      paths.add("javafx/concurrent");
      paths.add("javafx/css");
      paths.add("javafx/embed/swing");
      paths.add("javafx/event");
      paths.add("javafx/fxml");
      paths.add("javafx/geometry");
      paths.add("javafx/print");
      paths.add("javafx/scene");
      paths.add("javafx/scene/canvas");
      paths.add("javafx/scene/chart");
      paths.add("javafx/scene/control");
      paths.add("javafx/scene/control/cell");
      paths.add("javafx/scene/effect");
      paths.add("javafx/scene/image");
      paths.add("javafx/scene/input");
      paths.add("javafx/scene/layout");
      paths.add("javafx/scene/media");
      paths.add("javafx/scene/paint");
      paths.add("javafx/scene/shape");
      paths.add("javafx/scene/text");
      paths.add("javafx/scene/transform");
      paths.add("javafx/scene/web");
      paths.add("javafx/stage");
      paths.add("javafx/util");
      paths.add("javafx/util/converter");
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
