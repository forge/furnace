package org.jboss.forge.classloader.mock;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * @author <a href="mailto:jesse.sightler@gmail.com">Jesse Sightler</a>
 */
public class JavaUtilLoggingFactory {
    public LogRecord getLogRecord() {
        LogRecord record = new LogRecord(Level.INFO, "Test Message");
        record.setThrown(new IOException());
        return record;
    }
}
