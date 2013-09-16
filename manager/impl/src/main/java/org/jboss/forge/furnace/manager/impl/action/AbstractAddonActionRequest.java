/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.manager.impl.action;

import java.util.logging.Logger;

import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.manager.request.AddonActionRequest;
import org.jboss.forge.furnace.manager.spi.AddonInfo;
import org.jboss.forge.furnace.repositories.MutableAddonRepository;
import org.jboss.forge.furnace.util.Assert;

/**
 * Abstract class for {@link AddonActionRequest} implementations
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public abstract class AbstractAddonActionRequest extends AbstractFurnaceAction implements AddonActionRequest
{
   protected final AddonInfo addonInfo;
   protected final MutableAddonRepository repository;

   protected Logger log = Logger.getLogger(getClass().getName());

   protected AbstractAddonActionRequest(AddonInfo addonInfo, MutableAddonRepository addonRepository, Furnace furnace)
   {
      super(furnace);
      Assert.notNull(addonInfo, "AddonInfo must not be null.");
      Assert.notNull(furnace, "Addon Repository must not be null.");

      this.addonInfo = addonInfo;
      this.repository = addonRepository;
   }

   @Override
   public final AddonInfo getRequestedAddonInfo()
   {
      return addonInfo;
   }

   @Override
   public String toString()
   {
      return getClass().getSimpleName() + ":[" + getRequestedAddonInfo() + "]";
   }

}
