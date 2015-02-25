/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package test.org.jboss.forge.furnace.repositories;

import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.ShouldThrowException;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.AddonDependencies;
import org.jboss.forge.arquillian.AddonDependency;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import test.org.jboss.forge.furnace.util.TestRepositoryDeploymentListener;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
@RunWith(Arquillian.class)
public class StrictModeTest
{
   /*
    * This dependency has no Furnace API version specified.
    */
   private static final String TEST_NO_DEP = "test:no_dep";

   /*
    * This dependency is incompatible with any Furnace API version.
    */
   private static final String TEST_ONE_DEP_INCOMPATIBLE = "test:one_dep_incompatible_version";

   @Deployment
   @AddonDependencies({
            @AddonDependency(name = TEST_ONE_DEP_INCOMPATIBLE, version = "9999.0.0.Final", listener = TestRepositoryDeploymentListener.class),
            @AddonDependency(name = TEST_NO_DEP, version = "1.0.0.Final", listener = TestRepositoryDeploymentListener.class)
   })
   @ShouldThrowException(DeploymentException.class)
   public static AddonArchive getDeployment() throws Exception
   {
      AddonArchive archive = ShrinkWrap.create(AddonArchive.class);
      archive.addAsLocalServices(StrictModeTest.class);
      return archive;
   }

   @Test
   public void testDependenciesAreCorrectlyDeployedAndAssigned()
   {
      Assert.fail("Test should not be executable.");
   }
}
