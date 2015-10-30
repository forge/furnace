/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package test.org.jboss.forge.furnace.services;

import static org.hamcrest.CoreMatchers.equalTo;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.forge.arquillian.services.LocalServices;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.services.Imported;
import org.jboss.forge.furnace.util.Iterators;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import test.org.jboss.forge.furnace.mocks.MockImpl1;
import test.org.jboss.forge.furnace.mocks.MockImpl2;
import test.org.jboss.forge.furnace.mocks.MockInterface;
import test.org.jboss.forge.furnace.mocks.SubMockImpl1;

/**
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
@RunWith(Arquillian.class)
public class ServiceLookupTest
{
   @Deployment
   public static AddonArchive getDeployment()
   {
      return ShrinkWrap.create(AddonArchive.class)
               .addClasses(MockInterface.class, MockImpl1.class, MockImpl2.class, SubMockImpl1.class)
               .addAsLocalServices(MockImpl1.class, MockImpl2.class, SubMockImpl1.class, ServiceLookupTest.class);
   }

   @Test
   public void shouldResolveImpls() throws Exception
   {
      AddonRegistry registry = LocalServices.getFurnace(getClass().getClassLoader())
               .getAddonRegistry();
      Imported<MockInterface> imported = registry.getServices(MockInterface.class);
      Assert.assertTrue(imported.isAmbiguous());
      Assert.assertEquals(3, Iterators.asList(imported).size());
      Assert.assertThat(registry.getExportedTypes(MockInterface.class).size(), equalTo(3));
   }

}
