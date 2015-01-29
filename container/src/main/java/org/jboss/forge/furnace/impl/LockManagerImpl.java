/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.impl;

import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jboss.forge.furnace.exception.ContainerException;
import org.jboss.forge.furnace.lock.DeadlockError;
import org.jboss.forge.furnace.lock.LockManager;
import org.jboss.forge.furnace.lock.LockMode;
import org.jboss.forge.furnace.util.Assert;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class LockManagerImpl implements LockManager
{
   private ReentrantReadWriteLock readWriteLock;

   private synchronized Lock obtainLock(LockMode mode)
   {
      if (readWriteLock == null)
         readWriteLock = new ReentrantReadWriteLock(true);

      if (LockMode.READ.equals(mode))
      {
         return readWriteLock.readLock();
      }
      else
      {
         if (readWriteLock.getReadHoldCount() > 0)
            throw new DeadlockError(
                     "Thread with READ lock attempted to obtain a WRITE lock (This is never allowed.)");
         return readWriteLock.writeLock();
      }
   }

   @Override
   public <T> T performLocked(LockMode mode, Callable<T> task)
   {
      Assert.notNull(mode, "LockMode must not be null.");
      Assert.notNull(task, "Task to perform must not be null.");

      Lock lock = obtainLock(mode);
      lock.lock();

      T result;
      try
      {
         result = task.call();
      }
      catch (RuntimeException e)
      {
         throw e;
      }
      catch (Exception e)
      {
         throw new ContainerException("Failed to execute task in LockMode [" + mode + "]", e);
      }
      finally
      {
         lock.unlock();
      }
      return result;
   }

}
