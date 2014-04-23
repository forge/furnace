/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.manager.maven.addon;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.maven.settings.Settings;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.version.Version;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.manager.maven.MavenContainer;
import org.jboss.forge.furnace.manager.maven.result.MavenResponseBuilder;
import org.jboss.forge.furnace.manager.maven.util.MavenRepositories;
import org.jboss.forge.furnace.manager.spi.AddonDependencyResolver;
import org.jboss.forge.furnace.manager.spi.AddonInfo;
import org.jboss.forge.furnace.manager.spi.Response;
import org.jboss.forge.furnace.util.Assert;
import org.jboss.forge.furnace.util.Strings;
import org.jboss.forge.furnace.versions.SingleVersion;
import org.jboss.forge.furnace.versions.Versions;

/**
 * Maven implementation of the {@link AddonDependencyResolver} used by the AddonManager
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 *
 */
public class MavenAddonDependencyResolver implements AddonDependencyResolver
{
   private static final String FURNACE_API_GROUP_ID = "org.jboss.forge.furnace";
   private static final String FURNACE_API_ARTIFACT_ID = "furnace-api";

   public static final String FORGE_ADDON_CLASSIFIER = "forge-addon";
   private final String classifier;
   private Settings settings;
   private final MavenContainer container = new MavenContainer();

   public MavenAddonDependencyResolver()
   {
      classifier = FORGE_ADDON_CLASSIFIER;
   }

   public MavenAddonDependencyResolver(String classifier)
   {
      Assert.notNull(classifier, "Classifier should not be null");
      this.classifier = classifier;
   }

   @Override
   public AddonInfo resolveAddonDependencyHierarchy(AddonId addonId)
   {
      String coords = toMavenCoords(addonId);
      RepositorySystem system = container.getRepositorySystem();
      Settings settings = getSettings();
      DefaultRepositorySystemSession session = container.setupRepoSession(system, settings);

      DependencyNode dependencyNode = traverseAddonGraph(coords, system, settings, session);
      return fromNode(addonId, dependencyNode, system, settings, session);
   }

   @Override
   public Response<File[]> resolveResources(final AddonId addonId)
   {
      RepositorySystem system = container.getRepositorySystem();
      Settings settings = getSettings();
      DefaultRepositorySystemSession session = container.setupRepoSession(system, settings);
      final String mavenCoords = toMavenCoords(addonId);
      Artifact queryArtifact = new DefaultArtifact(mavenCoords);
      session.setDependencyTraverser(new AddonDependencyTraverser(classifier));
      session.setDependencySelector(new AddonDependencySelector(classifier));
      Dependency dependency = new Dependency(queryArtifact, null);

      List<RemoteRepository> repositories = MavenRepositories.getRemoteRepositories(container, settings);

      CollectRequest collectRequest = new CollectRequest(dependency, repositories);
      DependencyResult result;
      try
      {
         result = system.resolveDependencies(session, new DependencyRequest(collectRequest, null));
      }
      catch (DependencyResolutionException e)
      {
         throw new RuntimeException(e);
      }
      List<Exception> collectExceptions = result.getCollectExceptions();
      Set<File> files = new HashSet<File>();
      List<ArtifactResult> artifactResults = result.getArtifactResults();
      for (ArtifactResult artifactResult : artifactResults)
      {
         Artifact artifact = artifactResult.getArtifact();
         if (this.classifier.equals(artifact.getClassifier())
                  && !mavenCoords.equals(artifact.toString()))
         {
            continue;
         }
         files.add(artifact.getFile());
      }
      return new MavenResponseBuilder<File[]>(files.toArray(new File[files.size()])).setExceptions(collectExceptions);
   }

