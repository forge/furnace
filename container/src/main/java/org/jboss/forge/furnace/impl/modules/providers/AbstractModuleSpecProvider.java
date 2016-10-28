/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.impl.modules.providers;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.jboss.forge.furnace.impl.modules.ModuleSpecProvider;
import org.jboss.modules.DependencySpec;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleSpec;
import org.jboss.modules.ModuleSpec.Builder;
import org.jboss.modules.filter.PathFilters;

/**
 * This class is the base class for any {@link ModuleSpecProvider} implementation inside Furnace
 */
public abstract class AbstractModuleSpecProvider implements ModuleSpecProvider
{
   @Override
   public ModuleSpec get(ModuleLoader loader, ModuleIdentifier id)
   {
      if (getId().equals(id))
      {
         Builder builder = ModuleSpec.build(id);
         builder.addDependency(DependencySpec.createClassLoaderDependencySpec(PathFilters.acceptAll(),
                  PathFilters.acceptAll(), AbstractModuleSpecProvider.class.getClassLoader(), getPaths()));
         builder.addDependency(DependencySpec.createClassLoaderDependencySpec(PathFilters.acceptAll(),
                  PathFilters.acceptAll(), ClassLoader.getSystemClassLoader(), getPaths()));

         configure(loader, builder);

         return builder.create();
      }
      return null;
   }

   protected void configure(ModuleLoader loader, Builder builder)
   {
   }

   @Override
   public abstract ModuleIdentifier getId();

   protected abstract Set<String> getPaths();

   private static Set<String> getPathsFrom(String root, File file)
   {
      Set<String> result = new HashSet<String>();
      String[] children = file.list();
      for (String name : children)
      {
         File child = new File(file, name);
         if (child.isDirectory())
         {
            result.addAll(getPathsFrom(root, child));
            String path = child.getAbsolutePath().substring(root.length() + 1);
            result.add(path);
         }
      }
      return result;
   }

   protected static Set<String> getLoaderPaths()
   {
      Set<String> result = new HashSet<String>();
      ClassLoader loader = ClassLoader.getSystemClassLoader();
      URL[] urls = ((URLClassLoader) loader).getURLs();

      for (URL url : urls)
      {
         try
         {
            File file = new File(url.toURI());
            if (file.isDirectory())
            {
               result.addAll(getPathsFrom(file.getAbsolutePath(), file));
            }
            else if (!file.isDirectory())
            {
               try (JarFile jar = new JarFile(file))
               {
                  Enumeration<JarEntry> entries = jar.entries();
                  while (entries.hasMoreElements())
                  {
                     JarEntry entry = entries.nextElement();
                     String name = entry.getName();
                     if (name.indexOf('/') != -1)
                     {
                        result.add(name.substring(0, name.lastIndexOf('/')));
                     }
                  }
               }
            }
         }
         catch (IOException e)
         {
            System.out.println("Failed loading paths from: [" + url.toString() + "]. Attempting folder discovery");
         }
         catch (URISyntaxException e)
         {
            throw new RuntimeException(e);
         }
      }
      return result;
   }
}
