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
      Assert.assertEquals(SingleVersion.valueOf("1"), SingleVersion.valueOf("1"));
      Assert.assertEquals(SingleVersion.valueOf("1.1"), SingleVersion.valueOf("1.1"));
      Assert.assertEquals(SingleVersion.valueOf("1.1.1"), SingleVersion.valueOf("1.1.1"));
      Assert.assertEquals(SingleVersion.valueOf("1.1.1-SNAPSHOT"), SingleVersion.valueOf("1.1.1-SNAPSHOT"));
      Assert.assertNotEquals(SingleVersion.valueOf("1"), SingleVersion.valueOf("2"));
      Assert.assertNotEquals(SingleVersion.valueOf("1.1"), SingleVersion.valueOf("1.1.1"));
      Assert.assertNotEquals(SingleVersion.valueOf("1.1.1-SNAPSHOT"), SingleVersion.valueOf("1.1.1"));
   }

   @Test
   public void testParseVersionRange() throws Exception
   {
      VersionRange range = Versions.parseVersionRange("[0,15]");
      Assert.assertEquals(SingleVersion.valueOf("0"), range.getMin());
      Assert.assertEquals(SingleVersion.valueOf("15"), range.getMax());
      Assert.assertTrue(range.isMinInclusive());
      Assert.assertTrue(range.isMaxInclusive());
   }

   @Test
   public void testVersionRangeIntersection() throws Exception
   {
      VersionRange set = Versions.parseVersionRange("[0,15]");
      VersionRange subset = Versions.parseVersionRange("[3,7)");

      VersionRange intersection = Versions.intersection(set, subset);
      Assert.assertEquals(SingleVersion.valueOf("3"), intersection.getMin());
      Assert.assertEquals(SingleVersion.valueOf("7"), intersection.getMax());
      Assert.assertTrue(intersection.isMinInclusive());
      Assert.assertFalse(intersection.isMaxInclusive());
   }

   @Test
   public void testVersionSnapshot() throws Exception
   {
      Version nonSnapshot = SingleVersion.valueOf("1.1.1");
      Assert.assertFalse(Versions.isSnapshot(nonSnapshot));
      Version snapshot = SingleVersion.valueOf("1.1.1-SNAPSHOT");
      Assert.assertTrue(Versions.isSnapshot(snapshot));
   }

   @Test
   public void testSnapshotLowerThanRelease() throws Exception
   {
      Version nonSnapshot = SingleVersion.valueOf("2.2.0-Final");
      Version snapshot = SingleVersion.valueOf("2.1.2-SNAPSHOT");
      Assert.assertTrue(nonSnapshot.compareTo(snapshot) >= 0);
      Assert.assertTrue(snapshot.compareTo(nonSnapshot) < 0);
   }

   @Test
   public void testIsApiCompatible0() throws Exception
   {
      Assert.assertTrue(Versions.isApiCompatible(
               SingleVersion.valueOf("2.18.2-SNAPSHOT"),
               SingleVersion.valueOf("2.16.1.Final")));
   }

   @Test
   public void testIsApiCompatible1() throws Exception
   {
      Assert.assertTrue(Versions.isApiCompatible(
               SingleVersion.valueOf("2.18.2.Final"),
               SingleVersion.valueOf("2.16.1.Final")));
   }
}
