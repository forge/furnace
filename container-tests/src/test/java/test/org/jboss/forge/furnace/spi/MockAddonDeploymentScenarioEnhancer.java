/**
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package test.org.jboss.forge.furnace.spi;

import java.util.List;

import org.jboss.arquillian.container.spi.client.deployment.DeploymentDescription;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.forge.arquillian.spi.AddonDeploymentScenarioEnhancer;

/**
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class MockAddonDeploymentScenarioEnhancer implements AddonDeploymentScenarioEnhancer
{
   private static volatile int calls;

   @Override
   public List<DeploymentDescription> enhance(TestClass testClass, List<DeploymentDescription> deployments)
   {
      calls++;
      return deployments;
   }

   public static int getCalls()
   {
      return calls;
   }

   public static void resetCalls()
   {
      calls = 0;
   }
}
