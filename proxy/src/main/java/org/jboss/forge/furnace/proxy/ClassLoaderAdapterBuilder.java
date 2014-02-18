/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.proxy;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import org.jboss.forge.furnace.util.Callables;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class ClassLoaderAdapterBuilder implements ClassLoaderAdapterBuilderCallingLoader,
         ClassLoaderAdapterBuilderDelegateLoader, ClassLoaderAdapterBuilderWhitelist
{
   private ClassLoader callingLoader;
   private ClassLoader delegateLoader;
   private Callable<Set<ClassLoader>> whitelist = Callables.returning((Set<ClassLoader>) new HashSet<ClassLoader>());

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
   public ClassLoaderAdapterBuilderWhitelist whitelist(Set<ClassLoader> whitelist)
   {
      this.whitelist = Callables.returning(whitelist);
      return this;
   }

   @Override
   public ClassLoaderAdapterBuilderWhitelist whitelist(Callable<Set<ClassLoader>> whitelist)
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
