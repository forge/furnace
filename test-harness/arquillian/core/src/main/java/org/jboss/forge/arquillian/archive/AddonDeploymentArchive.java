/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.arquillian.archive;

import java.util.List;

import org.jboss.forge.arquillian.AddonDeployment;
import org.jboss.forge.arquillian.DeploymentListener;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.shrinkwrap.api.Archive;

/**
 * Archive representing a Furnace {@link AddonDeployment} deployment.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public interface AddonDeploymentArchive extends Archive<AddonDeploymentArchive>,
         RepositoryLocationAware<AddonDeploymentArchive>, DeploymentTimeoutAware<AddonDeploymentArchive>
{
   /**
    * Get the {@link AddonId} of this {@link AddonDeploymentArchive}.
    */
   AddonId getAddonId();

   /**
    * Set the {@link AddonId} of this {@link AddonDeploymentArchive}.
    */
   AddonDeploymentArchive setAddonId(AddonId id);

   /**
    * Get the {@link DeploymentListener} instances for this {@link AddonDeploymentArchive}
    */
   List<DeploymentListener> getDeploymentListeners();

   /**
    * Set the {@link DeploymentListener} for this {@link AddonDeploymentArchive}
    */
   void addDeploymentListener(DeploymentListener listener);
}
