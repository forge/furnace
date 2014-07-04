/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.classloader.mock.sidewaysproxy;

public class Action1 implements Action
{

   @Override
   public void handle(Context context)
   {
      Object payload = context.get().get();
      System.out.println(payload);
   }

}
