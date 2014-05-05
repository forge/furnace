/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.proxy.classloader.whitelist;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class MockContext
{
   private final Map<Object, Object> attributes = new HashMap<>();

   public Map<Object, Object> getAttributes()
   {
      return attributes;
   }
}
