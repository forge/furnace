/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.event;

import org.jboss.forge.furnace.addons.Addon;

/**
 * Fired before the container begins its shutdown process.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public final class PreShutdown
{
   private Addon addon;

   public PreShutdown(Addon addon)
   {
      this.addon = addon;
   }

   public Addon getAddon()
   {
      return addon;
   }
}
