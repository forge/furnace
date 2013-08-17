/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package test.org.jboss.forge.furnace.lifecycle;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.jboss.forge.furnace.event.EventException;
import org.jboss.forge.furnace.event.EventManager;
import org.jboss.forge.furnace.event.PostStartup;
import org.jboss.forge.furnace.event.PreShutdown;
import org.jboss.forge.furnace.services.Exported;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
@Exported
public class RecordingEventManager implements EventManager
{
   private static List<Object> events = new ArrayList<Object>();
   private static List<Annotation[]> qualifiers = new ArrayList<Annotation[]>();
   private static int postStartupCount;
   private static int preShutdownCount;

   @Override
   public void fireEvent(Object event, Annotation... qualifiers) throws EventException
   {
      if (event instanceof PostStartup)
         postStartupCount++;
      if (event instanceof PreShutdown)
         preShutdownCount++;

      RecordingEventManager.events.add(event);
      RecordingEventManager.qualifiers.add(qualifiers);
   }

   public List<Object> getEvents()
   {
      return RecordingEventManager.events;
   }

   public List<Annotation[]> getQualifiers()
   {
      return RecordingEventManager.qualifiers;
   }

   public int getPostStartupCount()
   {
      return postStartupCount;
   }

   public int getPreShutdownCount()
   {
      return preShutdownCount;
   }

   @Override
   public String toString()
   {
      return "RecordingEventManager [events=" + events + ", qualifiers=" + qualifiers + "]";
   }

}
