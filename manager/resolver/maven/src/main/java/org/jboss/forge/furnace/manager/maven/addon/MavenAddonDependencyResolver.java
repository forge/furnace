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
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.util.artifact.JavaScopes;
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
import org.jboss.forge.furnace.versions.EmptyVersion;
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
   private static final String FURNACE_CONTAINER_GROUP_ID = "org.jboss.forge.furnace.container";
   private static final String FURNACE_API_ARTIFACT_ID = "furnace-api";

   public static final String FORGE_ADDON_CLASSIFIER = "forge-addon";
   private final String classifier;
   private Settings settings;
   private boolean resolveAddonAPIVersions = true;
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
      AddonInfoBuilder builder = AddonInfoBuilder.from(addonId);
      try
      {
         ArtifactDescriptorResult result = readArtifactDescriptor(addonId);
         AddonId furnaceContainerId = null;
         for (Dependency dependency : result.getDependencies())
         {
            String scope = dependency.getScope();
            // Skip test scoped dependencies
            if (JavaScopes.TEST.equals(scope))
               continue;
            Artifact artifact = dependency.getArtifact();
            // Searching for the API version
            if (isFurnaceAPI(artifact))
            {
               SingleVersion apiVersion = new SingleVersion(artifact.getBaseVersion());
               builder.setAPIVersion(apiVersion);
            }
            else if (isAddon(artifact))
            {
               AddonId childId = toAddonId(artifact);
               if (isFurnaceContainer(artifact))
               {
                  furnaceContainerId = childId;
               }
               boolean exported = isExported(scope);
               boolean optional = dependency.isOptional();
               if (optional)
               {
                  builder.addOptionalDependency(childId, exported);
               }
               else
               {
                  builder.addRequiredDependency(childId, exported);
               }
            }
         }
         org.jboss.forge.furnace.versions.Version addonAPIVersion = builder.getAddon().getApiVersion();
         if (resolveAddonAPIVersions && (addonAPIVersion == null || EmptyVersion.getInstance().equals(addonAPIVersion)))
         {
            String apiVersion = null;
            if (furnaceContainerId != null)
            {
               ArtifactDescriptorResult containerDescriptor = readArtifactDescriptor(furnaceContainerId);
               apiVersion = findDependencyVersion(containerDescriptor.getDependencies(), FURNACE_API_GROUP_ID,
                        FURNACE_API_ARTIFACT_ID);
            }
            else
            {
               apiVersion = resolveAPIVersion(addonId).get();
            }
            if (apiVersion != null)
            {
               builder.setAPIVersion(new SingleVersion(apiVersion));
            }
         }
      }
      catch (ArtifactDescriptorException e)
      {
         throw new RuntimeException("Error while retrieving addon information for " + addonId, e);
      }
      return new LazyAddonInfo(this, builder);
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
         if (isFurnaceAPI(artifact) ||
                  (this.classifier.equals(artifact.getClassifier())
                  && !addonId.getName().equals(artifact.getGroupId() + ":" + artifact.getArtifactId())))
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
            if (isAddon(artifact))
            {
               return true;
            }
            return isFurnaceAPI(artifact);
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

   private ArtifactDescriptorResult readArtifactDescriptor(AddonId addonId) throws ArtifactDescriptorException
   {
      String coords = toMavenCoords(addonId);
      RepositorySystem system = container.getRepositorySystem();
      Settings settings = getSettings();
      DefaultRepositorySystemSession session = container.setupRepoSession(system, settings);
      List<RemoteRepository> repositories = MavenRepositories.getRemoteRepositories(container, settings);
      ArtifactDescriptorRequest request = new ArtifactDescriptorRequest();
      request.setArtifact(new DefaultArtifact(coords));
      request.setRepositories(repositories);

      ArtifactDescriptorResult result = system.readArtifactDescriptor(session, request);
      return result;
   }

   private String findDependencyVersion(List<Dependency> dependencies, String groupId, String artifactId)
   {
      for (Dependency child : dependencies)
      {
         Artifact childArtifact = child.getArtifact();

         if (groupId.equals(childArtifact.getGroupId())
                  && artifactId.equals(childArtifact.getArtifactId()))
         {
            return childArtifact.getBaseVersion();
         }
      }
      return null;
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

   private boolean isFurnaceAPI(Artifact artifact)
   {
      return (FURNACE_API_GROUP_ID.equals(artifact.getGroupId()) && FURNACE_API_ARTIFACT_ID.equals(artifact
               .getArtifactId()));
   }

   /**
    * @param scope the scope to be tested upon
    * @return <code>true</code> if the scope indicates an exported dependency
    */
   public static boolean isExported(String scope)
   {
      String artifactScope = Strings.isNullOrEmpty(scope) ? JavaScopes.COMPILE : scope;
      switch (artifactScope)
      {
      case JavaScopes.COMPILE:
      case JavaScopes.RUNTIME:
         return true;
      case JavaScopes.PROVIDED:
      default:
         return false;
      }
   }

   /**
    * Returns if this artifact belongs to a Furnace Container
    */
   private boolean isFurnaceContainer(Artifact artifact)
   {
      return FURNACE_CONTAINER_GROUP_ID.equals(artifact.getGroupId());
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

   /**
    * @param resolveAddonAPIVersions the resolveAddonAPIVersions to set
    */
   public void setResolveAddonAPIVersions(boolean resolveAddonAPIVersions)
   {
      this.resolveAddonAPIVersions = resolveAddonAPIVersions;
   }

   /**
    * @return the resolveAddonAPIVersions
    */
   public boolean isResolveAddonAPIVersions()
   {
      return resolveAddonAPIVersions;
   }
}
