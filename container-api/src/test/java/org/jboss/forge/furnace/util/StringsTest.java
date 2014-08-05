/**
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.util;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author <a href="ggastald@redhat.com">George Gastaldi</a>
 */
public class StringsTest
{

   /**
    * Test method for {@link org.jboss.forge.furnace.util.Strings#isURL(java.lang.String)}.
    */
   @Test
   public void testValidURLs()
   {
      assertValidURL("http://foo.com/blah_blah");
      assertValidURL("http://foo.com/blah_blah/");
      assertValidURL("http://foo.com/blah_blah_(wikipedia)");
      assertValidURL("http://foo.com/blah_blah_(wikipedia)_(again)");
      assertValidURL("http://www.example.com/wpstyle/?p=364");
      assertValidURL("https://www.example.com/foo/?bar=baz&inga=42&quux");
      assertValidURL("http://\u272adf.ws/123");
      assertValidURL("http://userid:password@example.com:8080");
      assertValidURL("http://userid:password@example.com:8080/");
      assertValidURL("http://userid@example.com");
      assertValidURL("http://userid@example.com/");
      assertValidURL("http://userid@example.com:8080");
      assertValidURL("http://userid@example.com:8080/");
      assertValidURL("http://userid:password@example.com");
      assertValidURL("http://userid:password@example.com/");
      assertValidURL("http://142.42.1.1/");
      assertValidURL("http://142.42.1.1:8080/");
      assertValidURL("http://\u27a1.ws/\u4a39");
      assertValidURL("http://\u2318.ws");
      assertValidURL("http://\u2318.ws/");
      assertValidURL("http://foo.com/blah_(wikipedia)#cite-1");
      assertValidURL("http://foo.com/blah_(wikipedia)_blah#cite-1");
      assertValidURL("http://foo.com/unicode_(\u272a)_in_parens");
      assertValidURL("http://foo.com/(something)?after=parens");
      assertValidURL("http://\u263a.damowmow.com/");
      assertValidURL("http://code.google.com/events/#&product=browser");
      assertValidURL("http://j.mp");
      assertValidURL("ftp://foo.bar/baz");
      assertValidURL("http://foo.bar/?q=Test%20URL-encoded%20stuff");
      assertValidURL("http://\u0645\u062b\u0627\u0644.\u0625\u062e\u062a\u0628\u0627\u0631");
      assertValidURL("http://\u4f8b\u5b50.\u6d4b\u8bd5");
      assertValidURL("http://\u0909\u0926\u093e\u0939\u0930\u0923.\u092a\u0930\u0940\u0915\u094d\u0937\u093e");
      assertValidURL("http://-.~_!$&'()*+,;=:%40:80%2f::::::@example.com");
      assertValidURL("http://1337.net");
      assertValidURL("http://a.b-c.de");
      assertValidURL("http://223.255.255.254");
   }

   @Test
   @Ignore("Check URL regex")
   public void testInvalidURLs()
   {
      assertInvalidURL("http://");
      assertInvalidURL("http://.");
      assertInvalidURL("http://..");
      assertInvalidURL("http://../");
      assertInvalidURL("http://?");
      assertInvalidURL("http://??");
      assertInvalidURL("http://??/");
      assertInvalidURL("http://#");
      assertInvalidURL("http://##");
      assertInvalidURL("http://##/");
      assertInvalidURL("http://foo.bar?q=Spaces should be encoded");
      assertInvalidURL("//");
      assertInvalidURL("//a");
      assertInvalidURL("///a");
      assertInvalidURL("///");
      assertInvalidURL("http:///a");
      assertInvalidURL("foo.com");
      assertInvalidURL("rdar://1234");
      assertInvalidURL("h://test");
      assertInvalidURL("http:// shouldfail.com");
      assertInvalidURL(":// should fail");
      assertInvalidURL("http://foo.bar/foo(bar)baz quux");
      assertInvalidURL("ftps://foo.bar/");
      assertInvalidURL("http://-error-.invalid/");
      assertInvalidURL("http://a.b--c.de/");
      assertInvalidURL("http://-a.b.co");
      assertInvalidURL("http://a.b-.co");
      assertInvalidURL("http://0.0.0.0");
      assertInvalidURL("http://10.1.1.0");
      assertInvalidURL("http://10.1.1.255");
      assertInvalidURL("http://224.1.1.1");
      assertInvalidURL("http://1.1.1.1.1");
      assertInvalidURL("http://123.123.123");
      assertInvalidURL("http://3628126748");
      assertInvalidURL("http://.www.foo.bar/");
      assertInvalidURL("http://www.foo.bar./");
      assertInvalidURL("http://.www.foo.bar./");
      assertInvalidURL("http://10.1.1.1");
      assertInvalidURL("http://10.1.1.254");
   }

   private void assertValidURL(String url)
   {
      Assert.assertTrue(Strings.isURL(url));
   }

   private void assertInvalidURL(String url)
   {
      Assert.assertFalse(Strings.isURL(url));
   }
}
