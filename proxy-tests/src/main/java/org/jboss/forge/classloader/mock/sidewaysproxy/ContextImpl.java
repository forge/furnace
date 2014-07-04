/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.classloader.mock.sidewaysproxy;

public class ContextImpl implements Context
{
   private ContextValue<Payload> payload;

   @Override
   public void set(ContextValue<Payload> payload)
   {
      this.payload = payload;
   }

   @Override
   public ContextValue<Payload> get()
   {
      return payload;
   }

}
