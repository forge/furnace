package org.jboss.arquillian.test.impl;

import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.EventContext;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.arquillian.test.spi.annotation.ClassScoped;
import org.jboss.arquillian.test.spi.context.ClassContext;
import org.jboss.arquillian.test.spi.context.SuiteContext;
import org.jboss.arquillian.test.spi.context.TestContext;
import org.jboss.arquillian.test.spi.event.suite.After;
import org.jboss.arquillian.test.spi.event.suite.AfterClass;
import org.jboss.arquillian.test.spi.event.suite.ClassEvent;
import org.jboss.arquillian.test.spi.event.suite.SuiteEvent;
import org.jboss.arquillian.test.spi.event.suite.TestEvent;

/**
 * TestContextHandler
 * 
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
public class TestContextHandler
{
   @Inject
   private Instance<SuiteContext> suiteContextInstance;

   @Inject
   private Instance<ClassContext> classContextInstance;

   @Inject
   private Instance<TestContext> testContextInstance;

   @Inject
   @ClassScoped
   private InstanceProducer<TestClass> testClassProducer;

   public void createSuiteContext(@Observes(precedence = 100) EventContext<SuiteEvent> context)
   {
      SuiteContext suiteContext = this.suiteContextInstance.get();
      try
      {
         suiteContext.activate();
         context.proceed();
      }
      finally
      {
         suiteContext.deactivate();
      }
   }

   public void createClassContext(@Observes(precedence = 100) EventContext<ClassEvent> context)
   {
      ClassContext classContext = this.classContextInstance.get();
      try
      {
         classContext.activate(context.getEvent().getTestClass().getJavaClass());
         testClassProducer.set(context.getEvent().getTestClass());
         context.proceed();
      }
      finally
      {
         classContext.deactivate();
         if (AfterClass.class.isAssignableFrom(context.getEvent().getClass()))
            classContext.destroy(context.getEvent().getTestClass().getJavaClass());
      }
   }

   public void createTestContext(@Observes(precedence = 100) EventContext<TestEvent> context)
   {
      TestContext testContext = this.testContextInstance.get();
      try
      {
         testContext.activate(context.getEvent().getTestInstance());
         context.proceed();
      }
      finally
      {
         testContext.deactivate();
         if (After.class.isAssignableFrom(context.getEvent().getClass()))
            testContext.destroy(context.getEvent().getTestInstance());
      }
   }
}
