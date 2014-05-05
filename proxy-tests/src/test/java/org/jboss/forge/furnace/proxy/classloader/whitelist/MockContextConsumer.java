/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.proxy.classloader.whitelist;

import org.jboss.forge.furnace.proxy.Proxies;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class MockContextConsumer
{
   public void processContext(MockContext context)
   {
      MockContextPayload payload = (MockContextPayload) context.getAttributes().get(
               MockContextPayload.class.getName());

      // this payload should not be proxied with the CLAC, since its interface is available in this classloader.
      if (Proxies.isForgeProxy(payload))
         throw new IllegalStateException("Should not have been a proxy");
   }

   public Class<?> getNativeClass()
   {
      return getClass();
   }

}
