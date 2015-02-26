/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package test.org.jboss.forge.furnace.repositories;

import java.util.Iterator;
import java.util.Set;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.AddonDependencies;
import org.jboss.forge.arquillian.AddonDependency;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.forge.arquillian.services.LocalServices;
import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.addons.AddonStatus;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import test.org.jboss.forge.furnace.util.FurnaceDisableStrictModeDeploymentListener;
import test.org.jboss.forge.furnace.util.FurnaceVersion_2_14_0_DeploymentListener;
import test.org.jboss.forge.furnace.util.TestRepositoryDeploymentListener;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
@RunWith(Arquillian.class)
public class StrictModeDisabledTest
{
   /*
    * This dependency has no Furnace API version specified.
    */
   private static final String COMPATIBLE = "test:no_dep";
   private static final String COMPATIBLE_VERSION = "1.0.0.Final";

   /*
    * This dependency is incompatible with any Furnace API version.
    */
   private static final String INCOMPATIBLE = "test:one_dep_incompatible_version";
   private static final String INCOMPATIBLE_VERSION = "9999.0.0.Final";

   @Deployment
   @AddonDependencies({
            @AddonDependency(name = INCOMPATIBLE, version = INCOMPATIBLE_VERSION,
                     listener = {
                              TestRepositoryDeploymentListener.class,
                              FurnaceVersion_2_14_0_DeploymentListener.class,
                              FurnaceDisableStrictModeDeploymentListener.class
                     }, timeout = 5000
            ),
            @AddonDependency(name = COMPATIBLE, version = COMPATIBLE_VERSION, listener = TestRepositoryDeploymentListener.class)
   })
   public static AddonArchive getDeployment() throws Exception
   {
      AddonArchive archive = ShrinkWrap.create(AddonArchive.class);
      archive.addAsLocalServices(StrictModeDisabledTest.class);
      return archive;
   }

   @Test
   public void testDependenciesAreCorrectlyDeployedAndAssigned()
   {
      Furnace furnace = LocalServices.getFurnace(getClass().getClassLoader());
      Assert.assertFalse(furnace.isStrictMode());

      Addon self = LocalServices.getAddon(getClass().getClassLoader());

      Set<org.jboss.forge.furnace.addons.AddonDependency> dependencies = self.getDependencies();
      Assert.assertEquals(2, dependencies.size());

      Iterator<org.jboss.forge.furnace.addons.AddonDependency> iterator = dependencies.iterator();

      org.jboss.forge.furnace.addons.AddonDependency incompatibleAddon = iterator.next();
      Assert.assertEquals(AddonId.from(INCOMPATIBLE, INCOMPATIBLE_VERSION), incompatibleAddon.getDependency().getId());
      Assert.assertEquals(AddonStatus.STARTED, incompatibleAddon.getDependency().getStatus());

      org.jboss.forge.furnace.addons.AddonDependency compatibleAddon = iterator.next();
      Assert.assertEquals(AddonId.from(COMPATIBLE, COMPATIBLE_VERSION), compatibleAddon.getDependency().getId());
      Assert.assertEquals(AddonStatus.STARTED, compatibleAddon.getDependency().getStatus());
   }

}
