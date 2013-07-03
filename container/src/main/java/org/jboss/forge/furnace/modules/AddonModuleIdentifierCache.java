/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.modules;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.addons.AddonView;
import org.jboss.modules.ModuleIdentifier;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
class AddonModuleIdentifierCache
{
   private Map<AddonKey, ModuleIdentifier> map = new HashMap<AddonKey, ModuleIdentifier>();

   public void clear(Set<AddonView> views, AddonId addonId)
   {
      map.remove(addonId);
   }

   public ModuleIdentifier getModuleId(Set<AddonView> views, AddonId addonId)
   {
      AddonKey key = new AddonKey(views, addonId);
      if (!map.containsKey(key))
         map.put(key, ModuleIdentifier.fromString(toModuleId(addonId) + "_" + UUID.randomUUID().toString()));
      return map.get(key);
   }

   private String toModuleId(AddonId id)
   {
      return id.getName().replaceAll(":", ".") + ":" + id.getVersion();
   }

   @Override
   public String toString()
   {
      StringBuilder builder = new StringBuilder();
      Iterator<Entry<AddonKey, ModuleIdentifier>> iterator = map.entrySet().iterator();
      while (iterator.hasNext())
      {
         Entry<AddonKey, ModuleIdentifier> entry = iterator.next();
         builder.append(entry.getKey()).append(" -> ").append(entry.getValue());
         if (iterator.hasNext())
            builder.append("\n");
      }
      return builder.toString();
   }

   public class AddonKey
   {
      private Set<AddonView> views;
      private AddonId addonId;

      public AddonKey(Set<AddonView> views, AddonId addonId)
      {
         this.views = views;
         this.addonId = addonId;
      }

      @Override
      public int hashCode()
      {
         final int prime = 31;
         int result = 1;
         result = prime * result + getOuterType().hashCode();
         result = prime * result + ((addonId == null) ? 0 : addonId.hashCode());
         result = prime * result + ((views == null) ? 0 : views.hashCode());
         return result;
      }

      @Override
      public boolean equals(Object obj)
      {
         if (this == obj)
            return true;
         if (obj == null)
            return false;
         if (getClass() != obj.getClass())
            return false;
         AddonKey other = (AddonKey) obj;
         if (!getOuterType().equals(other.getOuterType()))
            return false;
         if (addonId == null)
         {
            if (other.addonId != null)
               return false;
         }
         else if (!addonId.equals(other.addonId))
            return false;
         if (views == null)
         {
            if (other.views != null)
               return false;
         }
         else if (!views.equals(other.views))
            return false;
         return true;
      }

      private AddonModuleIdentifierCache getOuterType()
      {
         return AddonModuleIdentifierCache.this;
      }

   }
}
