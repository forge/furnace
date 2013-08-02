/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.impl.addons;

import java.lang.annotation.Annotation;

import org.jboss.forge.furnace.event.EventException;
import org.jboss.forge.furnace.event.EventManager;

/**
 * Placeholder for addons that do not provide an {@link EventManager}.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class NullEventManager implements EventManager
{
   @Override
   public void fireEvent(Object event, Annotation... qualifiers) throws EventException
   {
      // do nothing
   }
}
