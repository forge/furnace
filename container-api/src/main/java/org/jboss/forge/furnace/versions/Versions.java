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

import org.jboss.forge.furnace.util.Assert;
import org.jboss.forge.furnace.util.Strings;

/**
 * Utility for interacting with {@link Version} instances.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class Versions
{
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

      int runtimeMajorVersion = runtimeVersion.getMajorVersion();
      int runtimeMinorVersion = runtimeVersion.getMinorVersion();
      int addonApiMajorVersion = addonApiVersion.getMajorVersion();
      int addonApiMinorVersion = addonApiVersion.getMinorVersion();
      return (addonApiMajorVersion == runtimeMajorVersion && addonApiMinorVersion <= runtimeMinorVersion);
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
    * </ul>
    * 
    * @param range string representation of a version or version range
    * @return a new {@link VersionRange} object that represents the specification
    * @throws VersionException
    */
   public static VersionRange parseVersionRange(String range) throws VersionException
   {
      Assert.notNull(range, "Version range must not be null.");
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

         Version version = SingleVersion.valueOf(process);
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
            lowerVersion = SingleVersion.valueOf(lowerBound);
         }
         Version upperVersion = null;
         if (upperBound.length() > 0)
         {
            upperVersion = SingleVersion.valueOf(upperBound);
         }

         if (upperVersion != null && lowerVersion != null && upperVersion.compareTo(lowerVersion) < 0)
         {
            throw new VersionException("Range defies version ordering: " + range);
         }

         result = new DefaultVersionRange(lowerVersion, lowerBoundInclusive, upperVersion, upperBoundInclusive);
      }

      return result;
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
      Assert.notNull(intersection, "Version range must not be null.");

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
                  ranges.add(new SingleVersionRange(SingleVersion.valueOf(version)));
            }
         }
         else
         {
            ranges.add(new SingleVersionRange(SingleVersion.valueOf(process)));
         }
      }

      return new MultipleVersionRange(ranges);
   }

   /**
    * Calculate the intersection of one or more {@link VersionRange} instances, returning a single {@link VersionRange}
    * as the result.
    */
   public static VersionRange intersection(VersionRange... ranges)
   {
      Assert.notNull(ranges, "Version ranges must not be null.");
      Assert.isTrue(ranges.length >= 1, "Version ranges must not be empty.");
      return intersection(Arrays.asList(ranges));
   }

   /**
    * Calculate the intersection of one or more {@link VersionRange} instances, returning a single {@link VersionRange}
    * as the result.
    */
   public static VersionRange intersection(Collection<VersionRange> ranges)
   {
      Assert.notNull(ranges, "Version ranges must not be null.");
      Assert.isTrue(ranges.size() >= 1, "Version ranges must not be empty.");

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
      Assert.notNull(version, "Version must not be null.");
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
      Assert.notNull(type, "Type must not be null.");
      final Version result;
      Package pkg = type.getPackage();
      if (pkg == null)
      {
         result = EmptyVersion.getInstance();
      }
      else
      {
         String version = pkg.getSpecificationVersion();
         if (Strings.isNullOrEmpty(version))
         {
            result = EmptyVersion.getInstance();
         }
         else
         {
            result = SingleVersion.valueOf(version);
         }
      }
      return result;
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
      Assert.notNull(type, "Type must not be null.");
      final Version result;
      Package pkg = type.getPackage();
      if (pkg == null)
      {
         result = EmptyVersion.getInstance();
      }
      else
      {
         String version = pkg.getImplementationVersion();
         if (Strings.isNullOrEmpty(version))
         {
            result = EmptyVersion.getInstance();
         }
         else
         {
            result = SingleVersion.valueOf(version);
         }
      }
      return result;
   }

}