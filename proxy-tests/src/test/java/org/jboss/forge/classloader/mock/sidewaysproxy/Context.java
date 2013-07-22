package org.jboss.forge.classloader.mock.sidewaysproxy;

public interface Context
{
   ContextValue<Payload> get();
   void set(ContextValue<Payload> payload);
}
