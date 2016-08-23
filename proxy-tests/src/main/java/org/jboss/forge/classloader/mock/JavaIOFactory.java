/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.classloader.mock;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jboss.forge.furnace.proxy.Proxies;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class JavaIOFactory
{

   public Path getPath()
   {
      return Paths.get("/foo");
   }

   public File getFile()
   {
      return new File("/foo");
   }

   public void useFile(File file)
   {
      if (file == null)
         throw new IllegalArgumentException("Should have been a File");
      if (!file.getClass().equals(File.class))
         throw new IllegalArgumentException("Should have been a File class");
      if (Proxies.isForgeProxy(file))
         throw new IllegalArgumentException("Should not have been a proxy");
   }
}
