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

public class SunJDKClasspathSpec extends AbstractModuleSpecProvider
{
   public static final ModuleIdentifier ID = ModuleIdentifier.create("sun.jdk");

   public static Set<String> paths = new HashSet<String>();

   static
   {
      paths.add("com/sun/org/apache/xml/internal/serializer");
      paths.add("com/sun/script/javascript");
      paths.add("com/sun/jndi/dns");
      paths.add("com/sun/jndi/ldap");
      paths.add("com/sun/jndi/url");
      paths.add("com/sun/jndi/url/dns");
      paths.add("com/sun/security/auth");
      paths.add("com/sun/security/auth/login");
      paths.add("com/sun/security/auth/module");
      paths.add("com/sun/tools/attach");
      paths.add("com/sun/media/sound");
      paths.add("sun/misc");
      paths.add("sun/io");
      paths.add("sun/nio");
      paths.add("sun/nio/ch");
      paths.add("sun/nio/cs");
      paths.add("sun/nio/fs");
      paths.add("sun/security");
      paths.add("sun/security/krb5");
      paths.add("sun/text/resources");
      paths.add("sun/util");
      paths.add("sun/util/calendar");
      paths.add("sun/util/locale");
      paths.add("sun/util/resources");
      paths.add("sun/security/provider");
      paths.add("META-INF/services");
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
