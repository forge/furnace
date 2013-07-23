/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.manager.maven.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.jboss.forge.furnace.manager.maven.MavenContainer;
import org.sonatype.aether.repository.Authentication;
import org.sonatype.aether.repository.RemoteRepository;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class MavenRepositories
{
   private static final String MAVEN_CENTRAL_REPO = "http://repo1.maven.org/maven2";

   public static List<RemoteRepository> getRemoteRepositories(MavenContainer container, Settings settings)
   {
      List<RemoteRepository> remoteRepos = new ArrayList<RemoteRepository>();
      remoteRepos.addAll(container.getEnabledRepositoriesFromProfile(settings));
      if (remoteRepos.isEmpty())
      {
         // Add central in case remote repo list is empty
         remoteRepos.add(convertToMavenRepo("central", MAVEN_CENTRAL_REPO, settings));
      }
      return remoteRepos;
   }

   static RemoteRepository convertToMavenRepo(final String id, String url, final Settings settings)
   {
      RemoteRepository remoteRepository = new RemoteRepository(id, "default", url);
      Proxy activeProxy = settings.getActiveProxy();
      if (activeProxy != null)
      {
         Authentication auth = new Authentication(activeProxy.getUsername(), activeProxy.getPassword());
         remoteRepository.setProxy(new org.sonatype.aether.repository.Proxy(activeProxy.getProtocol(),
                  activeProxy
                           .getHost(), activeProxy.getPort(), auth));
      }
      return remoteRepository;
   }

}
