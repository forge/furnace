/**
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.util;

import static org.hamcrest.CoreMatchers.is;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

/**
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class OperatingSystemUtilsTest
{

   /**
    * Test method for {@link org.jboss.forge.furnace.util.OperatingSystemUtils#createTempDir()}.
    */
   @Test
   public void testCreateTempDir()
   {
      File tmpDir = OperatingSystemUtils.createTempDir();
      tmpDir.deleteOnExit();
      Assert.assertThat(tmpDir.isDirectory(), is(true));
   }

}
