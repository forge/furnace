/**
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.impl.modules.providers;

import java.util.HashSet;
import java.util.Set;

import org.jboss.forge.furnace.impl.modules.ModuleSpecProvider;
import org.jboss.modules.ModuleIdentifier;

/**
 * {@link ModuleSpecProvider} for the JAXB classes
 * 
 * @author <a href="ggastald@redhat.com">George Gastaldi</a>
 */
public class JAXBJDKClasspathSpec extends AbstractModuleSpecProvider
{
   public static final ModuleIdentifier ID = ModuleIdentifier.create("sun.jdk.jaxb");

   public static Set<String> paths = new HashSet<String>();

   static
   {
      paths.add("com/sun/xml/internal/bind");
      paths.add("com/sun/xml/internal/bind/v2");
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
