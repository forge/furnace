/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

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
import org.jboss.forge.furnace.manager.request.InstallRequest;
import org.jboss.forge.furnace.repositories.AddonRepository;
import org.jboss.forge.furnace.repositories.AddonRepositoryMode;

/**
 * Goal which installs addons to a specified directory
 *
 * @author <a href="ggastald@redhat.com">George Gastaldi</a>
 */
@Mojo(name = "addon-install", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, threadSafe = true, requiresProject = false)
public class AddonInstallMojo extends AbstractMojo
{
   /**
    * Addon repository file location
    */
   @Parameter(property = "forge.repository", required = true)
   private File addonRepository;

   /**
    * Addon IDs to install
    */
   @Parameter(property = "forge.addonIds", required = true)
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
    * Skip Addon API version resolution? Default is false
    */
   @Parameter(property = "furnace.addon.api.resolution.skip")
   private boolean skipAddonAPIVersionResolution;

   /**
    * Overwrite addon repositoy Resolve Addon API Versions ? Default is true
    */
   @Parameter(property = "furnace.addon.overwrite", defaultValue = "true")
   private boolean overwrite = true;

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
      Furnace forge = new FurnaceImpl();
      if (!addonRepository.exists())
      {
         addonRepository.mkdirs();
      }
      else if (overwrite)
      {
         try
         {
            deleteDirectory(addonRepository);
            addonRepository.mkdirs();
         }
         catch (IOException e)
         {
            throw new MojoExecutionException("Could not delete " + addonRepository, e);
         }
      }
      AddonRepository repository = forge.addRepository(AddonRepositoryMode.MUTABLE, addonRepository);
      MavenAddonDependencyResolver addonResolver = new MavenAddonDependencyResolver(this.classifier);
      addonResolver.setSettings(settings);
      addonResolver.setResolveAddonAPIVersions(!skipAddonAPIVersionResolution);
      AddonManager addonManager = new AddonManagerImpl(forge, addonResolver);

      for (String addonId : addonIds)
      {
         AddonId id = AddonId.fromCoordinates(addonId);
         InstallRequest install = addonManager.install(id, repository);
         if (!install.getActions().isEmpty())
         {
            getLog().info("" + install);
            install.perform();
         }
      }
   }

   private void deleteDirectory(File addonRepository) throws IOException
   {
      Files.walkFileTree(addonRepository.toPath(), new SimpleFileVisitor<Path>()
      {
         @Override
         public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
         {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
         }

         @Override
         public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
         {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
         }
      });
   }
}
