/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.se;

import java.util.Set;
import java.util.concurrent.Callable;

import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.proxy.ClassLoaderAdapterBuilder;
import org.jboss.forge.furnace.util.Sets;

/**
 * Use to obtain {@link Furnace} instances in various class-loading scenarios.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class FurnaceFactory
{
   /**
    * Produce a {@link Furnace} instance using the default bootstrap {@link ClassLoader} to load core furnace
    * implementation JARs from `<code>src/main/java/bootpath/*</code>`.
    */
   public static Furnace getInstance()
   {
      try
      {
         final BootstrapClassLoader loader = new BootstrapClassLoader("bootpath");
         return getInstance(FurnaceFactory.class.getClassLoader(), loader);
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }

   /**
    * Produce a {@link Furnace} instance using the given {@link ClassLoader} to load core furnace implementation
    * classes.
    */
   public static Furnace getInstance(final ClassLoader clientLoader)
   {
      final BootstrapClassLoader loader = new BootstrapClassLoader("bootpath");
      return getInstance(clientLoader, loader);
   }

   /**
    * Produce a {@link Furnace} instance using the first given {@link ClassLoader} to load core furnace implementation
    * classes, and the second given {@link ClassLoader} to act as the client for which {@link Class} instances should be
    * translated across {@link ClassLoader} boundaries.
    */
   public static Furnace getInstance(final ClassLoader clientLoader, final ClassLoader furnaceLoader)
   {
      try
      {
         Class<?> furnaceType = furnaceLoader.loadClass("org.jboss.forge.furnace.impl.FurnaceImpl");
         final Object instance = furnaceType.newInstance();

         final Furnace furnace = (Furnace) ClassLoaderAdapterBuilder
                  .callingLoader(clientLoader)
                  .delegateLoader(furnaceLoader)
                  .enhance(instance, Furnace.class);

         Callable<Set<ClassLoader>> whitelistCallback = new Callable<Set<ClassLoader>>()
         {
            volatile long lastRegistryVersion = -1;
            final Set<ClassLoader> result = Sets.getConcurrentSet();

            @Override
            public Set<ClassLoader> call() throws Exception
            {
               if (furnace.getStatus().isStarted())
               {
                  long registryVersion = furnace.getAddonRegistry().getVersion();
                  if (registryVersion != lastRegistryVersion)
                  {
                     result.clear();
                     lastRegistryVersion = registryVersion;
                     for (Addon addon : furnace.getAddonRegistry().getAddons())
                     {
                        ClassLoader classLoader = addon.getClassLoader();
                        if (classLoader != null)
                           result.add(classLoader);
                     }
                  }
               }

               return result;
            }
         };

         return (Furnace) ClassLoaderAdapterBuilder
                  .callingLoader(clientLoader)
                  .delegateLoader(furnaceLoader)
                  .whitelist(whitelistCallback)
                  .enhance(instance, Furnace.class);
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }
}
