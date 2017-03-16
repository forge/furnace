/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.impl.addons;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class AddonRepositoryStateStrategyImplTest
{

   /**
    * Test method for
    * {@link org.jboss.forge.furnace.impl.addons.AddonRepositoryStateStrategyImpl#getXmlRoot(java.io.File)}.
    */
   @Test
   public void testGetXmlRoot() throws Exception
   {
      File registryFile = new File("src/test/resources/invalid.xml");
      Assert.assertNotNull(AddonRepositoryStateStrategyImpl.getXmlRoot(registryFile));
   }

}
