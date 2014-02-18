/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.proxy;

import java.util.Set;
import java.util.concurrent.Callable;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public interface ClassLoaderAdapterBuilderDelegateLoader extends ClassLoaderAdapterBuilderWhitelist
{
   ClassLoaderAdapterBuilderWhitelist whitelist(Set<ClassLoader> whitelist);

   ClassLoaderAdapterBuilderWhitelist whitelist(Callable<Set<ClassLoader>> whitelist);

}
