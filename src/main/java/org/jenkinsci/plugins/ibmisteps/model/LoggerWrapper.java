package org.jenkinsci.plugins.ibmisteps.model;

import java.io.PrintStream;

public class LoggerWrapper {
    private final PrintStream logger;
    private final boolean doTrace;

    public LoggerWrapper(final PrintStream logger, final boolean doTrace) {
        this.logger = logger;
        this.doTrace = doTrace;
    }

    public void log(final String format, final Object... args) {
        logger.format(format, args);
        logger.println();
    }

    public void log(final String message) {
        logger.println(message);
    }

    public void trace(final String format, final Object... args) {
        if (doTrace) {
            logger.print("[TRACE] ");
            logger.format(format, args);
            logger.println();
        }
    }

    public void trace(final String message) {
        if (doTrace) {
            logger.print("[TRACE] ");
            logger.println(message);
        }
    }
}
