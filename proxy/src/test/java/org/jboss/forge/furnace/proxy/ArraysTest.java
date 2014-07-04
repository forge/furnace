/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.proxy;

import org.jboss.forge.furnace.proxy.Arrays;
import org.junit.Assert;
import org.junit.Test;

public class ArraysTest
{

   @Test
   public void testSingleValue()
   {
      String[] data = new String[] { "a" };

      int index = 0;
      String[] result = Arrays.removeElementAtIndex(data, index);
      Assert.assertEquals(1, data.length);
      Assert.assertEquals(0, result.length);
   }

   @Test
   public void testRemoveElementAtIndex()
   {
      String[] data = new String[] { "a", "b", "c", "d", "e" };

      int index = 2;
      String[] result = Arrays.removeElementAtIndex(data, index);
      Assert.assertEquals(data[0], result[0]);
      Assert.assertEquals(data[1], result[1]);
      Assert.assertEquals(data[3], result[2]);
      Assert.assertEquals(data[4], result[3]);
   }

   @Test
   public void testRemoveElementAtLastIndex()
   {
      String[] data = new String[] { "a", "b", "c", "d", "e" };

      int index = 4;
      String[] result = Arrays.removeElementAtIndex(data, index);
      Assert.assertEquals(data[0], result[0]);
      Assert.assertEquals(data[1], result[1]);
      Assert.assertEquals(data[2], result[2]);
      Assert.assertEquals(data[3], result[3]);

      Assert.assertEquals(4, result.length);
   }

}
