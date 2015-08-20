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
import java.util.List;

import org.jboss.forge.furnace.util.Assert;

/**
 * Default implementation of artifact versioning.
 * 
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * @author <a href="mailto:gegastaldi@gmail.com">George Gastaldi</a>
 */
public class SingleVersion implements Version
{
   private String version;
   private int majorVersion;
   private int minorVersion;
   private int incrementalVersion;
   private int buildNumber;
   private String qualifier;

   private ComparableVersion comparable;

   public SingleVersion(String version)
   {
      parseVersion(version);
   }

   @Override
   public int hashCode()
   {
      return 11 + comparable.hashCode();
   }

   @Override
   public boolean equals(Object other)
   {
      if (this == other)
      {
         return true;
      }

      if (!(other instanceof Version))
      {
         return false;
      }

      return compareTo((Version) other) == 0;
   }

   @Override
   public int compareTo(Version otherVersion)
   {
      if (otherVersion == null)
         throw new NullPointerException("Cannot compare against null.");

      if (otherVersion instanceof SingleVersion)
      {
         return this.comparable.compareTo(((SingleVersion) otherVersion).comparable);
      }
      else
      {
         return compareTo(new SingleVersion(otherVersion.toString()));
      }
   }

   @Override
   public int getMajorVersion()
   {
      return majorVersion != null ? majorVersion : 0;
   }

   @Override
   public int getMinorVersion()
   {
      return minorVersion != null ? minorVersion : 0;
   }

   @Override
   public int getIncrementalVersion()
   {
      return incrementalVersion != null ? incrementalVersion : 0;
   }

   @Override
   public int getBuildNumber()
   {
      return buildNumber != null ? buildNumber : 0;
   }

   @Override
   public String getQualifier()
   {
      return qualifier;
   }

   private final void parseVersion(String version)
   {
      Assert.notNull(version, "Version must not be null.");
      this.version = version.trim();
      comparable = new ComparableVersion(this.version);

      List<String> parts = new ArrayList<>(Arrays.asList(this.version.split("[\\._-]")));

      try
      {
         this.majorVersion = Integer.parseInt(parts.remove(0));
         if (!parts.isEmpty())
            minorVersion = Integer.parseInt(parts.remove(0));
         if (!parts.isEmpty())
            incrementalVersion = Integer.parseInt(parts.remove(0));
         if (!parts.isEmpty())
         {
            qualifier = parts.remove(0);
            try
            {
               if (parts.isEmpty())
               {
                  buildNumber = Integer.parseInt(qualifier);
                  qualifier = null;
               }
            }
            catch (NumberFormatException e)
            {
               // qualifier is not a build number, carry on.
            }
         }
         if (!parts.isEmpty())
            buildNumber = Integer.parseInt(parts.remove(0));
      }
      catch (NumberFormatException e)
      {
         // It's probably just a literal version, so... give up and use it "as is."
         majorVersion = 0;
         minorVersion = 0;
         incrementalVersion = 0;
         buildNumber = 0;
         qualifier = null;
      }
   }

   @Override
   public String toString()
   {
      return version;
   }
}
