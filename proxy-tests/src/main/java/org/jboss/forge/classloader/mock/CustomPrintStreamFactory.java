/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.classloader.mock;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import org.jboss.forge.furnace.proxy.Proxies;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class CustomPrintStreamFactory
{
   public PrintStream getPrintStream()
   {
      return getCustomPrintStream();
   }

   public CustomPrintStream getCustomPrintStream()
   {
      try
      {
         File file = File.createTempFile("furnace", "printStreamTest");
         file.deleteOnExit();
         return new CustomPrintStream(file);
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }
   }

   public void usePrintStream(PrintStream stream)
   {
      if (stream == null)
         throw new IllegalArgumentException("Should have been a File");
      if (Proxies.isForgeProxy(stream))
         throw new IllegalArgumentException("Should not have been a proxy");
   }
}
