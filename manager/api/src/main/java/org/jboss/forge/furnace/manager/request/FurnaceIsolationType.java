/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.manager.request;

import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.manager.AddonManager;

/**
 * Specifies the isolation types that an {@link AddonManager} should use during {@link FurnaceAction} execution.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public enum FurnaceIsolationType
{
   /**
    * Does not require any isolation; the operation will be performed without waiting.
    */
   NONE,
   /**
    * Requires that {@link Furnace} rescan its configuration at least once after the operation has been performed.
    */
   CONFIGURATION_RELOAD;
}
