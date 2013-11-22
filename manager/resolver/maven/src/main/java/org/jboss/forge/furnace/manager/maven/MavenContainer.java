/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.manager.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.Activation;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.eclipse.aether.DefaultRepositoryCache;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.connector.wagon.WagonProvider;
import org.eclipse.aether.connector.wagon.WagonRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.eclipse.aether.util.repository.DefaultProxySelector;
import org.eclipse.aether.util.repository.SimpleResolutionErrorPolicy;

/**
 * Configures the Maven API for usage inside Furnace
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class MavenContainer
{
   private static final String M2_HOME = System.getenv().get("M2_HOME");

   /**
    * Sets an alternate location to Maven user settings.xml configuration
    */
   public static final String ALT_USER_SETTINGS_XML_LOCATION = "org.apache.maven.user-settings";

   /**
    * Sets an alternate location of Maven global settings.xml configuration
    */
   public static final String ALT_GLOBAL_SETTINGS_XML_LOCATION = "org.apache.maven.global-settings";

   /**
    * Sets an alternate location of Maven local repository
    */
   public static final String ALT_LOCAL_REPOSITORY_LOCATION = "maven.repo.local";

   /**
    * Get a list of all {@link RemoteRepository} instances from the Maven settings.
    * The returned list can be used as is in a CollectRequest or a VersionRangeRequest,
    * since all repositories and mirrors have been enriched with the required
    * authentication info.
    * 
    * @param settings The Maven settings instance.
    * @return A list of {@link RemoteRepository} instances
    */
   public List<RemoteRepository> getEnabledRepositoriesFromProfile(Settings settings)
   {
      List<RemoteRepository> settingsRepos = new ArrayList<RemoteRepository>();
      List<String> activeProfiles = settings.getActiveProfiles();
      
      // "Active by default" profiles must be added separately, since they are not recognized as active ones
      for (Profile profile : settings.getProfiles())
      {
         Activation activation = profile.getActivation();
         if (activation != null && activation.isActiveByDefault())
         {
            activeProfiles.add(profile.getId());
         }
      }

      Map<String, Profile> profiles = settings.getProfilesAsMap();

      // Collect all repositories declared in all active profiles
      for (String id : activeProfiles)
      {
         Profile profile = profiles.get(id);
         if (profile != null)
         {
            List<Repository> repositories = profile.getRepositories();
            for (Repository repository : repositories)
            {
               settingsRepos.add(new RemoteRepository.Builder(repository.getId(), repository.getLayout(), repository
                        .getUrl()).build());
            }
         }
      }
      
      final DefaultMirrorSelector mirrorSelector = createMirrorSelector(settings);
      
      final List<RemoteRepository> mirrorsForSettingsRepos = new ArrayList<RemoteRepository>();
      for(Iterator<RemoteRepository> iter  = settingsRepos.iterator(); iter.hasNext();)
      {
         RemoteRepository settingsRepository = iter.next();
         RemoteRepository repoMirror = mirrorSelector.getMirror(settingsRepository);
         // If a mirror is available for a repository, then remove the repo, and use the mirror instead
         if(repoMirror != null)
         {
            iter.remove();
            mirrorsForSettingsRepos.add(repoMirror);
         }
      }
      // We now have a collection of mirrors and unmirrored repositories. 
      settingsRepos.addAll(mirrorsForSettingsRepos);
      
      List<RemoteRepository> enrichedRepos = new ArrayList<RemoteRepository>();
      LazyAuthenticationSelector authSelector = createAuthSelector(settings, mirrorSelector);
      for(RemoteRepository settingsRepo: settingsRepos)
      {
         // Obtain the Authentication for the repository or it's mirror
         Authentication auth = authSelector.getAuthentication(settingsRepo);
         // All RemoteRepositories (Mirrors and Repositories) constructed so far lack Authentication info.
         // Use the settings repo as the prototype and create an enriched repo with the Authentication.
         enrichedRepos.add(new RemoteRepository.Builder(settingsRepo).setAuthentication(auth).build());
      }
      return enrichedRepos;
   }

   public Settings getSettings()
   {
      try
      {
         SettingsBuilder settingsBuilder = new DefaultSettingsBuilderFactory().newInstance();
         SettingsBuildingRequest settingsRequest = new DefaultSettingsBuildingRequest();
         String userSettingsLocation = System.getProperty(ALT_USER_SETTINGS_XML_LOCATION);
         if (userSettingsLocation != null)
         {
            settingsRequest.setUserSettingsFile(new File(userSettingsLocation));
         }
         else
         {
            settingsRequest.setUserSettingsFile(new File(getUserHomeDir(), "/.m2/settings.xml"));
         }
         String globalSettingsLocation = System.getProperty(ALT_GLOBAL_SETTINGS_XML_LOCATION);
         if (globalSettingsLocation != null)
         {
            settingsRequest.setGlobalSettingsFile(new File(globalSettingsLocation));
         }
         else
         {
            if (M2_HOME != null)
            {
               settingsRequest.setGlobalSettingsFile(new File(M2_HOME, "/conf/settings.xml"));
            }
         }
         SettingsBuildingResult settingsBuildingResult = settingsBuilder.build(settingsRequest);
         Settings effectiveSettings = settingsBuildingResult.getEffectiveSettings();

         if (effectiveSettings.getLocalRepository() == null)
         {
            String userRepositoryLocation = System.getProperty(ALT_LOCAL_REPOSITORY_LOCATION);
            if (userRepositoryLocation != null)
            {
               effectiveSettings.setLocalRepository(userRepositoryLocation);
            }
            else
            {
               effectiveSettings.setLocalRepository(getUserHomePath() + "/.m2/repository");
            }
         }

         return effectiveSettings;
      }
      catch (SettingsBuildingException e)
      {
         throw new RuntimeException(e);
      }
   }

   public RepositorySystem getRepositorySystem()
   {

      final DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
      locator.setServices(ModelBuilder.class, new DefaultModelBuilderFactory().newInstance());
      // Installing Wagon to fetch from HTTP repositories
      locator.setServices(WagonProvider.class, new ManualWagonProvider());
      locator.addService(RepositoryConnectorFactory.class, WagonRepositoryConnectorFactory.class);
      final RepositorySystem repositorySystem = locator.getService(RepositorySystem.class);
      return repositorySystem;
   }

   public static org.eclipse.aether.repository.Proxy convertFromMavenProxy(org.apache.maven.settings.Proxy proxy)
   {
      org.eclipse.aether.repository.Proxy result = null;
      if (proxy != null)
      {
         Authentication auth = new AuthenticationBuilder().addUsername(proxy.getUsername())
                  .addPassword(proxy.getPassword()).build();
         result = new org.eclipse.aether.repository.Proxy(proxy.getProtocol(), proxy.getHost(), proxy.getPort(), auth);
      }
      return result;
   }

   private File getUserHomeDir()
   {
      return new File(System.getProperty("user.home")).getAbsoluteFile();
   }

   private String getUserHomePath()
   {
      return getUserHomeDir().getAbsolutePath();
   }

   public DefaultRepositorySystemSession setupRepoSession(final RepositorySystem repoSystem, final Settings settings)
   {
      DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
      session.setOffline(false);
      
      Proxy activeProxy = settings.getActiveProxy();
      if (activeProxy != null)
      {
         DefaultProxySelector dps = new DefaultProxySelector();
         dps.add(convertFromMavenProxy(activeProxy), activeProxy.getNonProxyHosts());
         session.setProxySelector(dps);
      }

      final DefaultMirrorSelector mirrorSelector = createMirrorSelector(settings);
      final LazyAuthenticationSelector authSelector = createAuthSelector(settings, mirrorSelector);

      session.setMirrorSelector(mirrorSelector);
      session.setAuthenticationSelector(authSelector);

      LocalRepository localRepo = new LocalRepository(new File(settings.getLocalRepository()));
      session.setLocalRepositoryManager(repoSystem.newLocalRepositoryManager(session, localRepo));
      session.setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_IGNORE);
      session.setCache(new DefaultRepositoryCache());
      boolean cacheNotFoundArtifacts = true;
      boolean cacheTransferErrors = true;
      session.setResolutionErrorPolicy(new SimpleResolutionErrorPolicy(cacheNotFoundArtifacts, cacheTransferErrors));
      session.setWorkspaceReader(new ClasspathWorkspaceReader());
      session.setTransferListener(new LogTransferListener(System.out));
      return session;
   }
   
   private DefaultMirrorSelector createMirrorSelector(Settings settings)
   {
      final DefaultMirrorSelector mirrorSelector = new DefaultMirrorSelector();
      final List<Mirror> mirrors = settings.getMirrors();
      if (mirrors != null)
      {
         for (Mirror mirror : mirrors)
         {
            mirrorSelector.add(mirror.getId(), mirror.getUrl(), mirror.getLayout(), false, mirror.getMirrorOf(),
                     mirror.getMirrorOfLayouts());
         }
      }
      return mirrorSelector;
   }

   private LazyAuthenticationSelector createAuthSelector(final Settings settings,
            final DefaultMirrorSelector mirrorSelector)
   {
      LazyAuthenticationSelector authSelector = new LazyAuthenticationSelector(mirrorSelector);
      for (Server server : settings.getServers())
      {
         authSelector.add(
                  server.getId(),
                  new AuthenticationBuilder().addUsername(server.getUsername()).addPassword(server.getPassword())
                           .addPrivateKey(server.getPrivateKey(), server.getPassphrase()).build());
      }
      return authSelector;
   }

}