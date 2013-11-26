/**
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.manager.maven.result;

import java.util.List;

import org.jboss.forge.furnace.manager.spi.Response;

/**
 * Builds {@link Response} objects
 * 
 * @author <a href="ggastald@redhat.com">George Gastaldi</a>
 */
public class MavenResponseBuilder<TYPE> implements Response<TYPE>
{
   private final TYPE result;
   private List<Exception> exceptions;

   public MavenResponseBuilder(TYPE result)
   {
      this.result = result;
   }

   /**
    * @return the result
    */
   public TYPE get()
   {
      return result;
   }

   /**
    * @return the exceptions
    */
   public List<Exception> getExceptions()
   {
      return exceptions;
   }

   /**
    * @param exceptions the exceptions to set
    */
   public MavenResponseBuilder<TYPE> setExceptions(List<Exception> exceptions)
   {
      this.exceptions = exceptions;
      return this;
   }

}
