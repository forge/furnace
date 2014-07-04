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

public class CORBAClasspathSpec extends AbstractModuleSpecProvider
{
   public static final ModuleIdentifier ID = ModuleIdentifier.create("org.omg.CORBA");

   public static Set<String> paths = new HashSet<String>();

   static
   {
      paths.add("org/omg/CORBA");
      paths.add("org/omg/CORBA_2_3");
      paths.add("org/omg/CORBA_2_3/portable");
      paths.add("org/omg/CORBA/DynAnyPackage");
      paths.add("org/omg/CORBA/ORBPackage");
      paths.add("org/omg/CORBA/portable");
      paths.add("org/omg/CORBA/TypeCodePackage");
      paths.add("org/omg/CosNaming");
      paths.add("org/omg/CosNaming/NamingContextExtPackage");
      paths.add("org/omg/CosNaming/NamingContextPackage");
      paths.add("org/omg/Dynamic");
      paths.add("org/omg/DynamicAny");
      paths.add("org/omg/DynamicAny/DynAnyFactoryPackage");
      paths.add("org/omg/DynamicAny/DynAnyPackage");
      paths.add("org/omg/IOP");
      paths.add("org/omg/IOP/CodecFactoryPackage");
      paths.add("org/omg/IOP/CodecPackage");
      paths.add("org/omg/Messaging");
      paths.add("org/omg/PortableInterceptor");
      paths.add("org/omg/PortableInterceptor/ORBInitInfoPackage");
      paths.add("org/omg/PortableServer");
      paths.add("org/omg/PortableServer/CurrentPackage");
      paths.add("org/omg/PortableServer/POAManagerPackage");
      paths.add("org/omg/PortableServer/POAPackage");
      paths.add("org/omg/PortableServer/portable");
      paths.add("org/omg/PortableServer/ServantLocatorPackage");
      paths.add("org/omg/SendingContext");
      paths.add("org/omg/stub/java/rmi");
      paths.add("org/omg/stub/javax/management/remote/rmi");
      paths.add("javax/rmi/CORBA");
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
