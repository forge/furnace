/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.arquillian;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.test.spi.client.deployment.AuxiliaryArchiveProcessor;
import org.jboss.arquillian.container.test.spi.client.deployment.DeploymentScenarioGenerator;
import org.jboss.arquillian.container.test.spi.client.protocol.Protocol;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.forge.arquillian.impl.FurnaceAuxiliaryArchiveProcessor;
import org.jboss.forge.arquillian.impl.FurnaceDeploymentScenarioGenerator;
import org.jboss.forge.arquillian.protocol.FurnaceProtocol;

public class FurnaceLoadableExtension implements LoadableExtension
{
   @Override
   public void register(ExtensionBuilder builder)
   {
      builder.service(DeployableContainer.class, FurnaceDeployableContainer.class);
      builder.service(DeploymentScenarioGenerator.class, FurnaceDeploymentScenarioGenerator.class);
      builder.service(Protocol.class, FurnaceProtocol.class);
      builder.service(AuxiliaryArchiveProcessor.class, FurnaceAuxiliaryArchiveProcessor.class);
   }
}
