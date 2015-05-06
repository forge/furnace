/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.impl.addons;

import java.util.Collections;
import java.util.Set;

import org.jboss.forge.furnace.spi.ExportedInstance;
import org.jboss.forge.furnace.spi.ServiceRegistry;

/**
 * Used when an addon does not provide services.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public enum NullServiceRegistry implements ServiceRegistry
{
   INSTANCE;

   @Override
   public <T> ExportedInstance<T> getExportedInstance(Class<T> serviceType)
   {
      // no-op
      return null;
   }

   @Override
   public <T> ExportedInstance<T> getExportedInstance(String serviceType)
   {
      // no-op
      return null;
   }

   @Override
   public <T> Set<ExportedInstance<T>> getExportedInstances(Class<T> serviceType)
   {
      // no-op
      return Collections.emptySet();
   }

   @Override
   public <T> Set<ExportedInstance<T>> getExportedInstances(String clazz)
   {
      // no-op
      return Collections.emptySet();
   }

   @Override
   public boolean hasService(Class<?> serviceType)
   {
      // no-op
      return false;
   }

   @Override
   public boolean hasService(String clazz)
   {
      // no-op
      return false;
   }

   @Override
   public Set<Class<?>> getExportedTypes()
   {
      // no-op
      return Collections.emptySet();
   }

   @Override
   public <T> Set<Class<T>> getExportedTypes(Class<T> type)
   {
      // no-op
      return Collections.emptySet();
   }

}
