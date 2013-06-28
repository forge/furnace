package org.jboss.forge.furnace.versions;

import org.jboss.forge.furnace.exception.ContainerException;

public class VersionException extends ContainerException
{
   private static final long serialVersionUID = -4959545545467948898L;

   public VersionException(String message, Throwable cause)
   {
      super(message, cause);
   }

   public VersionException(String message)
   {
      super(message);
   }

}
