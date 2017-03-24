/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.manager.maven.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.jboss.forge.furnace.manager.maven.MavenContainer;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class MavenRepositories
{
   protected static final String MAVEN_CENTRAL_REPO = "http://central.maven.org/maven2";

   public static List<RemoteRepository> getRemoteRepositories(MavenContainer container, Settings settings)
   {
      Set<RemoteRepository> remoteRepos = new HashSet<>();
      remoteRepos.addAll(container.getEnabledRepositoriesFromProfile(settings));

      // central repository is added if there is no central mirror
      String centralRepoURL = getCentralMirrorURL(settings).orElse(MAVEN_CENTRAL_REPO);
      remoteRepos.add(convertToMavenRepo("central", centralRepoURL, settings));
     
      return new ArrayList<>(remoteRepos);
   }

   static Optional<String> getCentralMirrorURL(Settings settings)
   {
      return settings.getMirrors().stream()
               .filter(m -> "central".equals(m.getMirrorOf()) ||
                        "*".equals(m.getMirrorOf()) ||
                        MAVEN_CENTRAL_REPO.equals(m.getMirrorOf()))
               .map(Mirror::getUrl)
               .findFirst();
   }

   static RemoteRepository convertToMavenRepo(final String id, String url, final Settings settings)
   {
      RemoteRepository.Builder remoteRepositoryBuilder = new RemoteRepository.Builder(id, "default", url);
      Proxy activeProxy = settings.getActiveProxy();
      if (activeProxy != null)
      {
         Authentication auth = new AuthenticationBuilder().addUsername(activeProxy.getUsername())
                  .addPassword(activeProxy.getPassword()).build();
         remoteRepositoryBuilder.setProxy(new org.eclipse.aether.repository.Proxy(activeProxy.getProtocol(),
                  activeProxy
                           .getHost(),
                  activeProxy.getPort(), auth));
      }
      return remoteRepositoryBuilder.build();
   }

}
