/**
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilities for interacting with {@link List} instances.
 *
 * @author <a href="ggastald@redhat.com">George Gastaldi</a>
 */
public class Lists
{
   /**
    * Converts a {@link Iterable} to a {@link List}
    */
   public static <T> List<T> toList(Iterable<T> iterable)
   {
      if (iterable == null)
      {
         return null;
      }
      else if (iterable instanceof List)
      {
         return (List<T>) iterable;
      }
      else
      {
         List<T> list = new ArrayList<>();
         for (T obj : iterable)
         {
            list.add(obj);
         }
         return list;
      }
   }

}
