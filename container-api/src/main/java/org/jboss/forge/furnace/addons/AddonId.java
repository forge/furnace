/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.addons;

import java.util.Arrays;
import java.util.List;

import org.jboss.forge.furnace.util.Assert;
import org.jboss.forge.furnace.versions.EmptyVersion;
import org.jboss.forge.furnace.versions.SingleVersion;
import org.jboss.forge.furnace.versions.Version;

/**
 * Represents the ID of an {@link Addon}.
 *
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class AddonId implements Comparable<AddonId>
{
   private String name;
   private Version apiVersion;
   private Version version;

   /**
    * Get the name of this {@link AddonId}.
    */
   public String getName()
   {
      return name;
   }

   /**
    * Get the API {@link Version} of this {@link AddonId}.
    */
   public Version getApiVersion()
   {
      return apiVersion;
   }

   /**
    * Get the {@link Version} of this {@link AddonId}.
    */
   public Version getVersion()
   {
      return version;
   }

   @Override
   public String toString()
   {
      return toCoordinates();
   }

   /**
    * Used by the converter addon
    */
   public static AddonId valueOf(String coordinates)
   {
      return fromCoordinates(coordinates);
   }

   /**
    * Attempt to parse the given string as {@link Addon} coordinates in the form: "group:name,version"
    *
    * @throws IllegalArgumentException when coordinates are malformed.
    */
   public static AddonId fromCoordinates(final String coordinates) throws IllegalArgumentException
   {
      String[] split = coordinates.split(",");
      List<String> tokens = Arrays.asList(split);

      if (tokens.size() < 2 || tokens.size() > 4)
      {
         throw new IllegalArgumentException(
                  "Coordinates must be of the form 'name,version' or 'name,version,api-version");
      }

      if (tokens.size() >= 3)
      {
         if (tokens.get(2) == null || tokens.get(2).isEmpty())
            throw new IllegalArgumentException("API version was empty [" + coordinates + "]");

         if (tokens.get(1).startsWith("[") | tokens.get(1).startsWith("(")
                  && tokens.get(2).endsWith("]") | tokens.get(2).endsWith(")"))
         {
            if (tokens.size() == 3)
               return from(tokens.get(0), tokens.get(1) + "," + tokens.get(2));
            else if (tokens.size() == 4)
               return from(tokens.get(0), tokens.get(1) + "," + tokens.get(2), tokens.get(3));
         }

         if (tokens.size() > 3)
            throw new IllegalArgumentException(
                     "Coordinates must be of the form 'name,version' or 'name,version,api-version");

         return from(tokens.get(0), tokens.get(1), tokens.get(2));
      }
      return from(tokens.get(0), tokens.get(1));
   }

   /**
    * Create an {@link AddonId} from the given name and version.
    */
   public static AddonId from(String name, String version)
   {
      return from(name, version, null);
   }

   /**
    * Create an {@link AddonId} from the given name and {@link Version}.
    */
   public static AddonId from(String name, Version version)
   {
      return from(name, version, null);
   }

   /**
    * Create an {@link AddonId} from the given name, {@link Version}, and API {@link Version}.
    */
   public static AddonId from(String name, Version version, Version apiVersion)
   {
      Assert.notNull(name, "Name cannot be null.");
      if (name.trim().isEmpty())
         throw new IllegalArgumentException("Name cannot be empty.");
      Assert.notNull(version, "Version cannot be null.");
      if (version.toString().trim().isEmpty())
         throw new IllegalArgumentException("Version cannot be empty.");

      AddonId id = new AddonId();
      id.name = name;
      id.version = version;
      if (apiVersion == null || apiVersion.toString().trim().isEmpty())
         id.apiVersion = EmptyVersion.getInstance();
      else
         id.apiVersion = apiVersion;
      return id;
   }

   /**
    * Create an {@link AddonId} from the given name, version, and API version.
    */
   public static AddonId from(String name, String version, String apiVersion)
   {
      Assert.notNull(name, "Name cannot be null.");
      if (name.trim().isEmpty())
         throw new IllegalArgumentException("Name cannot be empty.");
      Assert.notNull(version, "Version cannot be null.");
      if (version.trim().isEmpty())
         throw new IllegalArgumentException("Version cannot be empty.");

      AddonId id = new AddonId();

      id.name = name;
      id.version = SingleVersion.valueOf(version);
      if (apiVersion == null || apiVersion.trim().isEmpty())
         id.apiVersion = EmptyVersion.getInstance();
      else
         id.apiVersion = SingleVersion.valueOf(apiVersion);

      return id;

   }

   /**
    * The name and version, comma separated.
    */
   public String toCoordinates()
   {
      StringBuilder coord = new StringBuilder(getName()).append(",").append(getVersion());
      return coord.toString();
   }

   @Override
   public int hashCode()
   {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + ((version == null) ? 0 : version.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (!(obj instanceof AddonId))
         return false;
      AddonId other = (AddonId) obj;
      if (name == null)
      {
         if (other.getName() != null)
            return false;
      }
      else if (!name.equals(other.getName()))
         return false;
      if (version == null)
      {
         if (other.getVersion() != null)
            return false;
      }
      else if (!version.equals(other.getVersion()))
         return false;
      return true;
   }

   @Override
   public int compareTo(AddonId other)
   {
      if (other == null)
         throw new IllegalArgumentException("Cannot compare against null.");

      int result = getName().compareTo(other.getName());

      if (result == 0)
         result = getVersion().compareTo(other.getVersion());

      if (result == 0)
         result = getApiVersion().compareTo(other.getApiVersion());

      return result;
   }

}