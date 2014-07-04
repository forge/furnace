/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.classloader.mock.exceptions;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class MockException extends RuntimeException
{
   private static final long serialVersionUID = 5266075954460779189L;

   public MockException()
   {
      super();
   }

   public MockException(String message, Throwable cause)
   {
      super(message, cause);
   }

   public MockException(String message)
   {
      super(message);
   }

   public MockException(Throwable cause)
   {
      super(cause);
   }

}
