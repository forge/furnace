/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.classloader.mock;

import java.util.Optional;

/**
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class MockOptionalService
{
   public Optional<MockParentInterface2> getOptional()
   {
      return Optional.<MockParentInterface2> of(new MockService2(123));
   }
}
