/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.classloader.mock.result;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class InstanceFactoryImpl implements InstanceFactory
{
   @SuppressWarnings("unchecked")
   @Override
   public <T extends SuperInterface> T getInstance()
   {
      return (T) new Implementation();
   }

   @Override
   public Object getRawInstance()
   {
      return getInstance();
   }
}
