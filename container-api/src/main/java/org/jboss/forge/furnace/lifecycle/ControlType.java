/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.lifecycle;

import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.Addon;

/**
 * Describes the control that an {@link AddonLifecycleProvider} instance will have over various {@link Addon}
 * dependencies in the {@link Furnace} container.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public enum ControlType
{
   /**
    * Controls only the life-cycle of the {@link Addon} from which the {@link AddonLifecycleProvider} instance
    * originated.
    */
   SELF,

   /**
    * Controls only the life-cycle of addons that depend on the {@link Addon} from which the
    * {@link AddonLifecycleProvider} instance originated.
    */
   DEPENDENTS,

   /**
    * Controls both the life-cycle of the {@link Addon}, and the life-cycle of addons that depend on the {@link Addon},
    * from which the {@link AddonLifecycleProvider} instance originated.
    */
   ALL
}
