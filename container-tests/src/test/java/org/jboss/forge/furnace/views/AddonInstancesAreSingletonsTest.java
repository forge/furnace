/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.views;

import java.io.File;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.addon.manager.AddonManager;
import org.jboss.forge.addon.manager.InstallRequest;
import org.jboss.forge.arquillian.AddonDependency;
import org.jboss.forge.arquillian.Dependencies;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.lock.LockMode;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.forge.furnace.repositories.AddonRepository;
import org.jboss.forge.furnace.util.Addons;
import org.jboss.forge.furnace.versions.SingleVersion;
import org.jboss.forge.furnace.versions.SingleVersionRange;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * FIXME This test needs to be refactored to be a bit less brittle. It breaks when addon POMs change.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
@RunWith(Arquillian.class)
public class AddonInstancesAreSingletonsTest
{
   @Deployment
   @Dependencies({
            @AddonDependency(name = "org.jboss.forge.addon:addon-manager", version = "2.0.0-SNAPSHOT"),
            @AddonDependency(name = "org.jboss.forge.addon:maven", version = "2.0.0-SNAPSHOT")
   })
   public static ForgeArchive getDeployment()
   {
      ForgeArchive archive = ShrinkWrap
               .create(ForgeArchive.class)
               .addBeansXML()
               .addAsAddonDependencies(
                        AddonDependencyEntry.create("org.jboss.forge.addon:addon-manager", "2.0.0-SNAPSHOT")
               );

      return archive;
   }

   @Inject
   private AddonRegistry registry;

   @Inject
   private AddonManager addonManager;

   @Inject
   private AddonRepository repository;

   @Inject
   private Furnace furnace;

   @Test
   public void testInstallingAddonWithSingleOptionalAddonDependency() throws InterruptedException, TimeoutException
   {
      int addonCount = registry.getAddons().size();
      final AddonId exampleId = AddonId.fromCoordinates("org.jboss.forge.addon:example,2.0.0-SNAPSHOT");

      /*
       * Ensure that the Addon instance we receive is requested before configuration is rescanned.
       */
      Addon example = furnace.getLockManager().performLocked(LockMode.WRITE, new Callable<Addon>()
      {
         @Override
         public Addon call() throws Exception
         {
            InstallRequest request = addonManager.install(exampleId);
            Assert.assertEquals(0, request.getRequiredAddons().size());
            Assert.assertEquals(1, request.getOptionalAddons().size());
            request.perform();

            Assert.assertTrue(repository.isEnabled(exampleId));
            Assert.assertEquals(2, repository.getAddonResources(exampleId).size());
            Assert.assertTrue(repository.getAddonResources(exampleId).contains(
                     new File(repository.getAddonBaseDir(exampleId), "commons-lang-2.6.jar")));
            Assert.assertTrue(repository.getAddonResources(exampleId).contains(
                     new File(repository.getAddonBaseDir(exampleId), "example-2.0.0-SNAPSHOT-forge-addon.jar")));

            Set<AddonDependencyEntry> dependencies = repository.getAddonDependencies(exampleId);
            Assert.assertEquals(1, dependencies.size());
            AddonDependencyEntry dependency = dependencies.toArray(new AddonDependencyEntry[dependencies.size()])[0];
            Assert.assertEquals("org.jboss.forge.addon:example2", dependency.getName());
            Assert.assertEquals(new SingleVersionRange(new SingleVersion("2.0.0-SNAPSHOT")),
                     dependency.getVersionRange());
            Assert.assertTrue(dependency.isOptional());
            Assert.assertFalse(dependency.isExported());

            Assert.assertTrue(registry.getAddon(AddonId.from("org.jboss.forge.addon:example2", "2.0.0-SNAPSHOT"))
                     .getStatus().isMissing());

            return registry.getAddon(exampleId);
         }
      });
      Addons.waitUntilStarted(example, 10, TimeUnit.SECONDS);
      Assert.assertEquals(addonCount + 1, registry.getAddons().size());
   }

}
