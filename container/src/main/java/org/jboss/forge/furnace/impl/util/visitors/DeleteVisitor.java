/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.impl.util.visitors;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * @author <a href="mailto:danielsoro@gmail.com">Daniel Cunha (soro)</a>
 *
 */
public class DeleteVisitor extends SimpleFileVisitor<Path>
{
   @Override
   public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
   {
      Files.delete(file);
      return FileVisitResult.CONTINUE;
   }

   @Override
   public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
   {
      Files.delete(dir);
      return FileVisitResult.CONTINUE;
   }
}
