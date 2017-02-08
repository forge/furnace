/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.manager.maven.util;

import static org.hamcrest.CoreMatchers.equalTo;

import java.io.File;
import java.util.List;

import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.eclipse.aether.repository.RemoteRepository;
import org.jboss.forge.furnace.manager.maven.MavenContainer;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class MavenRepositoriesTest
{

   @Test
   public void testMirrorCentralWithoutProfiles() throws Exception
   {
      SettingsBuilder settingsBuilder = new DefaultSettingsBuilderFactory().newInstance();
      SettingsBuildingRequest settingsRequest = new DefaultSettingsBuildingRequest();
      settingsRequest.setUserSettingsFile(new File("src/test/resources/profiles/mirror-settings.xml"));
      Settings settings = settingsBuilder.build(settingsRequest).getEffectiveSettings();
      MavenContainer container = new MavenContainer();
      List<RemoteRepository> remoteRepositories = MavenRepositories.getRemoteRepositories(container, settings);
      Assert.assertThat(remoteRepositories.size(), equalTo(1));
      Assert.assertThat(remoteRepositories.get(0).getId(), equalTo("central"));
      Assert.assertThat(remoteRepositories.get(0).getUrl(),
               equalTo("http://repo.cloudbees.com/content/repositories/central/"));
   }

}
