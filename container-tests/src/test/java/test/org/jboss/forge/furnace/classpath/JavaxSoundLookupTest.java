/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package test.org.jboss.forge.furnace.classpath;

import java.util.ServiceLoader;

import javax.sound.midi.spi.MidiDeviceProvider;
import javax.sound.sampled.spi.MixerProvider;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class JavaxSoundLookupTest
{
   @Deployment
   public static ForgeArchive getDeployment()
   {
      ForgeArchive archive = ShrinkWrap.create(ForgeArchive.class)
               .addAsLocalServices(JavaxSoundLookupTest.class);

      return archive;
   }

   @Test
   public void testGetJDKProvidedImpl() throws Exception
   {
      try
      {
         getClass().getClassLoader().loadClass("com.sun.media.sound.RealTimeSequencerProvider");
         getClass().getClassLoader().loadClass("com.sun.media.sound.MidiOutDeviceProvider");
         getClass().getClassLoader().loadClass("com.sun.media.sound.MidiInDeviceProvider");
         getClass().getClassLoader().loadClass("com.sun.media.sound.SoftProvider");
      }
      catch (Exception e)
      {
         Assert.fail("Could not load required Factory class." + e.getMessage());
      }
   }

   @Test
   public void testMidiDeviceProviderLookup()
   {
      Assert.assertTrue(ServiceLoader.load(MidiDeviceProvider.class).iterator().hasNext());
   }

   @Test
   public void testMixerProviderLookup()
   {
      Assert.assertTrue(ServiceLoader.load(MixerProvider.class).iterator().hasNext());
   }

}
