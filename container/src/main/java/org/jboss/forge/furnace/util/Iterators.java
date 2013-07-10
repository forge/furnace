/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Utility methods for working with {@link Iterator} and {@link Iterable} instances.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public final class Iterators
{
   public Iterators()
   {
   }

   /**
    * Return the elements of the given {@link Iterable} as a {@link List}.
    */
   public static <T> List<T> asList(final Iterable<T> iterable)
   {
      List<T> result = new ArrayList<T>();
      for (T t : iterable)
      {
         result.add(t);
      }
      return result;
   }

   /**
    * Return the elements of the given {@link Iterator} as a {@link List}.
    */
   public static <T> List<T> asList(final Iterator<T> iterator)
   {
      List<T> result = new ArrayList<T>();
      while (iterator.hasNext())
      {
         T t = iterator.next();
         result.add(t);
      }
      return result;
   }

   /**
    * Return the elements of the given {@link Iterable} as a {@link List}.
    */
   public static <T> Set<T> asSet(final Iterable<T> iterable)
   {
      Set<T> result = new HashSet<T>();
      for (T t : iterable)
      {
         result.add(t);
      }
      return result;
   }

   /**
    * Return the elements of the given {@link Iterator} as a {@link List}.
    */
   public static <T> Set<T> asSet(final Iterator<T> iterator)
   {
      Set<T> result = new HashSet<T>();
      while (iterator.hasNext())
      {
         T t = iterator.next();
         result.add(t);
      }
      return result;
   }
}