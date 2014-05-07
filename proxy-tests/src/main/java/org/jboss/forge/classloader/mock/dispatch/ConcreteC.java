/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.classloader.mock.dispatch;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class ConcreteC extends AbstractB
{
   public void doSomethingWithPayload()
   {
      Object payload = getPayload();
      if (payload == null)
         throw new IllegalStateException("Payload should not be null!");
   }

   @Override
   public String toString()
   {
      return getPayload() == null ? "" : getPayload().toString();
   }
}