   @Override
   public Response<AddonId[]> resolveVersions(final String addonName)
   {
      String addonNameSplit;
      String version;

      String[] split = addonName.split(",");
      if (split.length == 2)
      {
         addonNameSplit = split[0];
         version = split[1];
      }
      else
      {
         addonNameSplit = addonName;
         version = null;
      }
      RepositorySystem system = container.getRepositorySystem();
      Settings settings = getSettings();
      DefaultRepositorySystemSession session = container.setupRepoSession(system, settings);
      List<RemoteRepository> repositories = MavenRepositories.getRemoteRepositories(container, settings);
      VersionRangeResult versions = getVersions(system, settings, session, repositories, addonNameSplit, version);
      List<Exception> exceptions = versions.getExceptions();
      List<Version> versionsList = versions.getVersions();
      List<AddonId> addons = new ArrayList<AddonId>();
      List<AddonId> snapshots = new ArrayList<AddonId>();
      for (Version artifactVersion : versionsList)
      {
         AddonId addonId = AddonId.from(addonName, artifactVersion.toString());
         if (Versions.isSnapshot(addonId.getVersion()))
         {
            snapshots.add(addonId);
         }
         else
         {
            addons.add(addonId);
         }
      }
      if (addons.isEmpty())
      {
         addons = snapshots;
      }
      return new MavenResponseBuilder<AddonId[]>(addons.toArray(new AddonId[addons.size()])).setExceptions(exceptions);
   }

   @Override
   public Response<String> resolveAPIVersion(AddonId addonId)
   {
      RepositorySystem system = container.getRepositorySystem();
      Settings settings = getSettings();
      DefaultRepositorySystemSession session = container.setupRepoSession(system, settings);
      return resolveAPIVersion(addonId, system, settings, session);
   }

   private Response<String> resolveAPIVersion(AddonId addonId, RepositorySystem system, Settings settings,
            DefaultRepositorySystemSession session)
   {
      List<RemoteRepository> repositories = MavenRepositories.getRemoteRepositories(container, settings);
      String mavenCoords = toMavenCoords(addonId);
      Artifact queryArtifact = new DefaultArtifact(mavenCoords);

      session.setDependencyTraverser(new AddonDependencyTraverser(classifier));
      session.setDependencySelector(new DependencySelector()
      {

         @Override
         public boolean selectDependency(Dependency dependency)
         {
            Artifact artifact = dependency.getArtifact();
            if (classifier.equals(artifact.getClassifier()))
            {
               return true;
            }
            return (FURNACE_API_GROUP_ID.equals(artifact.getGroupId()) && FURNACE_API_ARTIFACT_ID.equals(artifact
                     .getArtifactId()));
         }

         @Override
         public DependencySelector deriveChildSelector(DependencyCollectionContext context)
         {
            return this;
         }
      });
      CollectRequest request = new CollectRequest(new Dependency(queryArtifact, null), repositories);
      CollectResult result;
      try
      {
         result = system.collectDependencies(session, request);
      }
      catch (DependencyCollectionException e)
      {
         throw new RuntimeException(e);
      }
      List<Exception> exceptions = result.getExceptions();
      String apiVersion = findVersion(result.getRoot().getChildren(), FURNACE_API_GROUP_ID, FURNACE_API_ARTIFACT_ID);
      return new MavenResponseBuilder<String>(apiVersion).setExceptions(exceptions);
   }

   private String findVersion(List<DependencyNode> dependencies, String groupId, String artifactId)
   {
      for (DependencyNode child : dependencies)
      {
         Artifact childArtifact = child.getArtifact();

         if (groupId.equals(childArtifact.getGroupId())
                  && artifactId.equals(childArtifact.getArtifactId()))
         {
            return childArtifact.getBaseVersion();
         }
         else
         {
            String version = findVersion(child.getChildren(), groupId, artifactId);
            if (version != null)
            {
               return version;
            }
         }
      }
      return null;
   }

   private VersionRangeResult getVersions(RepositorySystem system, Settings settings, RepositorySystemSession session,
            List<RemoteRepository> repositories,
            String addonName,
            String version)
   {
      try
      {
         String[] split = addonName.split(",");
         if (split.length == 2)
         {
            version = split[1];
         }
         if (version == null || version.isEmpty())
         {
            version = "[,)";
         }
         else if (!version.matches("(\\(|\\[).*?(\\)|\\])"))
         {
            version = "[" + version + "]";
         }

         Artifact artifact = new DefaultArtifact(toMavenCoords(AddonId.from(addonName, version)));
         VersionRangeRequest rangeRequest = new VersionRangeRequest(artifact, repositories, null);
         VersionRangeResult rangeResult = system.resolveVersionRange(session, rangeRequest);
         return rangeResult;
      }
      catch (Exception e)
      {
         throw new RuntimeException("Failed to look up versions for [" + addonName + "]", e);
      }
   }

