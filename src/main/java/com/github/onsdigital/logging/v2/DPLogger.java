package com.github.onsdigital.logging.v2;

import com.github.onsdigital.logging.v2.config.DefaultConfig;
import com.github.onsdigital.logging.v2.config.ErrorWriter;
import com.github.onsdigital.logging.v2.config.LogConfig;
import com.github.onsdigital.logging.v2.config.ShutdownHook;
import com.github.onsdigital.logging.v2.event.BaseEvent;
import com.github.onsdigital.logging.v2.event.Severity;

import java.time.format.DateTimeFormatter;

import static com.github.onsdigital.logging.v2.event.Severity.ERROR;
import static com.github.onsdigital.logging.v2.event.Severity.FATAL;
import static com.github.onsdigital.logging.v2.event.Severity.WARN;
import static com.github.onsdigital.logging.v2.event.Severity.getSeverity;
import static java.text.MessageFormat.format;

public class DPLogger {

    private static LogConfig CONFIG = DefaultConfig.get();

    private static final String ISO8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    static final String MARSHAL_FAILURE = "failed to marshal log event to json: event {0}, exception: {1}";
    static final String MARSHALL_EVENT_ERR = "error marshalling dp log event to json";

    private static DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(ISO8601_FORMAT);

    private DPLogger() {
        // contains only static only methods - hide constructor.
    }

    public static void init(LogConfig config) {
        synchronized (DPLogger.class) {
            CONFIG = config;
        }
    }

    static void reload(LogConfig config) {
        synchronized (DPLogger.class) {
            CONFIG = config;
        }
    }

    public static LogConfig logConfig() {
        if (CONFIG == null) {
            throw new UncheckedLoggingException("DPLogger is not initalised");
        }
        return CONFIG;
    }

    public static <T extends BaseEvent> void log(T event) {
        try {
            String eventJson = marshalEvent(event);
            Severity severity = getSeverity(event.getSeverity());

            logEventJson(eventJson, severity, logConfig().getLogger());
        } catch (LoggingException ex) {
            ErrorWriter errorWriter = logConfig().getErrorWriter();
            ShutdownHook shutdownHook = logConfig().getShutdownHook();

            handleMarshalEventFailure(event, ex, errorWriter, shutdownHook);
        }
    }

    private static <T extends BaseEvent> String marshalEvent(T event) throws LoggingException {
        return logConfig().getSerialiser().marshallEvent(event);
    }

    static <T extends BaseEvent> void handleMarshalEventFailure(T event, LoggingException ex,
                                                                ErrorWriter errorWriter,
                                                                ShutdownHook shutdownHook) {
        boolean isError = errorWriter.write(getMarshalFailureMessage(event, ex));
        if (isError) {
            logConfig().getShutdownHook().shutdown();
        }
    }

    static void logEventJson(String eventJson, Severity severity, Logger logger) {
        if (FATAL == severity)
            logger.error(eventJson);
        else if (ERROR == severity)
            logger.error(eventJson);
        else if (WARN == severity)
            logger.warn(eventJson);
        else
            logger.info(eventJson);
    }

    static <T extends BaseEvent> String getMarshalFailureMessage(T event, LoggingException ex) {
        return format(MARSHAL_FAILURE, event, ex);
    }

    public static DateTimeFormatter formatter() {
        return FORMATTER;
    }
}