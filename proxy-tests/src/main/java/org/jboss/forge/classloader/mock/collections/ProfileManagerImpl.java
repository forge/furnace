/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.classloader.mock.collections;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class ProfileManagerImpl implements ProfileManager
{
   @Override
   public Map<String, Profile> getProfiles()
   {
      Map<String, Profile> map = new HashMap<>();
      map.put("demo", new Profile("demo"));
      return map;
   }

   @Override
   public void setProfiles(Collection<Profile> profiles)
   {
      for (Profile profile : profiles)
      {
         if (profile.getName() == null)
            throw new RuntimeException("FAIL");
      }
   }

}
