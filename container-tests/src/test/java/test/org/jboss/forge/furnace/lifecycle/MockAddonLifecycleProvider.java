/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package test.org.jboss.forge.furnace.lifecycle;

import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.event.EventManager;
import org.jboss.forge.furnace.lifecycle.AddonLifecycleProvider;
import org.jboss.forge.furnace.lifecycle.ControlType;
import org.jboss.forge.furnace.spi.ServiceRegistry;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class MockAddonLifecycleProvider implements AddonLifecycleProvider
{

   @Override
   public void initialize(Furnace furnace, AddonRegistry registry, Addon self) throws Exception
   {

   }

   @Override
   public void start(Addon addon) throws Exception
   {
   }

   @Override
   public void stop(Addon addon) throws Exception
   {
   }

   @Override
   public ServiceRegistry getServiceRegistry(Addon addon) throws Exception
   {
      return null;
   }

   @Override
   public EventManager getEventManager(Addon addon)
   {
      return null;
   }

   @Override
   public void postStartup(Addon addon) throws Exception
   {

   }

   @Override
   public void preShutdown(Addon addon) throws Exception
   {
   }

   @Override
   public ControlType getControlType()
   {
      return ControlType.ALL;
   }

}
