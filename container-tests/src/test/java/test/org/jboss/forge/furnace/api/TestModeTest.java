/**
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package test.org.jboss.forge.furnace.api;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.forge.arquillian.services.LocalServices;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
@RunWith(Arquillian.class)
public class TestModeTest
{
   @Deployment
   public static AddonArchive getDeployment()
   {
      AddonArchive archive = ShrinkWrap.create(AddonArchive.class)
               .addAsLocalServices(TestModeTest.class);

      return archive;
   }

   @Test
   public void testIsTestModeEnabled()
   {
      Assert.assertTrue(LocalServices.getFurnace(TestModeTest.class.getClassLoader()).isTestMode());
   }
}
