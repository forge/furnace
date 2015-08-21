/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.spi;

import org.jboss.forge.furnace.addons.Addon;

/**
 * An {@link ExportedInstance} object is a service returned from {@link ServiceRegistry}
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public interface ExportedInstance<T>
{
   /**
    * @return the instance associated with this {@link ExportedInstance}
    */
   T get();

   /**
    * Releases this {@link ExportedInstance}. Subsequent calls to {@link ExportedInstance#get()} may present a different
    * behavior
    * 
    * @param instance the instance returned in this{@link ExportedInstance#get()}
    */
   void release(T instance);

   /**
    * The type associated with this {@link ExportedInstance}
    * 
    * @return {@link Class} associated with this object
    */
   Class<? extends T> getActualType();

   /**
    * The addon that created this {@link ExportedInstance}
    * 
    * @return the {@link Addon} instance that created this object
    */
   Addon getSourceAddon();
}
