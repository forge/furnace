/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.manager.impl.request;

import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.manager.impl.action.AbstractFurnaceAction;
import org.jboss.forge.furnace.manager.request.DeployRequest;
import org.jboss.forge.furnace.manager.request.FurnaceIsolationType;
import org.jboss.forge.furnace.manager.request.RemoveRequest;
import org.jboss.forge.furnace.manager.request.UpdateRequest;
import org.jboss.forge.furnace.manager.spi.AddonInfo;

/**
 * An update consists in a two-step process: Remove the original addon and install the new one
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 * 
 */
public class UpdateRequestImpl extends AbstractFurnaceAction implements UpdateRequest
{
   private final RemoveRequest removeRequest;
   private final DeployRequest deployRequest;

   public UpdateRequestImpl(RemoveRequest removeRequest, DeployRequest deployRequest, Furnace furnace)
   {
      super(furnace);
      this.removeRequest = removeRequest;
      this.deployRequest = deployRequest;
   }

   @Override
   public AddonInfo getRequestedAddonInfo()
   {
      return deployRequest.getRequestedAddonInfo();
   }

   @Override
   public DeployRequest getDeployRequest()
   {
      return deployRequest;
   }

   @Override
   public RemoveRequest getRemoveRequest()
   {
      return removeRequest;
   }

   @Override
   public void execute()
   {
      removeRequest.perform(FurnaceIsolationType.CONFIGURATION_RELOAD);
      deployRequest.perform(FurnaceIsolationType.CONFIGURATION_RELOAD);
   }

   @Override
   public String toString()
   {
      AddonId oldAddon = removeRequest.getRequestedAddonInfo().getAddon();
      AddonId newAddon = deployRequest.getRequestedAddonInfo().getAddon();
      if (oldAddon.getVersion().equals(newAddon.getVersion()))
      {
         return "Update: [" + newAddon + "]";
      }
      else
      {
         return "Update: from [" + oldAddon + "] to [" + newAddon + "]";
      }
   }
}
