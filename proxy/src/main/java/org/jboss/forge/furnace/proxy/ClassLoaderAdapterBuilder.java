/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.proxy;

import java.util.Collections;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class ClassLoaderAdapterBuilder implements ClassLoaderAdapterBuilderCallingLoader,
         ClassLoaderAdapterBuilderDelegateLoader, ClassLoaderAdapterBuilderWhitelist
{
   private ClassLoader callingLoader;
   private ClassLoader delegateLoader;
   private Iterable<ClassLoader> whitelist = Collections.emptySet();

   public static ClassLoaderAdapterBuilderCallingLoader callingLoader(ClassLoader callingLoader)
   {
      ClassLoaderAdapterBuilder result = new ClassLoaderAdapterBuilder();
      result.callingLoader = callingLoader;
      return result;
   }

   @Override
   public ClassLoaderAdapterBuilderDelegateLoader delegateLoader(ClassLoader delegateLoader)
   {
      this.delegateLoader = delegateLoader;
      return this;
   }

   @Override
   public ClassLoaderAdapterBuilderWhitelist whitelist(Iterable<ClassLoader> whitelist)
   {
      this.whitelist = whitelist;
      return this;
   }

   @Override
   public <T> T enhance(T delegate)
   {
      return ClassLoaderAdapterCallback.enhance(whitelist, callingLoader, delegateLoader, delegate);
   }

   @Override
   public <T> T enhance(T delegate, Class<?>... types)
   {
      return ClassLoaderAdapterCallback.enhance(whitelist, callingLoader, delegateLoader, delegate, types);
   }

}
