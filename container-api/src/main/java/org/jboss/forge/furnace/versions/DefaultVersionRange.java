/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.versions;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class DefaultVersionRange implements VersionRange
{
   private final Version min;
   private final Version max;
   private final boolean minInclusive;
   private final boolean maxInclusive;

   public static final DefaultVersionRange EVERYTHING = new DefaultVersionRange(null, false, null, false);

   public DefaultVersionRange(Version min, boolean minInclusive, Version max,
            boolean maxInclusive)
   {
      this.min = min;
      this.minInclusive = minInclusive;
      this.max = max;
      this.maxInclusive = maxInclusive;
   }

   @Override
   public Version getMin()
   {
      return min;
   }

   @Override
   public boolean isMinInclusive()
   {
      return minInclusive;
   }

   @Override
   public Version getMax()
   {
      return max;
   }

   @Override
   public boolean isMaxInclusive()
   {
      return maxInclusive;
   }

   @Override
   public boolean isEmpty()
   {
      return (min.compareTo(max) == 0) && !minInclusive && !maxInclusive;
   }

   @Override
   public boolean isExact()
   {
      return min.equals(max);
   }

   @Override
   public VersionRange getIntersection(VersionRange... ranges)
   {
      List<VersionRange> list = new ArrayList<VersionRange>();
      for (VersionRange range : ranges)
      {
         list.add(range);
      }
      MultipleVersionRange intersection = new MultipleVersionRange(list);
      return new DefaultVersionRange(
               intersection.getMin(),
               intersection.isMinInclusive(),
               intersection.getMax(),
               intersection.isMaxInclusive());
   }

   @Override
   public boolean includes(Version version)
   {
      if (min != null)
      {
         int comparison = min.compareTo(version);

         if ((comparison == 0) && !minInclusive)
         {
            return false;
         }
         if (comparison > 0)
         {
            return false;
         }
      }
      if (max != null)
      {
         int comparison = max.compareTo(version);

         if ((comparison == 0) && !maxInclusive)
         {
            return false;
         }
         if (comparison < 0)
         {
            return false;
         }
      }

      return true;
   }

   @Override
   public int hashCode()
   {
      int result = 13;

      if (min == null)
      {
         result += 1;
      }
      else
      {
         result += min.hashCode();
      }

      result *= minInclusive ? 1 : 2;

      if (max == null)
      {
         result -= 3;
      }
      else
      {
         result -= max.hashCode();
      }

      result *= maxInclusive ? 2 : 3;

      return result;
   }

   @Override
   public boolean equals(Object other)
   {
      if (this == other)
      {
         return true;
      }

      if (!(other instanceof DefaultVersionRange))
      {
         return false;
      }

      DefaultVersionRange restriction = (DefaultVersionRange) other;
      if (min != null)
      {
         if (!min.equals(restriction.min))
         {
            return false;
         }
      }
      else if (restriction.min != null)
      {
         return false;
      }

      if (minInclusive != restriction.minInclusive)
      {
         return false;
      }

      if (max != null)
      {
         if (!max.equals(restriction.max))
         {
            return false;
         }
      }
      else if (restriction.max != null)
      {
         return false;
      }

      if (maxInclusive != restriction.maxInclusive)
      {
         return false;
      }

      return true;
   }

   @Override
   public String toString()
   {
      StringBuilder buf = new StringBuilder();

      buf.append(isMinInclusive() ? "[" : "(");
      if (getMin() != null)
      {
         buf.append(getMin().toString());
      }
      buf.append(",");
      if (getMax() != null)
      {
         buf.append(getMax().toString());
      }
      buf.append(isMaxInclusive() ? "]" : ")");

      return buf.toString();
   }
}