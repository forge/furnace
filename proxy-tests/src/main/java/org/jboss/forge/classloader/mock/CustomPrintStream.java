/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.classloader.mock;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class CustomPrintStream extends PrintStream
{
   public CustomPrintStream(File etc) throws FileNotFoundException
   {
      super(etc);
   }
}
