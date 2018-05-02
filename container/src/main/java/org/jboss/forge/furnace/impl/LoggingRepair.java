/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.forge.furnace.util.OperatingSystemUtils;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
class LoggingRepair
{
   public static Logger log = Logger.getLogger(LoggingRepair.class.getName());

   public static void init()
   {
      if (!OperatingSystemUtils.isJava8())
      {
         return;
      }

      AccessController.doPrivileged(new PrivilegedAction<Void>()
      {
         @Override
         @SuppressWarnings({ "unchecked", "rawtypes" })
         public Void run()
         {
            /*
             * This mysterious-looking hack is designed to trick JDK logging into not leaking classloaders and so forth
             * when adding levels, by simply shutting down the craptastic level name "registry" that it keeps.
             */
            final Class<Level> levelClass = Level.class;
            try
            {
               synchronized (levelClass)
               {
                  final Field knownField = levelClass.getDeclaredField("known");
                  knownField.setAccessible(true);
                  final List<Level> old = (List<Level>) knownField.get(null);
                  if (!(old instanceof ReadOnlyArrayList))
                  {
                     knownField.set(null, new ReadOnlyArrayList<>(Arrays.asList(
                              Level.ALL,
                              Level.FINEST,
                              Level.FINER,
                              Level.FINE,
                              Level.INFO,
                              Level.CONFIG,
                              Level.WARNING,
                              Level.SEVERE,
                              Level.OFF
                     )));
                  }
               }
            }
            catch (Throwable e)
            {
               // meh
            }

            try
            {
               Class<?> knownLevelClass = Class.forName("java.util.logging.Level$KnownLevel");
               synchronized (knownLevelClass)
               {
                  Constructor<?> constructor = knownLevelClass.getDeclaredConstructor(Level.class);
                  constructor.setAccessible(true);

                  final Field knownNameField = knownLevelClass.getDeclaredField("nameToLevels");
                  knownNameField.setAccessible(true);
                  final Map oldNames = (Map) knownNameField.get(null);
                  if (!(oldNames instanceof ReadOnlyHashMap))
                  {
                     knownNameField.set(null, new ReadOnlyHashMap(
                              Arrays.asList(new ReadOnlyMapEntry(Level.ALL.getName(),
                                                new ReadOnlyArrayList<>(Arrays.asList(constructor.newInstance(Level.ALL)))),
                                       new ReadOnlyMapEntry(Level.FINEST.getName(), new ReadOnlyArrayList<>(
                                                Arrays.asList(constructor.newInstance(Level.FINEST)))),
                                       new ReadOnlyMapEntry(Level.FINER.getName(), new ReadOnlyArrayList<>(
                                                Arrays.asList(constructor.newInstance(Level.FINER)))),
                                       new ReadOnlyMapEntry(Level.FINE.getName(), new ReadOnlyArrayList<>(
                                                Arrays.asList(constructor.newInstance(Level.FINE)))),
                                       new ReadOnlyMapEntry(Level.INFO.getName(), new ReadOnlyArrayList<>(
                                                Arrays.asList(constructor.newInstance(Level.INFO)))),
                                       new ReadOnlyMapEntry(Level.CONFIG.getName(), new ReadOnlyArrayList<>(
                                                Arrays.asList(constructor.newInstance(Level.CONFIG)))),
                                       new ReadOnlyMapEntry(Level.WARNING.getName(), new ReadOnlyArrayList<>(
                                                Arrays.asList(constructor.newInstance(Level.WARNING)))),
                                       new ReadOnlyMapEntry(Level.SEVERE.getName(), new ReadOnlyArrayList<>(
                                                Arrays.asList(constructor.newInstance(Level.SEVERE)))),
                                       new ReadOnlyMapEntry(Level.OFF.getName(), new ReadOnlyArrayList<>(
                                                Arrays.asList(constructor.newInstance(Level.OFF))))
                              )));
                  }

                  final Field knownIntField = knownLevelClass.getDeclaredField("intToLevels");
                  knownIntField.setAccessible(true);
                  final Map oldInts = (Map) knownIntField.get(null);
                  if (!(oldInts instanceof ReadOnlyHashMap))
                  {
                     knownIntField.set(null, new ReadOnlyHashMap(
                              Arrays.asList(new ReadOnlyMapEntry(Level.ALL.intValue(),
                                                new ReadOnlyArrayList<>(Arrays.asList(constructor.newInstance(Level.ALL)))),
                                       new ReadOnlyMapEntry(Level.FINEST.intValue(), new ReadOnlyArrayList(
                                                Arrays.asList(constructor.newInstance(Level.FINEST)))),
                                       new ReadOnlyMapEntry(Level.FINER.intValue(), new ReadOnlyArrayList(
                                                Arrays.asList(constructor.newInstance(Level.FINER)))),
                                       new ReadOnlyMapEntry(Level.FINE.intValue(), new ReadOnlyArrayList(
                                                Arrays.asList(constructor.newInstance(Level.FINE)))),
                                       new ReadOnlyMapEntry(Level.INFO.intValue(), new ReadOnlyArrayList(
                                                Arrays.asList(constructor.newInstance(Level.INFO)))),
                                       new ReadOnlyMapEntry(Level.CONFIG.intValue(), new ReadOnlyArrayList(
                                                Arrays.asList(constructor.newInstance(Level.CONFIG)))),
                                       new ReadOnlyMapEntry(Level.WARNING.intValue(), new ReadOnlyArrayList(
                                                Arrays.asList(constructor.newInstance(Level.WARNING)))),
                                       new ReadOnlyMapEntry(Level.SEVERE.intValue(), new ReadOnlyArrayList(
                                                Arrays.asList(constructor.newInstance(Level.SEVERE)))),
                                       new ReadOnlyMapEntry(Level.OFF.intValue(), new ReadOnlyArrayList(
                                                Arrays.asList(constructor.newInstance(Level.OFF))))
                              )));
                  }
               }
            }
            catch (Throwable e)
            {
               // meh
            }
            return null;
         }
      });
   }

