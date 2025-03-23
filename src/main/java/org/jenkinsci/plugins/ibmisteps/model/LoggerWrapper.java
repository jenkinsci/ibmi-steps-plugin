package org.jenkinsci.plugins.ibmisteps.model;

import java.io.PrintStream;
import java.util.Objects;

public class LoggerWrapper {
    private final PrintStream logger;
    private final boolean doTrace;

    public LoggerWrapper(final PrintStream logger, final boolean doTrace) {
        this.logger = Objects.requireNonNull(logger, "Logger cannot be null");
        this.doTrace = doTrace;
    }

    public synchronized void log(final String format, final Object... args) {
        logger.println("[INFO] " + String.format(format, args));
    }

    public synchronized void log(final String message) {
        logger.println("[INFO] " + message);
    }

    public synchronized void error(final String format, final Object... args) {
        logger.println("[ERROR] " + String.format(format, args));
    }

    public synchronized void error(final String message) {
        logger.println("[ERROR] " + message);
    }

    public synchronized void trace(final String format, final Object... args) {
        if (doTrace) {
            logger.println("[TRACE] " + String.format(format, args));
        }
    }

    public synchronized void trace(final String message) {
        if (doTrace) {
            logger.println("[TRACE] " + message);
        }
    }
}
