/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.classloader.mock;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class IterableFactory
{

   public Iterable<?> getIterable()
   {
      return new ArrayList<Object>();
   }

   public Iterable<?> getCustomIterable()
   {
      return new CustomIterable();
   }

   private class CustomIterable implements Iterable<Object>
   {

      @Override
      public Iterator<Object> iterator()
      {
         return new CustomIterator();
      }

   }

   public class CustomIterator implements Iterator<Object>
   {
      @Override
      public boolean hasNext()
      {
         return false;
      }

      @Override
      public Object next()
      {
         return null;
      }

      @Override
      public void remove()
      {
      }

   }
}
