/**
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package test.org.jboss.forge.furnace.api;

import static org.hamcrest.CoreMatchers.notNullValue;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.services.LocalServices;
import org.jboss.forge.furnace.Furnace;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
@RunWith(Arquillian.class)
public class AddonArchiveDefaultSmokeTest
{
   @Test
   public void test()
   {
      Furnace furnace = LocalServices.getFurnace(getClass().getClassLoader());
      Assert.assertThat(furnace, notNullValue());
   }

}
