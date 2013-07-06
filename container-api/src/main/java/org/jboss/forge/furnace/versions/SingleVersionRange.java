/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.versions;

import org.jboss.forge.furnace.util.Assert;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class SingleVersionRange implements VersionRange
{
   private Version version;

   public SingleVersionRange(Version version)
   {
      Assert.notNull(version, "Version must not be null.");
      Assert.notNull(version.toString(), "Version must not be null.");
      if (version.toString().isEmpty())
         throw new IllegalArgumentException("Version must not be empty.");

      this.version = version;
   }

   @Override
   public boolean isEmpty()
   {
      return false;
   }

   @Override
   public boolean isExact()
   {
      return true;
   }

   @Override
   public Version getMin()
   {
      return version;
   }

   @Override
   public Version getMax()
   {
      return version;
   }

   @Override
   public boolean includes(Version version)
   {
      return version != null && this.version.equals(version);
   }

   @Override
   public VersionRange getIntersection(VersionRange... ranges)
   {
      for (VersionRange range : ranges)
      {
         if (range.includes(version))
            return this;
      }
      return new EmptyVersionRange();
   }

   @Override
   public boolean isMaxInclusive()
   {
      return true;
   }

   @Override
   public boolean isMinInclusive()
   {
      return true;
   }

   @Override
   public String toString()
   {
      return version.toString();
   }

   @Override
   public int hashCode()
   {
      int result = 13;

      if (getMin() == null)
      {
         result += 1;
      }
      else
      {
         result += getMin().hashCode();
      }

      result *= isMinInclusive() ? 1 : 2;

      if (getMax() == null)
      {
         result -= 3;
      }
      else
      {
         result -= getMax().hashCode();
      }

      result *= isMaxInclusive() ? 2 : 3;

      return result;
   }

   @Override
   public boolean equals(Object other)
   {
      if (this == other)
      {
         return true;
      }

      if (!(other instanceof VersionRange))
      {
         return false;
      }

      VersionRange restriction = (VersionRange) other;
      if (getMin() != null)
      {
         if (!getMin().equals(restriction.getMin()))
         {
            return false;
         }
      }
      else if (restriction.getMin() != null)
      {
         return false;
      }

      if (isMinInclusive() != restriction.isMinInclusive())
      {
         return false;
      }

      if (getMax() != null)
      {
         if (!getMax().equals(restriction.getMax()))
         {
            return false;
         }
      }
      else if (restriction.getMax() != null)
      {
         return false;
      }

      if (isMaxInclusive() != restriction.isMaxInclusive())
      {
         return false;
      }

      return true;
   }

}
