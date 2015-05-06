/**
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.impl.event;

import java.lang.annotation.Annotation;
import java.util.concurrent.Callable;

import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonView;
import org.jboss.forge.furnace.event.EventException;
import org.jboss.forge.furnace.event.EventManager;
import org.jboss.forge.furnace.lock.LockManager;
import org.jboss.forge.furnace.lock.LockMode;

/**
 * {@link EventManager} that delegates calls to each started {@link Addon} in the {@link AddonView#getAddons()} set.
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class AddonViewEventManager implements EventManager
{
   private final AddonView addonView;
   private final LockManager lockManager;

   public AddonViewEventManager(AddonView addonView, LockManager lockManager)
   {
      super();
      this.addonView = addonView;
      this.lockManager = lockManager;
   }

   @Override
   public void fireEvent(final Object event, final Annotation... qualifiers) throws EventException
   {
      lockManager.performLocked(LockMode.READ, new Callable<Void>()
      {
         @Override
         public Void call() throws Exception
         {
            for (Addon addon : addonView.getAddons())
            {
               if (addon.getStatus().isStarted())
               {
                  EventManager eventManager = addon.getEventManager();
                  eventManager.fireEvent(event, qualifiers);
               }
            }
            return null;
         }
      });
   }
}
