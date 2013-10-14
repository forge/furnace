/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.arquillian;

import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.deployment.Deployment;
import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.repositories.MutableAddonRepository;
import org.jboss.shrinkwrap.api.Archive;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public interface DeploymentStrategyType
{
   void beforeTestMethodExecution(Furnace furnace) throws DeploymentException;

   void deploy(Furnace furnace, MutableAddonRepository target, Deployment deployment, Archive<?> archive,
            AddonId addonToDeploy) throws DeploymentException;

   void undeploy(Furnace furnace, MutableAddonRepository target, AddonId addonToUndeploy) throws DeploymentException;
}
