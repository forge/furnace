/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.arquillian.services;

import java.lang.annotation.Annotation;

import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.event.EventException;
import org.jboss.forge.furnace.event.EventManager;
import org.jboss.forge.furnace.spi.ExportedInstance;
import org.jboss.forge.furnace.spi.ServiceRegistry;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class LazyServiceRegistryEventManager implements EventManager
{
   private Addon addon;

   public LazyServiceRegistryEventManager(Addon addon)
   {
      this.addon = addon;
   }

   @Override
   public void fireEvent(Object event, Annotation... qualifiers) throws EventException
   {
      ServiceRegistry registry = addon.getServiceRegistry();
      ExportedInstance<EventManager> instance = registry.getExportedInstance(EventManager.class);
      if (instance != null)
      {
         EventManager manager = instance.get();
         manager.fireEvent(event, qualifiers);
         instance.release(manager);
      }
   }

}
