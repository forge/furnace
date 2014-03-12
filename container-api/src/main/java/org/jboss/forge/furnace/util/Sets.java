package org.jboss.forge.furnace.util;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utilities for interacting with {@link Set} instances.
 *
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class Sets
{
   /**
    * Get a new instance of a concurrent {@link Set} (implemented by {@link ConcurrentHashMap}).
    */
   public static <T> Set<T> getConcurrentSet(Class<T> type)
   {
      return Collections.newSetFromMap(new ConcurrentHashMap<T, Boolean>());
   }

   /**
    * Get a new instance of a concurrent {@link Set} (implemented by {@link ConcurrentHashMap}).
    */
   public static <T> Set<T> getConcurrentSet()
   {
      return Collections.newSetFromMap(new ConcurrentHashMap<T, Boolean>());
   }

   /**
    * Converts an {@link Iterable} to a {@link Set}
    */
   public static <T> Set<T> toSet(Iterable<T> iterable)
   {
      if (iterable == null)
      {
         return null;
      }
      else if (iterable instanceof Set)
      {
         return (Set<T>) iterable;
      }
      else
      {
         Set<T> list = new LinkedHashSet<>();
         for (T obj : iterable)
         {
            list.add(obj);
         }
         return list;
      }
   }

}