/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.se;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.proxy.ClassLoaderAdapterBuilder;

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

         return (Furnace) ClassLoaderAdapterBuilder.callingLoader(FurnaceFactory.class.getClassLoader())
                  .delegateLoader(loader).whitelist(new Callable<Set<ClassLoader>>()
                  {
                     volatile long lastRegistryVersion = 0;
                     final Set<ClassLoader> result = new HashSet<>();

                     @Override
                     public Set<ClassLoader> call() throws Exception
                     {
                        if (result == null)
                        {
                           if (furnace.getStatus().isStarted())
                           {
                              long registryVersion = furnace.getAddonRegistry().getVersion();
                              if (registryVersion > lastRegistryVersion)
                              {
                                 lastRegistryVersion = registryVersion;
                                 for (Addon addon : furnace.getAddonRegistry().getAddons())
                                 {
                                    result.add(addon.getClassLoader());
                                 }
                              }
                           }
                        }

                        return result;
                     }
                  })
                  .enhance(instance, Furnace.class);
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }
}