   private AddonInfo fromNode(AddonId id, DependencyNode dependencyNode, RepositorySystem system, Settings settings,
            DefaultRepositorySystemSession session)
   {
      AddonInfoBuilder builder = AddonInfoBuilder.from(enrichAddonId(id, system, settings, session));
      List<DependencyNode> children = dependencyNode.getChildren();
      for (DependencyNode child : children)
      {
         Dependency dependency = child.getDependency();
         Artifact artifact = dependency.getArtifact();
         if (isAddon(artifact))
         {
            AddonId childId = toAddonId(artifact);
            boolean exported = false;
            boolean optional = dependency.isOptional();
            String scope = dependency.getScope();
            if (scope != null && !optional)
            {
               if ("compile".equalsIgnoreCase(scope) || "runtime".equalsIgnoreCase(scope))
                  exported = true;
               else if ("provided".equalsIgnoreCase(scope))
                  exported = false;
            }
            DependencyNode node = traverseAddonGraph(toMavenCoords(childId), system, settings, session);
            AddonInfo addonInfo = fromNode(childId, node, system, settings, session);
            if (optional)
            {
               builder.addOptionalDependency(addonInfo, exported);
            }
            else
            {
               builder.addRequiredDependency(addonInfo, exported);
            }
         }
      }
      return new LazyAddonInfo(this, builder);
   }

   private DependencyNode traverseAddonGraph(String coords, RepositorySystem system, Settings settings,
            DefaultRepositorySystemSession session)
   {
      session.setDependencyTraverser(new AddonDependencyTraverser(this.classifier));
      session.setDependencySelector(new AddonDependencySelector(this.classifier));
      Artifact queryArtifact = new DefaultArtifact(coords);

      List<RemoteRepository> repositories = MavenRepositories.getRemoteRepositories(container, settings);
      CollectRequest collectRequest = new CollectRequest(new Dependency(queryArtifact, null), repositories);

      CollectResult result;
      try
      {
         result = system.collectDependencies(session, collectRequest);
      }
      catch (DependencyCollectionException e)
      {
         throw new RuntimeException(e);
      }
      return result.getRoot();
   }

   private String toMavenCoords(AddonId addonId)
   {
      String coords = addonId.getName() + ":jar:" + this.classifier + ":" + addonId.getVersion();
      return coords;
   }

   private boolean isAddon(Artifact artifact)
   {
      return this.classifier.equals(artifact.getClassifier());
   }

   private AddonId toAddonId(Artifact artifact)
   {
      if (isAddon(artifact))
      {
         return AddonId.from(artifact.getGroupId() + ":" + artifact.getArtifactId(), artifact.getBaseVersion());
      }
      throw new IllegalArgumentException("Not a forge-addon: " + artifact);
   }

   /**
    * Adds the API version to the supplied {@link AddonId}
    */
   private AddonId enrichAddonId(AddonId originalAddonId, RepositorySystem system, Settings settings,
            DefaultRepositorySystemSession session)
   {
      AddonId id;
      // FORGE-1769: Add API version to requested AddonID
      if (Strings.isNullOrEmpty(Objects.toString(originalAddonId.getApiVersion(), null)))
      {
         String apiVersion = resolveAPIVersion(originalAddonId, system, settings, session).get();

         if (Strings.isNullOrEmpty(apiVersion))
         {
            id = originalAddonId;
         }
         else
         {
            id = AddonId.from(originalAddonId.getName(), originalAddonId.getVersion(), new SingleVersion(apiVersion));
         }
      }
      else
      {
         id = originalAddonId;
      }
      return id;
   }

   /**
    * @param settings the settings to set
    */
   public void setSettings(Settings settings)
   {
      this.settings = settings;
   }

   /**
    * @return the settings
    */
   public Settings getSettings()
   {
      return settings == null ? container.getSettings() : settings;
   }
}
