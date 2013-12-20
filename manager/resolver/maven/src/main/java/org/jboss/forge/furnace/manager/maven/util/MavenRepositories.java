/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.manager.maven.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
   private static final String MAVEN_CENTRAL_REPO = "http://repo1.maven.org/maven2";

   public static List<RemoteRepository> getRemoteRepositories(MavenContainer container, Settings settings)
   {
      Set<RemoteRepository> remoteRepos = new HashSet<>();
      remoteRepos.addAll(container.getEnabledRepositoriesFromProfile(settings));
      if (remoteRepos.isEmpty())
      {
         // Add central in case remote repo list is empty
         remoteRepos.add(convertToMavenRepo("central", MAVEN_CENTRAL_REPO, settings));
      }
      return Arrays.asList(remoteRepos.toArray(new RemoteRepository[] {}));
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
                           .getHost(), activeProxy.getPort(), auth));
      }
      return remoteRepositoryBuilder.build();
   }

}
