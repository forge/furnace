/**
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package test.org.jboss.forge.furnace.api;

import java.util.List;

import org.jboss.arquillian.container.spi.client.deployment.DeploymentDescription;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.forge.arquillian.AddonDeploymentScenarioEnhancer;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for {@link AddonDeploymentScenarioEnhancer}
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
@RunWith(Arquillian.class)
public class AddonDeploymentEnhancerTest
{
   private static int listenerCalled;

   @Deployment
   public static AddonArchive getDeployment()
   {
      AddonArchive addonArchive = ShrinkWrap.create(AddonArchive.class)
               .addClasses(MockAddonDeploymentGenerator.class)
               .addAsServiceProvider(AddonDeploymentScenarioEnhancer.class, MockAddonDeploymentGenerator.class)
               .addAsLocalServices(AddonDeploymentEnhancerTest.class);
      return addonArchive;
   }

   @Test
   public void testAddonDeploymentEnhancerCalled() throws Exception
   {
      Assert.assertEquals(1, listenerCalled);
   }

   public static class MockAddonDeploymentGenerator implements AddonDeploymentScenarioEnhancer
   {
      @Override
      public List<DeploymentDescription> enhance(TestClass testClass, List<DeploymentDescription> deployments)
      {
         listenerCalled++;
         return deployments;
      }

   }
}
