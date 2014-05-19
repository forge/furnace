/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.arquillian.services;

import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.proxy.ClassLoaderInterceptor;
import org.jboss.forge.furnace.proxy.Proxies;
import org.jboss.forge.furnace.spi.ExportedInstance;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class ReflectionExportedInstance<T> implements ExportedInstance<T>
{
   private final Class<T> type;
   private final Addon addon;

   public ReflectionExportedInstance(Addon addon, Class<T> clazz)
   {
      this.addon = addon;
      this.type = clazz;
   }

   @Override
   public T get()
   {
      T delegate = Proxies.instantiate(type);
      delegate = Proxies.enhance(addon.getClassLoader(), delegate, new ClassLoaderInterceptor(
               addon.getClassLoader(), delegate));
      return delegate;
   }

   @Override
   public void release(T instance)
   {
      // no action required
   }

   @Override
   public String toString()
   {
      return type.getName() + " from " + addon;
   }

   @Override
   public Class<? extends T> getActualType()
   {
      return type;
   }

   @Override
   public Addon getSourceAddon()
   {
      return addon;
   }
}
