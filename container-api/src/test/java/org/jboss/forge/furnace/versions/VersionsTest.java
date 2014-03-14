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
 *
 */
public class VersionsTest
{

   @Test
   public void testAreEqual()
   {
      Assert.assertEquals(new SingleVersion("1"), new SingleVersion("1"));
      Assert.assertEquals(new SingleVersion("1.1"), new SingleVersion("1.1"));
      Assert.assertEquals(new SingleVersion("1.1.1"), new SingleVersion("1.1.1"));
      Assert.assertEquals(new SingleVersion("1.1.1-SNAPSHOT"), new SingleVersion("1.1.1-SNAPSHOT"));
      Assert.assertNotEquals(new SingleVersion("1"), new SingleVersion("2"));
      Assert.assertNotEquals(new SingleVersion("1.1"), new SingleVersion("1.1.1"));
      Assert.assertNotEquals(new SingleVersion("1.1.1-SNAPSHOT"), new SingleVersion("1.1.1"));
   }

   @Test
   public void testParseVersionRange() throws Exception
   {
      VersionRange range = Versions.parseVersionRange("[0,15]");
      Assert.assertEquals(new SingleVersion("0"), range.getMin());
      Assert.assertEquals(new SingleVersion("15"), range.getMax());
      Assert.assertTrue(range.isMinInclusive());
      Assert.assertTrue(range.isMaxInclusive());
   }

   @Test
   public void testVersionRangeIntersection() throws Exception
   {
      VersionRange set = Versions.parseVersionRange("[0,15]");
      VersionRange subset = Versions.parseVersionRange("[3,7)");

      VersionRange intersection = Versions.intersection(set, subset);
      Assert.assertEquals(new SingleVersion("3"), intersection.getMin());
      Assert.assertEquals(new SingleVersion("7"), intersection.getMax());
      Assert.assertTrue(intersection.isMinInclusive());
      Assert.assertFalse(intersection.isMaxInclusive());
   }

   @Test
   public void testVersionSnapshot() throws Exception
   {
      Version nonSnapshot = new SingleVersion("1.1.1");
      Assert.assertFalse(Versions.isSnapshot(nonSnapshot));
      Version snapshot = new SingleVersion("1.1.1-SNAPSHOT");
      Assert.assertTrue(Versions.isSnapshot(snapshot));
   }

   @Test
   public void testSnapshotLowerThanRelease() throws Exception
   {
      Version nonSnapshot = new SingleVersion("2.2.0-Final");
      Version snapshot = new SingleVersion("2.1.2-SNAPSHOT");
      Assert.assertTrue(nonSnapshot.compareTo(snapshot) >= 0);
      Assert.assertTrue(snapshot.compareTo(nonSnapshot) < 0);

   }
}
