/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.addons;

import java.io.File;
import java.util.Set;
import java.util.concurrent.Future;

import org.jboss.forge.furnace.event.EventManager;
import org.jboss.forge.furnace.repositories.AddonRepository;
import org.jboss.forge.furnace.spi.ServiceRegistry;

/**
 * Represents a node in the {@link Addon} dependency graph.
 * 
 * @see {@link AddonDependency}
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public interface Addon
{
   /**
    * Get the ID of this {@link Addon}.
    */
   AddonId getId();

   /**
    * Get the {@link ClassLoader} containing the resources of this {@link Addon}.
    */
   ClassLoader getClassLoader();

   /**
    * Get the {@link EventManager} of this {@link Addon}.
    */
   EventManager getEventManager();

   /**
    * Get the {@link ServiceRegistry} containing services provided by this {@link Addon}.
    */
   ServiceRegistry getServiceRegistry();

   /**
    * Get the {@link AddonRepository} from which this {@link Addon} was loaded.
    */
   AddonRepository getRepository();

   /**
    * Get the {@link AddonStatus} of this {@link Addon}.
    */
   AddonStatus getStatus();

   /**
    * Get the {@link Set} of {@link AddonDependency} for this {@link Addon} (never <code>null</code>.)
    */
   Set<AddonDependency> getDependencies();

   /**
    * Return the {@link Future} representing the boot-up sequence for this {@link Addon} instance. Returns
    * <code>null</code> if the {@link Addon} is not starting, {@link AddonStatus#isStarted()} or
    * {@link AddonStatus#isFailed()}
    */
   Future<Void> getFuture();

   /**
    * Return the storage area for this addon. Files relevant to the addon execution should be stored here.
    */
   File getStorageDirectory();
}
