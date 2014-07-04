/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.classloader.mock.sidewaysproxy;

import java.util.Iterator;

public class ContextValueImpl<PAYLOADTYPE> implements ContextValue<PAYLOADTYPE>
{
   private PAYLOADTYPE payload;

   @Override
   public void set(PAYLOADTYPE payload)
   {
      this.payload = payload;
   }

   @Override
   public PAYLOADTYPE get()
   {
      return payload;
   }

   @Override
   public Iterator<PAYLOADTYPE> iterator()
   {
      return null;
   }
}
