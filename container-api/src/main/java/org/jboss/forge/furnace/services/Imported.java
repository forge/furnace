/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.services;

/**
 * Represents a handle to one or more {@link Exported} services.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public interface Imported<T> extends Iterable<T>
{
   /**
    * Get a fully constructed instance of the underlying type.
    */
   T get();

   /**
    * Signal that the given instance may be released.
    */
   void release(T instance);

   /**
    * Get a fully constructed instance of the exact requested type.
    */
   T selectExact(Class<T> type);

   /**
    * Returns <code>true</code> if an instance of the requested type can be produced.
    */
   boolean isSatisfied();

   /**
    * Returns <code>true</code> if multiple instances of the requested type can be produced.
    */
   boolean isAmbiguous();
}
