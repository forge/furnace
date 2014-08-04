/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.arquillian.protocol;

import java.util.Collection;

import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.test.spi.ContainerMethodExecutor;
import org.jboss.arquillian.container.test.spi.client.deployment.DeploymentPackager;
import org.jboss.arquillian.container.test.spi.client.protocol.Protocol;
import org.jboss.arquillian.container.test.spi.command.CommandCallback;
import org.jboss.arquillian.test.spi.TestMethodExecutor;
import org.jboss.arquillian.test.spi.TestResult;
import org.jboss.forge.arquillian.ForgeDeploymentPackager;
import org.jboss.forge.arquillian.ForgeTestMethodExecutor;
import org.jboss.forge.furnace.Furnace;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class ForgeProtocol implements Protocol<ForgeProtocolConfiguration>
{

   @Override
   public Class<ForgeProtocolConfiguration> getProtocolConfigurationClass()
   {
      return ForgeProtocolConfiguration.class;
   }

   @Override
   public ProtocolDescription getDescription()
   {
      return new ForgeProtocolDescription();
   }

   @Override
   public DeploymentPackager getPackager()
   {
      return new ForgeDeploymentPackager();
   }

   @Override
   public ContainerMethodExecutor getExecutor(ForgeProtocolConfiguration protocolConfiguration,
            ProtocolMetaData metaData, CommandCallback callback)
   {
      if (metaData == null)
      {
         return new ContainerMethodExecutor()
         {
            @Override
            public TestResult invoke(TestMethodExecutor arg0)
            {
               return TestResult.skipped(null);
            }
         };
      }

      Collection<FurnaceHolder> contexts = metaData.getContexts(FurnaceHolder.class);
      if (contexts.size() == 0)
      {
         throw new IllegalArgumentException(
                  "No " + Furnace.class.getName() + " found in " + ProtocolMetaData.class.getName() + ". " +
                           "Furnace protocol can not be used");
      }
      return new ForgeTestMethodExecutor(protocolConfiguration, contexts.iterator().next());
   }

}
