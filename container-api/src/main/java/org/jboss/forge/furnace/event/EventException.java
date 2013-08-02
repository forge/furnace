/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.event;

import org.jboss.forge.furnace.exception.ContainerException;

/**
 * Thrown when an {@link Exception} is encountered during event processing.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class EventException extends ContainerException
{
   private static final long serialVersionUID = 2990600221242735267L;

   public EventException(String message)
   {
      super(message);
   }

   public EventException(String message, Throwable cause)
   {
      super(message, cause);
   }
}
