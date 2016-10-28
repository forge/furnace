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

public class XPathJDKClasspathSpec extends AbstractModuleSpecProvider
{
   public static final ModuleIdentifier ID = ModuleIdentifier.create("sun.jdk.xpath");

   public static Set<String> paths = new HashSet<String>();

   static
   {
      paths.add("com/sun/org/apache/xalan/internal/xsltc/trax");
      paths.add("com/sun/org/apache/xerces/internal/jaxp");
      paths.add("com/sun/org/apache/xerces/internal/jaxp/datatype");
      paths.add("com/sun/org/apache/xerces/internal/jaxp/validation");
      paths.add("com/sun/org/apache/xerces/internal/parsers");
      paths.add("com/sun/org/apache/xml/internal");
      paths.add("com/sun/org/apache/xml/internal/serializer");
      paths.add("com/sun/org/apache/xml/internal/util");
      paths.add("com/sun/org/apache/xpath/internal/jaxp");
      paths.add("com/sun/xml/internal/stream");
      paths.add("com/sun/xml/internal/stream/events");
      paths.add("javax/xml/xpath");
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