   private static final class ReadOnlyArrayList<T> extends ArrayList<T>
   {

      private static final long serialVersionUID = -6048215349511680936L;

      private ReadOnlyArrayList(final Collection<? extends T> c)
      {
         super(c);
      }

      @Override
      public void add(int index, T element)
      {
         // ignore
      }

      @Override
      public boolean add(T e)
      {
         // ignore
         return false;
      }

      @Override
      public boolean addAll(Collection<? extends T> c)
      {
         // ignore
         return false;
      }

      @Override
      public boolean addAll(int index, Collection<? extends T> c)
      {
         // ignore
         return false;
      }

      @Override
      public T set(final int index, final T element)
      {
         // ignore
         return null;
      }

      @Override
      public T remove(final int index)
      {
         // ignore
         return null;
      }

      @Override
      public boolean remove(final Object o)
      {
         // ignore
         return false;
      }

      @Override
      public void clear()
      {
         // ignore
      }

      @Override
      protected void removeRange(final int fromIndex, final int toIndex)
      {
         // ignore
      }

      @Override
      public Iterator<T> iterator()
      {
         final Iterator<T> superIter = super.iterator();
         return new Iterator<T>()
         {
            @Override
            public boolean hasNext()
            {
               return superIter.hasNext();
            }

            @Override
            public T next()
            {
               return superIter.next();
            }

            @Override
            public void remove()
            {
               // ignore
            }
         };
      }

      @Override
      public ListIterator<T> listIterator(final int index)
      {
         final ListIterator<T> superIter = super.listIterator(index);
         return new ListIterator<T>()
         {
            @Override
            public boolean hasNext()
            {
               return superIter.hasNext();
            }

            @Override
            public T next()
            {
               return superIter.next();
            }

            @Override
            public boolean hasPrevious()
            {
               return superIter.hasPrevious();
            }

            @Override
            public T previous()
            {
               return superIter.previous();
            }

            @Override
            public int nextIndex()
            {
               return superIter.nextIndex();
            }

            @Override
            public int previousIndex()
            {
               return superIter.previousIndex();
            }

            @Override
            public void remove()
            {
               // ignore
            }

            @Override
            public void set(final T o)
            {
               // ignore
            }

            @Override
            public void add(final T o)
            {
               // ignore
            }
         };
      }

      @Override
      public boolean removeAll(final Collection<?> c)
      {
         // ignore
         return false;
      }

      @Override
      public boolean retainAll(final Collection<?> c)
      {
         // ignore
         return false;
      }
   }

   public static class ReadOnlyHashMap<K, V> extends HashMap<K, V> implements Map<K, V>
   {
      private static final long serialVersionUID = 511696823116226682L;

      public ReadOnlyHashMap(List<ReadOnlyMapEntry<K, V>> entries)
      {
         for (ReadOnlyMapEntry<K, V> entry : entries)
         {
            super.put(entry.getKey(), entry.getValue());
         }
      }

      @Override
      public void clear()
      {
         // ignore
      }

      @Override
      public V put(K key, V value)
      {
         // ignore
         return null;
      }

      @Override
      public void putAll(Map<? extends K, ? extends V> m)
      {
         // ignore
      }

      @Override
      public V remove(Object key)
      {
         // ignore
         return null;
      }

      @Override
      public Collection<V> values()
      {
         return new ReadOnlyArrayList<>(super.values());
      }
   }

   private static class ReadOnlyMapEntry<K, V> implements Entry<K, V>
   {
      private final V value;
      private final K key;

      public ReadOnlyMapEntry(K key, V value)
      {
         this.key = key;
         this.value = value;
      }

      @Override
      public K getKey()
      {
         return key;
      }

      @Override
      public V getValue()
      {
         return value;
      }

      @Override
      public V setValue(V value)
      {
         // ignore
         return null;
      }
   }
}
