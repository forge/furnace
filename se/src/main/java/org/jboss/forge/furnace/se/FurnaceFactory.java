/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.se;

import java.util.Collections;
import java.util.Iterator;

import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.proxy.ClassLoaderAdapterBuilder;

public class FurnaceFactory
{

   public static Furnace getInstance()
   {
      try
      {
         final BootstrapClassLoader loader = new BootstrapClassLoader("bootpath");
         Class<?> furnaceType = loader.loadClass("org.jboss.forge.furnace.impl.FurnaceImpl");
         Object instance = furnaceType.newInstance();
         return (Furnace) ClassLoaderAdapterBuilder.callingLoader(FurnaceFactory.class.getClassLoader())
                  .delegateLoader(loader).whitelist(new FurnaceClassLoaderIterable(loader, instance))
                  .enhance(instance, Furnace.class);
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }

   public static Furnace getInstance(ClassLoader loader)
   {
      try
      {
         Class<?> furnaceType = loader.loadClass("org.jboss.forge.furnace.impl.FurnaceImpl");
         Object instance = furnaceType.newInstance();
         return (Furnace) ClassLoaderAdapterBuilder.callingLoader(FurnaceFactory.class.getClassLoader())
                  .delegateLoader(loader).whitelist(new FurnaceClassLoaderIterable(loader, instance))
                  .enhance(instance, Furnace.class);
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }

   private static class FurnaceClassLoaderIterable implements Iterable<ClassLoader>
   {
      private final Furnace furnace;

      public FurnaceClassLoaderIterable(ClassLoader loader, Object instance)
      {
         this.furnace = (Furnace) ClassLoaderAdapterBuilder.callingLoader(FurnaceFactory.class.getClassLoader())
                  .delegateLoader(loader).enhance(instance, Furnace.class);
      }

      @Override
      public Iterator<ClassLoader> iterator()
      {
         if (furnace.getStatus().isStarted())
         {
            final AddonRegistry registry = furnace.getAddonRegistry();
            return new Iterator<ClassLoader>()
            {
               private final Iterator<Addon> iterator = registry.getAddons().iterator();

               @Override
               public boolean hasNext()
               {
                  return iterator.hasNext();
               }

               @Override
               public ClassLoader next()
               {
                  return iterator.next().getClassLoader();
               }

               @Override
               public void remove()
               {
                  iterator.remove();
               }
            };
         }
         else
         {
            return Collections.emptyIterator();
         }
      }
   }
}
