package org.jboss.forge.furnace;

import java.io.File;

import org.jboss.forge.furnace.impl.FurnaceImpl;
import org.jboss.forge.furnace.repositories.AddonRepositoryMode;
import org.junit.Test;

public class FurnaceImplTest
{
   @Test(expected = IllegalArgumentException.class)
   public void shouldValidateAddRepositoryArgumentMode() throws Exception {
      Furnace f = new FurnaceImpl();
      f.addRepository(null, new File("."));
   }

   @Test(expected = IllegalArgumentException.class)
   public void shouldValidateAddRepositoryArgumentDirectory() throws Exception {
      Furnace f = new FurnaceImpl();
      f.addRepository(AddonRepositoryMode.IMMUTABLE, null);
   }
}
