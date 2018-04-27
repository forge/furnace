/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.proxy;

import java.lang.reflect.Method;

import javassist.util.proxy.MethodFilter;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 *
 */
class ForgeProxyMethodFilter implements MethodFilter
{
   @Override
   public boolean isHandled(Method method)
   {
      String name = method.getName();
      Class<?>[] parameterTypes = method.getParameterTypes();
      if (!method.getDeclaringClass().getName().contains("java.lang")
               || ("clone".equals(name) && parameterTypes.length == 0)
               || ("close".equals(name) && parameterTypes.length == 0)
               || ("equals".equals(name) && parameterTypes.length == 1)
               || ("hashCode".equals(name) && parameterTypes.length == 0)
               || ("toString".equals(name) && parameterTypes.length == 0))
         return true;
      return false;
   }
}