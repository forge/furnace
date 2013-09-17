/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.arquillian.services;

import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.event.EventManager;
import org.jboss.forge.furnace.event.PostStartup;
import org.jboss.forge.furnace.event.PreShutdown;
import org.jboss.forge.furnace.lifecycle.AddonLifecycleProvider;
import org.jboss.forge.furnace.lifecycle.ControlType;
import org.jboss.forge.furnace.spi.ServiceRegistry;
import org.jboss.forge.furnace.util.ClassLoaders;
import org.jboss.forge.furnace.util.Streams;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class LocalServices implements AddonLifecycleProvider
{
   private static final String SERVICE_REGISTRATION_FILE_NAME = "org.jboss.forge.furnace.services.Exported";
   private Furnace furnace;
   private static Map<ClassLoader, Furnace> started = new ConcurrentHashMap<ClassLoader, Furnace>();

   @Override
   public void initialize(Furnace furnace, AddonRegistry registry, Addon self) throws Exception
   {
      this.furnace = furnace;
   }

   @Override
   public void start(Addon addon) throws Exception
   {
      started.put(addon.getClassLoader(), furnace);
   }

   @Override
   public void stop(Addon addon) throws Exception
   {
      started.remove(addon.getClassLoader());
   }

   public static Furnace getFurnace(ClassLoader loader)
   {
      return started.get(loader);
   }

   @Override
   public EventManager getEventManager(Addon addon)
   {
      return new LazyServiceRegistryEventManager(addon);
   }

   @Override
   public ServiceRegistry getServiceRegistry(Addon addon) throws Exception
   {
      URL resource = addon.getClassLoader().getResource("/META-INF/services/" + SERVICE_REGISTRATION_FILE_NAME);
      Set<Class<?>> serviceTypes = new HashSet<Class<?>>();
      if (resource != null)
      {
         InputStream stream = resource.openStream();
         String services = Streams.toString(stream);
         for (String serviceType : services.split("\n"))
         {
            if (ClassLoaders.containsClass(addon.getClassLoader(), serviceType))
            {
               Class<?> type = ClassLoaders.loadClass(addon.getClassLoader(), serviceType);
               serviceTypes.add(type);
            }
         }

      }
      return new ReflectionServiceRegistry(furnace, addon, serviceTypes);
   }

   @Override
   public void postStartup(Addon addon) throws Exception
   {
      getEventManager(addon).fireEvent(new PostStartup(addon));
   }

   @Override
   public void preShutdown(Addon addon) throws Exception
   {
      getEventManager(addon).fireEvent(new PreShutdown(addon));
   }

   @Override
   public ControlType getControlType()
   {
      return ControlType.SELF;
   }

}
