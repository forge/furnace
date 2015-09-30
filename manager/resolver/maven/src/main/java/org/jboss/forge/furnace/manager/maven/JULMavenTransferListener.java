/**
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.manager.maven;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferListener;
import org.eclipse.aether.transfer.TransferResource;

/**
 * Implementation of the {@link TransferListener} interface
 * 
 * @author <a href="ggastald@redhat.com">George Gastaldi</a>
 */
public class JULMavenTransferListener extends AbstractTransferListener
{
   private final Map<TransferResource, Long> downloads = new ConcurrentHashMap<TransferResource, Long>();
   private int lastLength;

   protected final Logger out;

   public JULMavenTransferListener()
   {
      this.out = Logger.getLogger(getClass().getName());
   }

   @Override
   public void transferInitiated(TransferEvent event)
   {
      String message = event.getRequestType() == TransferEvent.RequestType.PUT ? "Uploading" : "Downloading";

      out.info(message + ": " + event.getResource().getRepositoryUrl() + event.getResource().getResourceName());
   }

   @Override
   public void transferCorrupted(TransferEvent event)
            throws TransferCancelledException
   {
      TransferResource resource = event.getResource();

      out.warning(event.getException().getMessage() + " for " + resource.getRepositoryUrl()
               + resource.getResourceName());
   }

   @Override
   public void transferSucceeded(TransferEvent event)
   {
      transferCompleted(event);
      TransferResource resource = event.getResource();
      long contentLength = event.getTransferredBytes();
      if (contentLength >= 0)
      {
         String type = (event.getRequestType() == TransferEvent.RequestType.PUT ? "Uploaded" : "Downloaded");
         String len = contentLength >= 1024 ? toKB(contentLength) + " KB" : contentLength + " B";

         String throughput = "";
         long duration = System.currentTimeMillis() - resource.getTransferStartTime();
         if (duration > 0)
         {
            DecimalFormat format = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.ENGLISH));
            double kbPerSec = (contentLength / 1024.0) / (duration / 1000.0);
            throughput = " at " + format.format(kbPerSec) + " KB/sec";
         }

         out.info(type + ": " + resource.getRepositoryUrl() + resource.getResourceName() + " (" + len
                  + throughput + ")");
      }
   }

   @Override
   public void transferProgressed(TransferEvent event)
            throws TransferCancelledException
   {
      TransferResource resource = event.getResource();
      downloads.put(resource, event.getTransferredBytes());

      StringBuilder buffer = new StringBuilder(64);

      for (Map.Entry<TransferResource, Long> entry : downloads.entrySet())
      {
         long total = entry.getKey().getContentLength();
         Long complete = entry.getValue();
         // NOTE: This null check guards against http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6312056
         if (complete != null)
         {
            buffer.append(getStatus(complete.longValue(), total)).append("  ");
         }
      }

      int pad = lastLength - buffer.length();
      lastLength = buffer.length();
      pad(buffer, pad);
      buffer.append('\r');

      out.info(buffer.toString());
   }

   private String getStatus(long complete, long total)
   {
      if (total >= 1024)
      {
         return toKB(complete) + "/" + toKB(total) + " KB ";
      }
      else if (total >= 0)
      {
         return complete + "/" + total + " B ";
      }
      else if (complete >= 1024)
      {
         return toKB(complete) + " KB ";
      }
      else
      {
         return complete + " B ";
      }
   }

   private void pad(StringBuilder buffer, int spaces)
   {
      String block = "                                        ";
      while (spaces > 0)
      {
         int n = Math.min(spaces, block.length());
         buffer.append(block, 0, n);
         spaces -= n;
      }
   }

   @Override
   public void transferFailed(TransferEvent event)
   {
      transferCompleted(event);
   }

   @Override
   public void transferStarted(TransferEvent event) throws TransferCancelledException
   {
   }

   private void transferCompleted(TransferEvent event)
   {
      downloads.remove(event.getResource());

      StringBuilder buffer = new StringBuilder(64);
      pad(buffer, lastLength);
      buffer.append('\r');
      out.info(buffer.toString());
   }

   protected long toKB(long bytes)
   {
      return (bytes + 1023) / 1024;
   }

}
