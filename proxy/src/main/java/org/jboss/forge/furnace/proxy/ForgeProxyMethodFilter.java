/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.proxy;

import java.lang.reflect.Method;

import org.jboss.forge.furnace.proxy.javassist.util.proxy.MethodFilter;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 *
 */
class ForgeProxyMethodFilter implements MethodFilter
{
   @Override
   public boolean isHandled(Method method)
   {
      return true;
   }
}