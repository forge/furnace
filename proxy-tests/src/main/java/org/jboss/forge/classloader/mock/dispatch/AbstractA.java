/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.classloader.mock.dispatch;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
abstract class AbstractA
{
   private Object payload;

   public Object getPayload()
   {
      return payload;
   }

   public void setPayload(Object payload)
   {
      this.payload = payload;
   }
}
