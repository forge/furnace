/**
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package test.org.jboss.forge.furnace.spi;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.forge.arquillian.spi.AddonDeploymentScenarioEnhancer;
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
   @Deployment
   public static AddonArchive getDeployment()
   {
      AddonArchive addonArchive = ShrinkWrap.create(AddonArchive.class)
               .addAsLocalServices(AddonDeploymentEnhancerTest.class);
      return addonArchive;
   }

   static
   {
      MockAddonDeploymentScenarioEnhancer.resetCalls();
   }

   @Test
   @RunAsClient
   public void testAddonDeploymentEnhancerCalled() throws Exception
   {
      Assert.assertEquals(1, MockAddonDeploymentScenarioEnhancer.getCalls());
   }
}
