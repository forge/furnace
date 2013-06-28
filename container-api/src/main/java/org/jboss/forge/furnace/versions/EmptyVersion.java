package org.jboss.forge.furnace.versions;

public class EmptyVersion extends SingleVersion implements Version
{
   private EmptyVersion()
   {
      super("");
   }

   public static Version getInstance()
   {
      return new EmptyVersion();
   }

}
