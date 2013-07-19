/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.manager.spi;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * Creates a new {@link AddonDependencyResolver} based on Sun's Service Provider
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 * 
 */
public class AddonDependencyResolverFactory
{

   public static AddonDependencyResolver createResolver()
   {
      Iterator<AddonDependencyResolver> resolvers = ServiceLoader.load(AddonDependencyResolver.class,
               Thread.currentThread().getContextClassLoader()).iterator();
      if (!resolvers.hasNext())
      {
         throw new IllegalStateException("No Addon Resolver found");
      }
      return resolvers.next();
   }

}
