/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.event;

import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.util.Assert;

/**
 * This event is fired by the container when a specific addon has started.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public final class PostStartup
{
   private final Addon addon;

   public PostStartup(Addon addon)
   {
      Assert.notNull(addon, "Addon must not be null.");
      this.addon = addon;
   }

   public Addon getAddon()
   {
      return addon;
   }

   @Override
   public String toString()
   {
      return addon.toString();
   }

}
