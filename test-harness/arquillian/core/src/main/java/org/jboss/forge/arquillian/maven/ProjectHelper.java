/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.arquillian.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestPopulator;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingResult;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.eclipse.aether.util.repository.DefaultProxySelector;
import org.jboss.forge.furnace.manager.maven.MavenContainer;
import org.jboss.forge.furnace.manager.maven.addon.MavenAddonDependencyResolver;
import org.jboss.forge.furnace.manager.maven.util.MavenRepositories;

public class ProjectHelper
{
   private final MavenContainer mavenContainer;
   private ProjectBuildingRequest request;

   public ProjectHelper()
   {
      this.mavenContainer = new MavenContainer();
   }

   public Model loadPomFromFile(File pomFile, String... profiles)
   {
      RepositorySystem system = mavenContainer.getRepositorySystem();
      Settings settings = mavenContainer.getSettings();
      DefaultRepositorySystemSession session = mavenContainer.setupRepoSession(system, settings);
      final DefaultModelBuildingRequest request = new DefaultModelBuildingRequest()
               .setSystemProperties(System.getProperties())
               .setPomFile(pomFile)
               .setActiveProfileIds(settings.getActiveProfiles());
      ModelBuilder builder = new DefaultModelBuilderFactory().newInstance();
      ModelBuildingResult result;
      try
      {
         request.setModelResolver(new MavenModelResolver(system, session,
                  MavenRepositories.getRemoteRepositories(mavenContainer, settings)));
         result = builder.build(request);
      }
      // wrap exception message
      catch (ModelBuildingException e)
      {
         String pomPath = request.getPomFile().getAbsolutePath();
         StringBuilder sb = new StringBuilder("Found ").append(e.getProblems().size())
                  .append(" problems while building POM model from ").append(pomPath).append("\n");

         int counter = 1;
         for (ModelProblem problem : e.getProblems())
         {
            sb.append(counter++).append("/ ").append(problem).append("\n");
         }

         throw new RuntimeException(sb.toString());
      }
      return result.getEffectiveModel();
   }

   public List<Dependency> resolveDependenciesFromPOM(File pomFile) throws Exception
   {
      PlexusContainer plexus = new PlexusContainer();
      List<Dependency> result;
      try
      {
         ProjectBuildingRequest request = getBuildingRequest(plexus);
         request.setResolveDependencies(true);
         ProjectBuilder builder = plexus.lookup(ProjectBuilder.class);
         ProjectBuildingResult build = builder.build(pomFile, request);
         result = build.getDependencyResolutionResult().getDependencies();
      }
      finally
      {
         plexus.shutdown();
      }
      return result;
   }

   private ProjectBuildingRequest getBuildingRequest(PlexusContainer plexus)
   {
      if (this.request == null)
      {
         ClassLoader cl = Thread.currentThread().getContextClassLoader();
         try
         {
            Settings settings = mavenContainer.getSettings();
            // TODO this needs to be configurable via .forge
            // TODO this reference to the M2_REPO should probably be centralized
            MavenExecutionRequest executionRequest = new DefaultMavenExecutionRequest();

            RepositorySystem repositorySystem = plexus.lookup(RepositorySystem.class);
            MavenExecutionRequestPopulator requestPopulator = plexus.lookup(MavenExecutionRequestPopulator.class);

            requestPopulator.populateFromSettings(executionRequest, settings);
            requestPopulator.populateDefaults(executionRequest);

            ProjectBuildingRequest request = executionRequest.getProjectBuildingRequest();

            org.apache.maven.artifact.repository.ArtifactRepository localRepository = RepositoryUtils
                     .toArtifactRepository("local",
                              new File(settings.getLocalRepository()).toURI().toURL().toString(), null, true, true);
            request.setLocalRepository(localRepository);

            List<org.apache.maven.artifact.repository.ArtifactRepository> settingsRepos = new ArrayList<>(
                     request.getRemoteRepositories());
            List<String> activeProfiles = settings.getActiveProfiles();

            Map<String, Profile> profiles = settings.getProfilesAsMap();

            for (String id : activeProfiles)
            {
               Profile profile = profiles.get(id);
               if (profile != null)
               {
                  List<Repository> repositories = profile.getRepositories();
                  for (Repository repository : repositories)
                  {
                     settingsRepos.add(RepositoryUtils.convertFromMavenSettingsRepository(repository));
                  }
               }
            }
            request.setRemoteRepositories(settingsRepos);
            request.setSystemProperties(System.getProperties());

            DefaultRepositorySystemSession repositorySession = MavenRepositorySystemUtils.newSession();
            Proxy activeProxy = settings.getActiveProxy();
            if (activeProxy != null)
            {
               DefaultProxySelector dps = new DefaultProxySelector();
               dps.add(RepositoryUtils.convertFromMavenProxy(activeProxy), activeProxy.getNonProxyHosts());
               repositorySession.setProxySelector(dps);
            }
            LocalRepository localRepo = new LocalRepository(settings.getLocalRepository());

            repositorySession.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(repositorySession,
                     localRepo));
            repositorySession.setOffline(settings.isOffline());
            List<Mirror> mirrors = executionRequest.getMirrors();
            DefaultMirrorSelector mirrorSelector = new DefaultMirrorSelector();
            if (mirrors != null)
            {
               for (Mirror mirror : mirrors)
               {
                  mirrorSelector.add(mirror.getId(), mirror.getUrl(), mirror.getLayout(), false, mirror.getMirrorOf(),
                           mirror.getMirrorOfLayouts());
               }
            }
            repositorySession.setMirrorSelector(mirrorSelector);

            LazyAuthenticationSelector authSelector = new LazyAuthenticationSelector(mirrorSelector);
            for (Server server : settings.getServers())
            {
               authSelector.add(
                        server.getId(),
                        new AuthenticationBuilder().addUsername(server.getUsername()).addPassword(server.getPassword())
                                 .addPrivateKey(server.getPrivateKey(), server.getPassphrase()).build());
            }
            repositorySession.setAuthenticationSelector(authSelector);

            request.setRepositorySession(repositorySession);
            request.setProcessPlugins(false);
            request.setResolveDependencies(false);
            this.request = request;
         }
         catch (RuntimeException e)
         {
            throw e;
         }
         catch (Exception e)
         {
            throw new RuntimeException(
                     "Could not create Maven project building request", e);
         }
         finally
         {
            /*
             * We reset the classloader to prevent potential modules bugs if Classwords container changes classloaders
             * on us
             */
            Thread.currentThread().setContextClassLoader(cl);
         }
      }
      return request;
   }

   /**
    * Returns <code>true</code> if this model is a single-project addon
    */
   public boolean isAddon(Model model)
   {
      boolean result = false;
      Build build = model.getBuild();
      if (build != null)
      {
         PLUGIN_LOOP: for (Plugin plugin : build.getPlugins())
         {
            if ("maven-jar-plugin".equals(plugin.getArtifactId()))
            {
               for (PluginExecution execution : plugin.getExecutions())
               {
                  Xpp3Dom config = (Xpp3Dom) execution.getConfiguration();
                  if (config != null)
                  {
                     Xpp3Dom classifierNode = config.getChild("classifier");
                     if (classifierNode != null
                              && MavenAddonDependencyResolver.FORGE_ADDON_CLASSIFIER.equals(classifierNode.getValue()))
                     {
                        result = true;
                        break PLUGIN_LOOP;
                     }
                  }
               }
            }
         }
      }
      return result;
   }

}
