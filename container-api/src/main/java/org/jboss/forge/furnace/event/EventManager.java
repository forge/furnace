/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.event;

import java.lang.annotation.Annotation;

/**
 * Responsible for handling event propagation.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public interface EventManager
{
   /**
    * Fire an event and notify observers within the enclosing addon.
    * 
    * @throws EventException if exceptions are encountered during event processing.
    */
   public void fireEvent(Object event, Annotation... qualifiers) throws EventException;
}
