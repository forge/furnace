/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.arquillian.maven;

import java.io.File;
import java.util.List;

import org.apache.maven.model.Model;
import org.eclipse.aether.graph.Dependency;
import org.junit.Assert;
import org.junit.Test;

public class ProjectHelperTest
{

   ProjectHelper projectHelper = new ProjectHelper();

   @Test
   public void testLoadPomFromFile()
   {
      File resource = new File(getClass().getResource("lib-1.0.0.Final.pom").getFile());
      Assert.assertTrue(resource.exists());
      Model model = projectHelper.loadPomFromFile(resource);
      Assert.assertNotNull(model);
   }

   @Test
   public void testProjectBuilding() throws Exception
   {
      File resource = new File(getClass().getResource("lib-1.0.0.Final.pom").getFile());
      Assert.assertTrue(resource.exists());
      List<Dependency> deps = projectHelper.resolveDependenciesFromPOM(resource);
      Assert.assertEquals(13, deps.size());
   }
}
