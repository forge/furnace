package org.jboss.forge.furnace.impl;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.jboss.forge.furnace.services.ServiceRegistry;

@Singleton
public class ServiceRegistryProducer
{
   private ServiceRegistry registry;

   @Produces
   @Singleton
   public ServiceRegistry produceGlobalAddonRepository()
   {
      return registry;
   }

   public void setServiceRegistry(ServiceRegistry registry)
   {
      this.registry = registry;
   }
}
