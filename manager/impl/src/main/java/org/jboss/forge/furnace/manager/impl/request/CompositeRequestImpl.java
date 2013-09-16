/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.manager.impl.request;

import java.util.Collections;
import java.util.List;

import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.manager.impl.action.AbstractFurnaceAction;
import org.jboss.forge.furnace.manager.request.AddonActionRequest;
import org.jboss.forge.furnace.manager.request.CompositeAddonActionRequest;
import org.jboss.forge.furnace.manager.request.FurnaceIsolationType;
import org.jboss.forge.furnace.manager.request.InstallRequest;

/**
 * Implementation of the {@link InstallRequest} interface
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 * 
 */
public class CompositeRequestImpl extends AbstractFurnaceAction implements CompositeAddonActionRequest
{
   private final List<AddonActionRequest> actions;

   public CompositeRequestImpl(List<AddonActionRequest> actions, Furnace furnace)
   {
      super(furnace);
      this.actions = Collections.unmodifiableList(actions);
   }

   @Override
   public void execute()
   {
      for (AddonActionRequest action : actions)
      {
         action.perform(FurnaceIsolationType.NONE);
      }
   }

   @Override
   public List<AddonActionRequest> getActions()
   {
      return actions;
   }

   @Override
   public String toString()
   {
      return actions.toString();
   }

}
