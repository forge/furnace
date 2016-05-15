/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.repositories;

import java.io.File;
import java.util.Date;

import org.jboss.forge.furnace.addons.Addon;

/**
 * Used to perform {@link Addon} installation/registration operations.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * @author <a href="mailto:koen.aers@gmail.com">Koen Aers</a>
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public interface AddonRepository extends AddonStorageRepository, AddonStateRepository
{

   /**
    * Get the root directory of this {@link AddonRepository}.
    *
    * @deprecated should be storage agnostic. Will be removed in the future.
    */
   @Deprecated
   public File getRootDirectory();

   /**
    * Returns the last modified date of this {@link AddonRepository}.
    *
    * @deprecated use {@link #getVersion()} for dirty checking.
    */
   @Deprecated
   public Date getLastModified();
}
