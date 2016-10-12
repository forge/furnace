/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.maven.plugin;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Settings;
import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.impl.FurnaceImpl;
import org.jboss.forge.furnace.manager.AddonManager;
import org.jboss.forge.furnace.manager.impl.AddonManagerImpl;
import org.jboss.forge.furnace.manager.maven.addon.MavenAddonDependencyResolver;
import org.jboss.forge.furnace.manager.request.RemoveRequest;
import org.jboss.forge.furnace.repositories.AddonRepository;
import org.jboss.forge.furnace.repositories.AddonRepositoryMode;

/**
 * Goal which removes addons from a specified directory
 *
 * @author <a href="ggastald@redhat.com">George Gastaldi</a>
 */
@Mojo(name = "addon-remove", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, threadSafe = true, requiresProject = false)
public class AddonRemoveMojo extends AbstractMojo
{
   /**
    * Addon repository file location
    */
   @Parameter(property = "furnace.repository", required = true)
   private File addonRepository;

   /**
    * Addon IDs to install
    */
   @Parameter(property = "furnace.addonIds", required = true)
   private String[] addonIds;

   /**
    * Classifier used for addon resolution (default is forge-addon)
    */
   @Parameter(defaultValue = "forge-addon")
   private String classifier;

   /**
    * The current settings
    */
   @Parameter(defaultValue = "${settings}", required = true, readonly = true)
   private Settings settings;

   /**
    * Skip this execution ?
    */
   @Parameter(property = "furnace.addon.skip")
   private boolean skip;

   @Override
   public void execute() throws MojoExecutionException, MojoFailureException
   {
      if (skip)
      {
         getLog().info("Execution skipped.");
         return;
      }
      if (!addonRepository.exists())
      {
         throw new MojoExecutionException("Addon Repository " + addonRepository.getAbsolutePath() + " does not exist.");
      }
      Furnace forge = new FurnaceImpl();
      AddonRepository repository = forge.addRepository(AddonRepositoryMode.MUTABLE, addonRepository);
      MavenAddonDependencyResolver addonResolver = new MavenAddonDependencyResolver(this.classifier);
      addonResolver.setSettings(settings);
      AddonManager addonManager = new AddonManagerImpl(forge, addonResolver);
      for (String addonId : addonIds)
      {
         AddonId id = AddonId.fromCoordinates(addonId);
         RemoveRequest request = addonManager.remove(id, repository);
         getLog().info("" + request);
         request.perform();
      }
   }
}
