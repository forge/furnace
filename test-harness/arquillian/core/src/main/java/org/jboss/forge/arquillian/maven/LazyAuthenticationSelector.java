/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.arquillian.maven;

import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.AuthenticationSelector;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.repository.DefaultAuthenticationSelector;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;

/**
 * An {@link AuthenticationSelector} that resolves the Authentication info lazily at runtime. This selector determines
 * whether a remote repository is mirrored and then returns the authentication info for the mirror. If no mirror exists,
 * the authentication info for the remote repository is returned.
 */
final class LazyAuthenticationSelector implements AuthenticationSelector
{
   private final DefaultMirrorSelector mirrorSelector;
   private DefaultAuthenticationSelector defaultAuthSelector;

   LazyAuthenticationSelector(DefaultMirrorSelector mirrorSelector)
   {
      this.mirrorSelector = mirrorSelector;
      this.defaultAuthSelector = new DefaultAuthenticationSelector();
   }

   @Override
   public Authentication getAuthentication(RemoteRepository repository)
   {
      RemoteRepository mirror = mirrorSelector.getMirror(repository);
      if (mirror != null)
      {
         return defaultAuthSelector.getAuthentication(mirror);
      }
      return defaultAuthSelector.getAuthentication(repository);
   }

   public void add(String id, Authentication authentication)
   {
      defaultAuthSelector.add(id, authentication);
   }
}