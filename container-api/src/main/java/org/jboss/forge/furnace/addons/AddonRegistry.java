/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.addons;

import java.util.Set;

import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.event.PostStartup;
import org.jboss.forge.furnace.event.PreShutdown;
import org.jboss.forge.furnace.services.Imported;

/**
 * Provides methods for registering, starting, stopping, and interacting with registered {@link Addon} instances.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public interface AddonRegistry extends AddonView
{
   /**
    * Return an {@link Imported} instance that can be used to obtain all currently available services of the given
    * {@link Class} type.
    * 
    * @return the {@link Imported} (Never null.)
    */
   <T> Imported<T> getServices(Class<T> clazz);

   /**
    * Return an {@link Imported} instance that can be used to obtain all currently available services with
    * {@link Class#getName()} matching the given name.
    * 
    * @return the {@link Imported} (Never null.)
    */
   <T> Imported<T> getServices(String clazz);

   /**
    * Get a {@link Set} of all currently available service types.
    * 
    * @return the {@link Set} of {@link Class} types (Never null.)
    */
   Set<Class<?>> getExportedTypes();

   /**
    * Get a {@link Set} of currently available service types for which {@link Class#isAssignableFrom(Class)} returns
    * <code>true</code>.
    * 
    * @return the {@link Set} of {@link Class} types (Never null.)
    */
   <T> Set<Class<T>> getExportedTypes(Class<T> type);

   /**
    * Return the current version of this {@link AddonRegistry}, which is incremented on every addon {@link PostStartup}
    * and {@link PreShutdown} event for registered {@link Addon} instances. (Starts at 0 and counts up.)
    */
   long getVersion();

   /**
    * Returns the {@link Furnace} instance this {@link AddonRegistry} belongs to.
    */
   Furnace getFurnace();
}
