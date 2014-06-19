package org.jboss.forge.furnace;

import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.versions.EmptyVersion;
import org.jboss.forge.furnace.versions.SingleVersion;
import org.jboss.forge.furnace.versions.Version;
import org.junit.Assert;
import org.junit.Test;

public class AddonIdTest
{
   @Test
   public void testFromCoordinatesMissingAPIVersion() throws Exception
   {
      AddonId addon = AddonId.fromCoordinates("org.jboss.forge.addon:resources,2.0.0-SNAPSHOT");
      Assert.assertEquals(EmptyVersion.getInstance(), addon.getApiVersion());
      Assert.assertEquals("org.jboss.forge.addon:resources", addon.getName());
      Assert.assertEquals(new SingleVersion("2.0.0-SNAPSHOT"), addon.getVersion());
   }

   @Test
   public void testFromCoordinates()
   {
      AddonId entry = AddonId.fromCoordinates("org.example:example-addon,1.0.0-SNAPSHOT,2.0.0");
      Assert.assertEquals("org.example:example-addon,1.0.0-SNAPSHOT", entry.toCoordinates());
   }

   @Test
   public void testFromCoordinatesWithVersionRange()
   {
      AddonId entry = AddonId.fromCoordinates("org.example:example-addon,[1.0.0-SNAPSHOT,2.0.0]");
      Assert.assertEquals("org.example:example-addon,[1.0.0-SNAPSHOT,2.0.0]", entry.toCoordinates());
   }

   @Test
   public void testFromCoordinatesWithVersionRangeAndApiVersion()
   {
      AddonId entry = AddonId.fromCoordinates("org.example:example-addon,[1.0.0-SNAPSHOT,2.0.0],2.6.0.Final");
      Assert.assertEquals("org.example:example-addon,[1.0.0-SNAPSHOT,2.0.0]", entry.toCoordinates());
   }

   @Test(expected = IllegalArgumentException.class)
   public void testFromCoordinatesInvalid()
   {
      AddonId.fromCoordinates("org.example:example-addon,[1.0.0-SNAPSHOT,2.0.0],2.6.0.Final,5.0.0");
   }

   @Test(expected = IllegalArgumentException.class)
   public void testFromCoordinatesInvalid2()
   {
      AddonId.fromCoordinates("org.example:example-addon,[1.0.0-SNAPSHOT,2.0.0,2.6.0.Final,5.0.0");
   }

   @Test
   public void testFromIndividual()
   {
      AddonId entry = AddonId.from("org.example:example-addon", "1.0.0-SNAPSHOT", "2.0.0");
      Assert.assertEquals("org.example:example-addon,1.0.0-SNAPSHOT", entry.toCoordinates());
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNoName()
   {
      AddonId.from(null, "1.0.0-SNAPSHOT", "2.0.0");
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNoVersion()
   {
      AddonId.from("name", "", "2.0.0");
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNoNameOrVersion()
   {
      AddonId.from(null, null, "2.0.0");
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNullVersion()
   {
      AddonId.from("name", null, "2.0.0");
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNullNameCompact()
   {
      AddonId.from(null, "1.0.0-SNAPSHOT");
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNoNameCompact()
   {
      AddonId.from("", "1.0.0-SNAPSHOT");
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNullVersionCompact()
   {
      AddonId.from("name", (Version) null);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNullVersionString()
   {
      AddonId.from("name", (String) null);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNoNameOrVersionCompact()
   {
      AddonId.from(null, (Version) null);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNoNameOrVersionCompactString()
   {
      AddonId.from(null, (String) null);
   }

   @Test
   public void testNoApi()
   {
      AddonId.from("name", "1.0.0-SNAPSHOT", null);
      AddonId.from("name", "1.0.0-SNAPSHOT");
   }

   @Test
   public void testCompareToEquals() throws Exception
   {
      AddonId left = AddonId.from("name", "1.0.0-SNAPSHOT");
      AddonId right = AddonId.from("name", "1.0.0-SNAPSHOT");

      Assert.assertEquals(0, left.compareTo(right));
   }

   @Test
   public void testCompareToNameLt() throws Exception
   {
      AddonId left = AddonId.from("abc", "1.0.0-SNAPSHOT");
      AddonId right = AddonId.from("def", "1.0.0-SNAPSHOT");

      Assert.assertTrue(left.compareTo(right) < 0);
   }

   @Test
   public void testCompareToNameGt() throws Exception
   {
      AddonId left = AddonId.from("def", "1.0.0-SNAPSHOT");
      AddonId right = AddonId.from("abc", "1.0.0-SNAPSHOT");

      Assert.assertTrue(left.compareTo(right) > 0);
   }

   @Test
   public void testCompareToVersionLt() throws Exception
   {
      AddonId left = AddonId.from("name", "1.0.0-SNAPSHOT");
      AddonId right = AddonId.from("name", "2.0.0-SNAPSHOT");

      Assert.assertTrue(left.compareTo(right) < 0);
   }

   @Test
   public void testCompareToVersionGt() throws Exception
   {
      AddonId left = AddonId.from("name", "2.0.0-SNAPSHOT");
      AddonId right = AddonId.from("name", "1.0.0-SNAPSHOT");

      Assert.assertTrue(left.compareTo(right) > 0);
   }

   @Test
   public void testCompareToEqualsWithApiVersion() throws Exception
   {
      AddonId left = AddonId.from("name", "1.0.0-SNAPSHOT", "0");
      AddonId right = AddonId.from("name", "1.0.0-SNAPSHOT", "0");

      Assert.assertEquals(0, left.compareTo(right));
   }

   @Test
   public void testCompareToEqualsWithMismatchedApiVersionLt() throws Exception
   {
      AddonId left = AddonId.from("name", "1.0.0-SNAPSHOT", "0");
      AddonId right = AddonId.from("name", "1.0.0-SNAPSHOT", "1");

      Assert.assertTrue(left.compareTo(right) < 0);
   }

   @Test
   public void testCompareToEqualsWithMismatchedApiVersionGt() throws Exception
   {
      AddonId left = AddonId.from("name", "1.0.0-SNAPSHOT", "2");
      AddonId right = AddonId.from("name", "1.0.0-SNAPSHOT", "1");

      Assert.assertTrue(left.compareTo(right) > 0);
   }
}
