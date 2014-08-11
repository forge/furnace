/**
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.impl.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A {@link Future} that simply throws an exception when get() is called
 *
 * @author <a href="ggastald@redhat.com">George Gastaldi</a>
 */
public class ExceptionFuture<T> implements Future<T>
{
   private final Throwable cause;

   public ExceptionFuture(Throwable cause)
   {
      super();
      this.cause = cause;
   }

   @Override
   public boolean cancel(boolean mayInterruptIfRunning)
   {
      return false;
   }

   @Override
   public boolean isCancelled()
   {
      return false;
   }

   @Override
   public boolean isDone()
   {
      return false;
   }

   @Override
   public T get() throws InterruptedException, ExecutionException
   {
      throw new ExecutionException(cause);
   }

   @Override
   public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
   {
      throw new ExecutionException(cause);
   }

}
