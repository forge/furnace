/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package test.org.jboss.forge.furnace.harness;

import java.util.Iterator;
import java.util.Set;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.AddonDependencies;
import org.jboss.forge.arquillian.AddonDependency;
import org.jboss.forge.arquillian.AddonDeployment;
import org.jboss.forge.arquillian.AddonDeployments;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.forge.arquillian.services.LocalServices;
import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonFilter;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import test.org.jboss.forge.furnace.util.TestRepositoryDeploymentListener;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
@RunWith(Arquillian.class)
public class AddonDependenciesTest
{
   private static final String TEST_NO_DEP = "test:no_dep";
   private static final String TEST_ONE_DEP = "test:one_dep";
   private static final String TEST_ONE_DEP_A = "test:one_dep_a";
   private static final String TEST_ONE_DEP_LIB = "test:one_dep_lib";

   @Deployment
   @AddonDeployments({
            @AddonDeployment(name = TEST_ONE_DEP_A, version = "1.0.0.Final", listener = TestRepositoryDeploymentListener.class),
            @AddonDeployment(name = TEST_NO_DEP, version = "1.0.0.Final", listener = TestRepositoryDeploymentListener.class, imported = true)
   })
   @AddonDependencies({
            @AddonDependency(name = TEST_ONE_DEP, version = "1.0.0.Final", listener = TestRepositoryDeploymentListener.class),
            @AddonDependency(name = TEST_ONE_DEP_LIB, version = "1.0.0.Final", listener = TestRepositoryDeploymentListener.class, imported = false)
   })
   public static AddonArchive getDeployment() throws Exception
   {
      AddonArchive archive = ShrinkWrap.create(AddonArchive.class);
      archive.addAsLocalServices(AddonDependenciesTest.class);
      return archive;
   }

   @Test
   public void testDependenciesAreCorrectlyDeployedAndAssigned()
   {
      AddonRegistry registry = LocalServices.getFurnace(getClass().getClassLoader()).getAddonRegistry();
      Set<Addon> addons = registry.getAddons(new AddonFilter()
      {
         @Override
         public boolean accept(Addon addon)
         {
            return addon.getId().getName().matches("_DEFAULT_");
         }
      });
      Addon addon = addons.iterator().next();
      Set<org.jboss.forge.furnace.addons.AddonDependency> dependencies = addon.getDependencies();
      Assert.assertEquals(2, dependencies.size());
      Iterator<org.jboss.forge.furnace.addons.AddonDependency> iterator = dependencies.iterator();
      Assert.assertEquals(TEST_NO_DEP, iterator.next().getDependency().getId().getName());
      Assert.assertEquals(TEST_ONE_DEP, iterator.next().getDependency().getId().getName());
   }
}
