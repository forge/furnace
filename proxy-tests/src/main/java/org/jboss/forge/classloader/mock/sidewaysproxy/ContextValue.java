package org.jboss.forge.classloader.mock.sidewaysproxy;


public interface ContextValue<PAYLOADTYPE> extends Iterable<PAYLOADTYPE>
{
   public void set(PAYLOADTYPE payload);
   public PAYLOADTYPE get();
}
