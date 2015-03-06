/**
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package test.org.jboss.forge.furnace.api;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.AddonDependencies;
import org.jboss.forge.arquillian.AddonDependency;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Test;
import org.junit.runner.RunWith;

import test.org.jboss.forge.furnace.util.TestRepositoryDeploymentListener;

/**
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
@SuppressWarnings("deprecation")
@RunWith(Arquillian.class)
public class ForgeArchiveSmokeTest
{
   @Deployment
   @AddonDependencies({
            @AddonDependency(name = "test:no_dep", version = "1.0.0.Final", listener = TestRepositoryDeploymentListener.class)
   })
   public static ForgeArchive getDeployment()
   {
      return ShrinkWrap.create(ForgeArchive.class).addAsLocalServices(ForgeArchiveSmokeTest.class);
   }

   @Test
   public void test()
   {
      // success
   }

}
