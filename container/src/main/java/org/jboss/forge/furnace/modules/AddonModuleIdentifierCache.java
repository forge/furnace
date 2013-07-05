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
import java.util.UUID;

import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.modules.ModuleIdentifier;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
class AddonModuleIdentifierCache
{
   private Map<Addon, ModuleIdentifier> map = new HashMap<Addon, ModuleIdentifier>();

   public void clear(Addon addon)
   {
      map.remove(addon);
   }

   public ModuleIdentifier getModuleId(Addon addon)
   {
      if (!map.containsKey(addon))
         map.put(addon, ModuleIdentifier.fromString(toModuleId(addon.getId()) + "_" + UUID.randomUUID().toString()));
      return map.get(addon);
   }

   private String toModuleId(AddonId id)
   {
      return id.getName().replaceAll(":", ".") + ":" + id.getVersion();
   }

   @Override
   public String toString()
   {
      StringBuilder builder = new StringBuilder();
      Iterator<Entry<Addon, ModuleIdentifier>> iterator = map.entrySet().iterator();
      while (iterator.hasNext())
      {
         Entry<Addon, ModuleIdentifier> entry = iterator.next();
         builder.append(entry.getKey()).append(" -> ").append(entry.getValue());
         if (iterator.hasNext())
            builder.append("\n");
      }
      return builder.toString();
   }

   public Addon getAddon(ModuleIdentifier id)
   {
      for (Entry<Addon, ModuleIdentifier> entry : map.entrySet())
      {
         if (entry.getValue().equals(id))
         {
            return entry.getKey();
         }
      }
      return null;
   }

}
