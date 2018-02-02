package com.github.onsdigital.logging.layouts;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.LayoutBase;

import static java.lang.System.getenv;


/**
 * Created by dave on 01/02/2018.
 */
public class ConfigurableLayout extends LayoutBase<ILoggingEvent> {

    private static final String LOGGING_FORMAT_ENV_KEY = "logging_format";
    private static final String TEXT_FORMAT = "text";
    private static final String JSON_FORMAT = "json";
    private static final String PRETTY_JSON_FORMAT = "pretty_json";

    private Layout layout;

    public ConfigurableLayout() {
        String format = getenv(LOGGING_FORMAT_ENV_KEY);
        if (JSON_FORMAT.equals(format)) {
            this.layout = new JsonLayout();
        } else if (PRETTY_JSON_FORMAT.equals(format)) {
            this.layout = new PrettyJsonLayout();
        } else {
            this.layout = new TextLayout();
        }
    }

    @Override
    public String doLayout(ILoggingEvent iLoggingEvent) {
        return layout.doLayout(iLoggingEvent);
    }
}
