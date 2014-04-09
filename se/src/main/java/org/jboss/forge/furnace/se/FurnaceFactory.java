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

public class FurnaceFactory
{

   public static Furnace getInstance()
   {
      try
      {
         final BootstrapClassLoader loader = new BootstrapClassLoader("bootpath");
         return getInstance(loader);
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }

   public static Furnace getInstance(final ClassLoader loader)
   {
      try
      {
         Class<?> furnaceType = loader.loadClass("org.jboss.forge.furnace.impl.FurnaceImpl");
         final Object instance = furnaceType.newInstance();

         final Furnace furnace = (Furnace) ClassLoaderAdapterBuilder
                  .callingLoader(FurnaceFactory.class.getClassLoader())
                  .delegateLoader(loader).enhance(instance, Furnace.class);

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

         return (Furnace) ClassLoaderAdapterBuilder.callingLoader(FurnaceFactory.class.getClassLoader())
                  .delegateLoader(loader).whitelist(whitelistCallback)
                  .enhance(instance, Furnace.class);
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }
}
