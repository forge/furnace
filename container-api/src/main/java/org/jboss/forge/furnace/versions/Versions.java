/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.versions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.forge.furnace.util.Assert;
import org.jboss.forge.furnace.util.Strings;

/**
 * Utility for interacting with {@link Version} instances.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class Versions
{
   private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)(\\.|-)(.*)");
   private static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";

   /**
    * This method only returns true if:
    * 
    * - The major version of addonApiVersion is equal to the major version of runtimeVersion AND
    * 
    * - The minor version of addonApiVersion is less or equal to the minor version of runtimeVersion
    * 
    * - The addonApiVersion is null
    * 
    * @param runtimeVersion a version in the format x.x.x
    * @param addonApiVersion a version in the format x.x.x
    */
   public static boolean isApiCompatible(Version runtimeVersion, Version addonApiVersion)
   {
      if (addonApiVersion == null || addonApiVersion.toString().length() == 0
               || runtimeVersion == null || runtimeVersion.toString().length() == 0)
         return true;

      Matcher runtimeMatcher = VERSION_PATTERN.matcher(runtimeVersion.toString());
      if (runtimeMatcher.matches())
      {
         int runtimeMajorVersion = Integer.parseInt(runtimeMatcher.group(1));
         int runtimeMinorVersion = Integer.parseInt(runtimeMatcher.group(2));

         Matcher addonApiMatcher = VERSION_PATTERN.matcher(addonApiVersion.toString());
         if (addonApiMatcher.matches())
         {
            int addonApiMajorVersion = Integer.parseInt(addonApiMatcher.group(1));
            int addonApiMinorVersion = Integer.parseInt(addonApiMatcher.group(2));

            if (addonApiMajorVersion == runtimeMajorVersion && addonApiMinorVersion <= runtimeMinorVersion)
            {
               return true;
            }
         }
      }
      return false;
   }

   /**
    * Create a version range from a string representation
    * <p/>
    * For example:
    * <ul>
    * <li><code>1.0</code> Version 1.0</li>
    * <li><code>[1.0,2.0)</code> Versions 1.0 (included) to 2.0 (not included)</li>
    * <li><code>[1.0,2.0]</code> Versions 1.0 to 2.0 (both included)</li>
    * <li><code>[1.5,)</code> Versions 1.5 and higher</li>
    * <li><code>(,1.0],[1.2,)</code> Versions up to 1.0 (included) and 1.2 or higher</li>
    * </ul>
    * 
    * @param intersection string representation of a version or version range
    * @return a new {@link MultipleVersionRange} object that represents the specification
    * @throws VersionException
    */
   public static MultipleVersionRange parseMultipleVersionRange(String intersection) throws VersionException
   {
      Assert.notNull(intersection, "Version range string must not be null.");

      List<VersionRange> ranges = new ArrayList<VersionRange>();
      String process = intersection;
      Version upperBound = null;
      Version lowerBound = null;

      while (process.startsWith("[") || process.startsWith("("))
      {
         int index1 = process.indexOf(")");
         int index2 = process.indexOf("]");

         int index = index2;
         if (index2 < 0 || index1 < index2)
         {
            if (index1 >= 0)
            {
               index = index1;
            }
         }

         if (index < 0)
         {
            throw new VersionException("Unbounded range: " + intersection);
         }

         VersionRange range = parseVersionRange(process.substring(0, index + 1));
         if (lowerBound == null)
         {
            lowerBound = range.getMin();
         }
         if (upperBound != null)
         {
            if (range.getMin() == null || range.getMin().compareTo(upperBound) < 0)
            {
               throw new VersionException("Ranges overlap: " + intersection);
            }
         }
         ranges.add(range);
         upperBound = range.getMax();

         process = process.substring(index + 1).trim();

         if (process.length() > 0 && process.startsWith(","))
         {
            process = process.substring(1).trim();
         }
      }

      if (process.length() > 0)
      {
         if (ranges.size() > 0)
         {
            throw new VersionException("Only fully-qualified sets allowed in multiple version range scenario: "
                     + intersection);
         }
         if (process.contains(","))
         {
            String[] split = process.split(",");
            for (String version : split)
            {
               if (version.startsWith("[") || version.startsWith("("))
                  ranges.add(parseVersionRange(version));
               else
                  ranges.add(new SingleVersionRange(new SingleVersion(version)));
            }
         }
         else
         {
            ranges.add(new SingleVersionRange(new SingleVersion(process)));
         }
      }

      return new MultipleVersionRange(ranges);
   }

   public static VersionRange parseVersionRange(String range) throws VersionException
   {
      boolean lowerBoundInclusive = range.startsWith("[");
      boolean upperBoundInclusive = range.endsWith("]");

      String process = range.substring(1, range.length() - 1).trim();

      VersionRange result;
      int index = process.indexOf(",");
      if (index < 0)
      {
         if (!lowerBoundInclusive || !upperBoundInclusive)
         {
            throw new VersionException("Single version must be surrounded by []: " + range);
         }

         Version version = new SingleVersion(process);
         result = new DefaultVersionRange(version, lowerBoundInclusive, version, upperBoundInclusive);
      }
      else
      {
         String lowerBound = process.substring(0, index).trim();
         String upperBound = process.substring(index + 1).trim();
         if (lowerBound.equals(upperBound))
         {
            throw new VersionException("Range cannot have identical boundaries: " + range);
         }

         Version lowerVersion = null;
         if (lowerBound.length() > 0)
         {
            lowerVersion = new SingleVersion(lowerBound);
         }
         Version upperVersion = null;
         if (upperBound.length() > 0)
         {
            upperVersion = new SingleVersion(upperBound);
         }

         if (upperVersion != null && lowerVersion != null && upperVersion.compareTo(lowerVersion) < 0)
         {
            throw new VersionException("Range defies version ordering: " + range);
         }

         result = new DefaultVersionRange(lowerVersion, lowerBoundInclusive, upperVersion, upperBoundInclusive);
      }

      return result;
   }

   public static VersionRange intersection(VersionRange... ranges)
   {
      return intersection(Arrays.asList(ranges));
   }

   public static VersionRange intersection(Collection<VersionRange> ranges)
   {
      Version min = null;
      Version max = null;
      boolean minInclusive = false;
      boolean maxInclusive = false;

      for (VersionRange range : ranges)
      {
         if (min == null || range.getMin().compareTo(min) > 0)
         {
            min = range.getMin();
            minInclusive = range.isMinInclusive();
         }
         if (max == null || range.getMax().compareTo(max) < 0)
         {
            max = range.getMax();
            maxInclusive = range.isMaxInclusive();
         }
      }

      return new DefaultVersionRange(min, minInclusive, max, maxInclusive);
   }

   /**
    * Returns if the version specified is a SNAPSHOT
    * 
    * @param version cannot be null
    * @return true if the version is a SNAPSHOT, false otherwise
    */
   public static boolean isSnapshot(Version version)
   {
      return version.toString().endsWith(SNAPSHOT_SUFFIX);
   }

   /**
    * Returns the Specification version for the given {@link Class}
    * 
    * @param type the {@link Class} with the corresponding package
    * @return {@link Version} representation from the {@link Package#getSpecificationVersion()} returned from
    *         {@link Class#getPackage()}
    */
   public static Version getSpecificationVersionFor(Class<?> type)
   {
      String version = type.getPackage().getSpecificationVersion();
      if (Strings.isNullOrEmpty(version))
      {
         return EmptyVersion.getInstance();
      }
      return new SingleVersion(version);
   }

   /**
    * Returns the Implementation version for the given {@link Class}
    * 
    * @param type the {@link Class} with the corresponding package
    * @return {@link Version} representation from the {@link Package#getImplementationVersion()} returned from
    *         {@link Class#getPackage()}
    */
   public static Version getImplementationVersionFor(Class<?> type)
   {
      String version = type.getPackage().getImplementationVersion();
      if (Strings.isNullOrEmpty(version))
      {
         return EmptyVersion.getInstance();
      }
      return new SingleVersion(version);
   }

}