/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.classloader.mock;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class MockSimpleCountService
{
   private int count = 0;

   /**
    * Return's the number of times this services has been previously called.
    */
   public int execute()
   {
      return count++;
   }
}
