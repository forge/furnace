/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jboss.forge.furnace.versions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Construct a version range from a specification.
 * 
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class MultipleVersionRange implements VersionRange
{
   private final List<VersionRange> ranges;

   public MultipleVersionRange(VersionRange... ranges)
   {
      this(Arrays.asList(ranges));
   }

   MultipleVersionRange(List<VersionRange> ranges)
   {
      this.ranges = ranges;
   }

   public List<VersionRange> getRanges()
   {
      return ranges;
   }

   public MultipleVersionRange cloneOf()
   {
      List<VersionRange> copiedRanges = null;

      if (ranges != null)
      {
         copiedRanges = new ArrayList<VersionRange>();

         if (!ranges.isEmpty())
         {
            copiedRanges.addAll(ranges);
         }
      }

      return new MultipleVersionRange(copiedRanges);
   }

   /**
    * Creates and returns a new <code>MultipleVersionRange</code> that is a intersection of this version range and the
    * specified version range.
    * <p>
    * Note: Precedence is given to the recommended version from this version range over the recommended version from the
    * specified version range.
    * </p>
    * 
    * @param intersection the <code>MultipleVersionRange</code> that will be used to restrict this version range.
    * @return the <code>MultipleVersionRange</code> that is a intersection of this version range and the specified
    *         version range.
    *         <p>
    *         The ranges of the returned version range will be an intersection of the ranges of this version range and
    *         the specified version range if both version ranges have ranges. Otherwise, the ranges on the returned
    *         range will be empty.
    *         </p>
    *         <p>
    *         The recommended version of the returned version range will be the recommended version of this version
    *         range, provided that ranges falls within the intersected ranges. If the ranges are empty, this version
    *         range's recommended version is used if it is not <code>null</code>. If it is <code>null</code>, the
    *         specified version range's recommended version is used (provided it is non-<code>null</code>). If no
    *         recommended version can be obtained, the returned version range's recommended version is set to
    *         <code>null</code>.
    *         </p>
    * @throws NullPointerException if the specified <code>MultipleVersionRange</code> is <code>null</code>.
    */
   public MultipleVersionRange intersect(MultipleVersionRange intersection)
   {
      List<VersionRange> r1 = this.ranges;
      List<VersionRange> r2 = intersection.ranges;
      List<VersionRange> ranges;

      if (r1.isEmpty() || r2.isEmpty())
      {
         ranges = Collections.emptyList();
      }
      else
      {
         ranges = intersection(r1, r2);
      }

      if (ranges.isEmpty())
      {
         throw new VersionException("Intersected incompatible version ranges");
      }

      return new MultipleVersionRange(ranges);
   }

   private List<VersionRange> intersection(List<VersionRange> r1, List<VersionRange> r2)
   {
      List<VersionRange> ranges = new ArrayList<VersionRange>(r1.size() + r2.size());
      Iterator<VersionRange> i1 = r1.iterator();
      Iterator<VersionRange> i2 = r2.iterator();
      VersionRange res1 = i1.next();
      VersionRange res2 = i2.next();

      boolean done = false;
      while (!done)
      {
         if (res1.getMin() == null || res2.getMax() == null
                  || res1.getMin().compareTo(res2.getMax()) <= 0)
         {
            if (res1.getMax() == null || res2.getMin() == null
                     || res1.getMax().compareTo(res2.getMin()) >= 0)
            {
               Version min;
               Version max;
               boolean minInclusive;
               boolean maxInclusive;

               // overlaps
               if (res1.getMin() == null)
               {
                  min = res2.getMin();
                  minInclusive = res2.isMinInclusive();
               }
               else if (res2.getMin() == null)
               {
                  min = res1.getMin();
                  minInclusive = res1.isMinInclusive();
               }
               else
               {
                  int comparison = res1.getMin().compareTo(res2.getMin());
                  if (comparison < 0)
                  {
                     min = res2.getMin();
                     minInclusive = res2.isMinInclusive();
                  }
                  else if (comparison == 0)
                  {
                     min = res1.getMin();
                     minInclusive = res1.isMinInclusive() && res2.isMinInclusive();
                  }
                  else
                  {
                     min = res1.getMin();
                     minInclusive = res1.isMinInclusive();
                  }
               }

               if (res1.getMax() == null)
               {
                  max = res2.getMax();
                  maxInclusive = res2.isMaxInclusive();
               }
               else if (res2.getMax() == null)
               {
                  max = res1.getMax();
                  maxInclusive = res1.isMaxInclusive();
               }
               else
               {
                  int comparison = res1.getMax().compareTo(res2.getMax());
                  if (comparison < 0)
                  {
                     max = res1.getMax();
                     maxInclusive = res1.isMaxInclusive();
                  }
                  else if (comparison == 0)
                  {
                     max = res1.getMax();
                     maxInclusive = res1.isMaxInclusive() && res2.isMaxInclusive();
                  }
                  else
                  {
                     max = res2.getMax();
                     maxInclusive = res2.isMaxInclusive();
                  }
               }

               // don't add if they are equal and one is not inclusive
               if (min == null || max == null || min.compareTo(max) != 0)
               {
                  ranges.add(new DefaultVersionRange(min, minInclusive, max, maxInclusive));
               }
               else if (minInclusive && maxInclusive)
               {
                  ranges.add(new DefaultVersionRange(min, minInclusive, max, maxInclusive));
               }

               // noinspection ObjectEquality
               if (max == res2.getMax())
               {
                  // advance res2
                  if (i2.hasNext())
                  {
                     res2 = i2.next();
                  }
                  else
                  {
                     done = true;
                  }
               }
               else
               {
                  // advance res1
                  if (i1.hasNext())
                  {
                     res1 = i1.next();
                  }
                  else
                  {
                     done = true;
                  }
               }
            }
            else
            {
               // move on to next in r1
               if (i1.hasNext())
               {
                  res1 = i1.next();
               }
               else
               {
                  done = true;
               }
            }
         }
         else
         {
            // move on to next in r2
            if (i2.hasNext())
            {
               res2 = i2.next();
            }
            else
            {
               done = true;
            }
         }
      }

      return ranges;
   }

   @Override
   public String toString()
   {
      StringBuilder buf = new StringBuilder();
      for (Iterator<VersionRange> i = ranges.iterator(); i.hasNext();)
      {
         VersionRange r = i.next();

         buf.append(r.toString());

         if (i.hasNext())
         {
            buf.append(',');
         }
      }
      return buf.toString();
   }

   /**
    * Return the highest {@link Version} of the given {@link List} of versions that satisfies all {@link VersionRange}
    * instances in this {@link MultipleVersionRange}; otherwise, return <code>null</code> if no match was found.
    */
   public Version getHighestMatch(List<Version> versions)
   {
      Version matched = null;
      for (Version version : versions)
      {
         if (includes(version))
         {
            if (matched == null || version.compareTo(matched) > 0)
            {
               matched = version;
            }
         }
      }
      return matched;
   }

   public Version matchLowestMatch(List<Version> versions)
   {
      Version matched = null;
      for (Version version : versions)
      {
         if (includes(version))
         {
            if (matched == null || version.compareTo(matched) > 0)
            {
               matched = version;
            }
         }
      }
      return matched;
   }

   @Override
   public boolean includes(Version version)
   {
      for (VersionRange range : ranges)
      {
         if (range.includes(version))
         {
            return true;
         }
      }
      return false;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
      {
         return true;
      }
      if (!(obj instanceof MultipleVersionRange))
      {
         return false;
      }
      MultipleVersionRange other = (MultipleVersionRange) obj;

      boolean equals = (ranges == other.ranges)
               || ((ranges != null) && ranges.equals(other.ranges));
      return equals;
   }

   @Override
   public int hashCode()
   {
      int hash = 7;
      hash = 31 * hash + (ranges == null ? 0 : ranges.hashCode());
      return hash;
   }

   @Override
   public Version getMin()
   {
      VersionRange min = null;
      for (VersionRange range : ranges)
      {
         if (min == null || range.getMin().compareTo(min.getMin()) < 0)
         {
            min = range;
         }
      }
      return min == null ? null : min.getMin();
   }

   @Override
   public boolean isMinInclusive()
   {
      VersionRange min = null;
      for (VersionRange range : ranges)
      {
         if (min == null || range.getMin().compareTo(min.getMin()) < 0)
         {
            min = range;
         }
      }
      return min == null ? false : min.isMinInclusive();
   }

   @Override
   public Version getMax()
   {
      VersionRange max = null;
      for (VersionRange range : ranges)
      {
         if (max == null || range.getMax().compareTo(max.getMax()) > 0)
         {
            max = range;
         }
      }
      return max == null ? null : max.getMax();
   }

   @Override
   public boolean isMaxInclusive()
   {
      VersionRange max = null;
      for (VersionRange range : ranges)
      {
         if (max == null || range.getMax().compareTo(max.getMax()) > 0)
         {
            max = range;
         }
      }
      return max == null ? false : max.isMaxInclusive();
   }

   @Override
   public boolean isEmpty()
   {
      return ranges.isEmpty();
   }

   @Override
   public boolean isExact()
   {
      VersionRange seen = null;
      for (VersionRange range : ranges)
      {
         if (range.isExact())
         {
            if (seen == null)
            {
               seen = range;
            }
            else if (!seen.equals(range) && !seen.isEmpty())
            {
               return false;
            }
         }
         else
         {
            return false;
         }
      }
      return false;
   }

   @Override
   public VersionRange getIntersection(VersionRange... ranges)
   {
      return intersect(new MultipleVersionRange(ranges));
   }
}
