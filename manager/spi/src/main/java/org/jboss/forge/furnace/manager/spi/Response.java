/**
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.manager.spi;

import java.util.List;

/**
 * A response object that returns the exceptions that may have happened while performing an operation
 * 
 * @author <a href="ggastald@redhat.com">George Gastaldi</a>
 */
public interface Response<TYPE>
{
   /**
    * The wrapped result object
    */
   TYPE get();

   /**
    * A {@link List} of {@link Exception} that occurred during invocation.
    * 
    * Should be logged
    */
   List<Exception> getExceptions();
}
