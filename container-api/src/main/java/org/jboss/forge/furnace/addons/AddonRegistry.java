/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.addons;

import java.util.Set;

import org.jboss.forge.furnace.services.Exported;
import org.jboss.forge.furnace.services.Imported;

/**
 * Provides methods for registering, starting, stopping, and interacting with registered {@link Addon} instances.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public interface AddonRegistry extends AddonView
{
   /**
    * Return an {@link Imported} containing the set of currently available {@link Exported} services of the given
    * {@link Class} type. Return an empty {@link Imported} if no matching services are found.
    * 
    * @return the {@link Imported} (Never null.)
    */
   <T> Imported<T> getInstance(Class<T> clazz);

   /**
    * Return an {@link Imported} containing the set of currently available {@link Exported} services with
    * {@link Class#getName()} matching the given name. Return an empty {@link Imported} if no matching services are
    * found.
    * 
    * @return the {@link Imported} (Never null.)
    */
   <T> Imported<T> getInstance(String clazz);

   /**
    * Get a {@link Set} of all currently available {@link Exported} service types.
    * 
    * @return the {@link Set} of {@link Class} types (Never null.)
    */
   Set<Class<?>> getExportedTypes();

   /**
    * Get a {@link Set} of currently available {@link Exported} service types for which
    * {@link Class#isAssignableFrom(Class)} returns <code>true</code>.
    * 
    * @return the {@link Set} of {@link Class} types (Never null.)
    */
   <T> Set<Class<T>> getExportedTypes(Class<T> type);
}
