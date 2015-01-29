/**
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package test.org.jboss.forge.furnace.api;

import java.util.concurrent.Callable;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.forge.arquillian.services.LocalServices;
import org.jboss.forge.furnace.lock.LockManager;
import org.jboss.forge.furnace.lock.LockMode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
@RunWith(Arquillian.class)
public class LockManagerDeadlockTest
{
   @Deployment
   public static ForgeArchive getDeployment()
   {
      ForgeArchive archive = ShrinkWrap.create(ForgeArchive.class)
               .addAsLocalServices(LockManagerDeadlockTest.class);

      return archive;
   }

   @Test
   public void testIsSameThreadDeadlockReported()
   {
      final LockManager lock = LocalServices.getFurnace(LockManagerDeadlockTest.class.getClassLoader())
               .getLockManager();
      Assert.assertTrue(lock.performLocked(LockMode.READ, new Callable<Boolean>()
      {
         @Override
         public Boolean call() throws Exception
         {
            try
            {
               return lock.performLocked(LockMode.WRITE, new Callable<Boolean>()
               {
                  @Override
                  public Boolean call() throws Exception
                  {
                     return false;
                  }
               });
            }
            catch (Error e)
            {
               if (e.getClass().getSimpleName().equals("DeadlockError"))
                  return true;
               else
                  return false;
            }
         }
      }));
   }
}
