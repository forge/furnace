/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.addons;

import java.util.concurrent.Future;

/**
 * The possible states for an {@link Addon}
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public enum AddonStatus
{
   /**
    * When the {@link Addon} is not started yet
    */
   NEW,
   /**
    * When the {@link Addon} is missing its dependencies to load
    */
   MISSING,
   /**
    * When the {@link Addon} is loaded (has assigned a {@link ClassLoader}) but has not been started
    */
   LOADED,
   /**
    * When the {@link Addon} is started and ready to be consumed.
    */
   STARTED,
   /**
    * When the {@link Addon} fails to start. The caught exception is thrown in the {@link Future#get()} method from the
    * {@link Addon#getFuture()} object
    */
   FAILED;

   public boolean isNew()
   {
      return this == NEW;
   }

   public boolean isMissing()
   {
      return this == MISSING;
   }

   public boolean isLoaded()
   {
      return this.ordinal() >= LOADED.ordinal();
   }

   public boolean isFailed()
   {
      return this == FAILED;
   }

   public boolean isStarted()
   {
      return this == STARTED || this == FAILED;
   }

}