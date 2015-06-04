/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.versions;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class SingleVersionTest
{
   @Test(expected = IllegalArgumentException.class)
   public void testVersionMustNotBeNull()
   {
      new SingleVersion(null);
   }

   @Test
   public void testIrregularVersion()
   {
      Version version = new SingleVersion("asdfa[23()_2345");
      Assert.assertEquals(0, version.getMajorVersion());
      Assert.assertEquals(0, version.getMinorVersion());
      Assert.assertEquals(0, version.getIncrementalVersion());
      Assert.assertEquals(null, version.getQualifier());
      Assert.assertEquals(0, version.getBuildNumber());
      Assert.assertEquals("asdfa[23()_2345", version.toString());
   }

   @Test
   public void testIrregularVersion2()
   {
      Version version = new SingleVersion("2.16.asdf.4.adsf");
      Assert.assertEquals(0, version.getMajorVersion());
      Assert.assertEquals(0, version.getMinorVersion());
      Assert.assertEquals(0, version.getIncrementalVersion());
      Assert.assertEquals(null, version.getQualifier());
      Assert.assertEquals(0, version.getBuildNumber());
      Assert.assertEquals("2.16.asdf.4.adsf", version.toString());
   }

   @Test
   public void testSnapshot() throws Exception
   {
      Version version = new SingleVersion("2.18.2-SNAPSHOT");
      Assert.assertEquals(2, version.getMajorVersion());
      Assert.assertEquals(18, version.getMinorVersion());
      Assert.assertEquals(2, version.getIncrementalVersion());
      Assert.assertEquals("SNAPSHOT", version.getQualifier());
      Assert.assertEquals(0, version.getBuildNumber());
      Assert.assertEquals("2.18.2-SNAPSHOT", version.toString());
   }

   @Test
   public void testSnapshotBuildNumber() throws Exception
   {
      Version version = new SingleVersion("2.18.2-SNAPSHOT-2");
      Assert.assertEquals(2, version.getMajorVersion());
      Assert.assertEquals(18, version.getMinorVersion());
      Assert.assertEquals(2, version.getIncrementalVersion());
      Assert.assertEquals("SNAPSHOT", version.getQualifier());
      Assert.assertEquals(2, version.getBuildNumber());
      Assert.assertEquals("2.18.2-SNAPSHOT-2", version.toString());
   }

   @Test
   public void testFinal() throws Exception
   {
      Version version = new SingleVersion("2.18.2.Final");
      Assert.assertEquals(2, version.getMajorVersion());
      Assert.assertEquals(18, version.getMinorVersion());
      Assert.assertEquals(2, version.getIncrementalVersion());
      Assert.assertEquals("Final", version.getQualifier());
      Assert.assertEquals(0, version.getBuildNumber());
      Assert.assertEquals("2.18.2.Final", version.toString());
   }

   @Test
   public void testFinalBuildNumber() throws Exception
   {
      Version version = new SingleVersion("2.18.2.Final-01");
      Assert.assertEquals(2, version.getMajorVersion());
      Assert.assertEquals(18, version.getMinorVersion());
      Assert.assertEquals(2, version.getIncrementalVersion());
      Assert.assertEquals("Final", version.getQualifier());
      Assert.assertEquals(1, version.getBuildNumber());
      Assert.assertEquals("2.18.2.Final-01", version.toString());
   }

   @Test
   public void testBare() throws Exception
   {
      Version version = new SingleVersion("2.18.2");
      Assert.assertEquals(2, version.getMajorVersion());
      Assert.assertEquals(18, version.getMinorVersion());
      Assert.assertEquals(2, version.getIncrementalVersion());
      Assert.assertEquals(null, version.getQualifier());
      Assert.assertEquals(0, version.getBuildNumber());
      Assert.assertEquals("2.18.2", version.toString());
   }

   @Test
   public void testBareBuildNumber() throws Exception
   {
      Version version = new SingleVersion("2.18.2-4");
      Assert.assertEquals(2, version.getMajorVersion());
      Assert.assertEquals(18, version.getMinorVersion());
      Assert.assertEquals(2, version.getIncrementalVersion());
      Assert.assertEquals(null, version.getQualifier());
      Assert.assertEquals(4, version.getBuildNumber());
      Assert.assertEquals("2.18.2-4", version.toString());
   }
}
