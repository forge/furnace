/**
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package test.org.jboss.forge.furnace.api;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author <a href="ggastald@redhat.com">George Gastaldi</a>
 */
@RunWith(Arquillian.class)
public class AssumptionViolatedTest
{
   @Deployment
   public static ForgeArchive getDeployment()
   {
      ForgeArchive archive = ShrinkWrap.create(ForgeArchive.class)
               .addAsLocalServices(AssumptionViolatedTest.class);

      return archive;
   }

   @Test
   public void testAssumptionShouldBeSkipped()
   {
      Assume.assumeTrue("If false, display this message", false);
      Assert.fail("This should not be executed");
   }

   @Test
   public void testAssumptionShouldBeSkippedNoMessage()
   {
      Assume.assumeTrue(false);
      Assert.fail("This should not be executed");
   }

   @Test
   public void testAssumptionShouldPass()
   {
      Assume.assumeTrue(true);
      Assert.assertTrue("Should have been true", true);
   }

}
