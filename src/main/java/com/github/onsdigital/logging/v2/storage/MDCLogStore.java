package com.github.onsdigital.logging.v2.storage;

import com.github.onsdigital.logging.v2.LoggingException;
import com.github.onsdigital.logging.v2.event.HTTP;
import com.github.onsdigital.logging.v2.serializer.LogSerialiser;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

import static java.text.MessageFormat.format;

public class MDCLogStore implements LogStore {

    static final String HTTP_KEY = "http";
    static final String TRACE_ID_KEY = "trace_id";
    static final String MARSHALL_ERR_FMT = "failed to marshall HTTP, trace_id: {0}";
    static final String UNMARSHALL_ERR_FMT = "failed to unmarshall HTTP, trace_id: {0}";

    private LogSerialiser serialiser;

    public MDCLogStore(LogSerialiser serialiser) {
        this.serialiser = serialiser;
    }

    @Override
    public void saveHTTP(HTTP http) {
        try {
            MDC.put(HTTP_KEY, serialiser.marshallHTTP(http));
        } catch (LoggingException ex) {
            LoggingException wrapped = new LoggingException(format(MARSHALL_ERR_FMT, getTraceID()), ex);
            System.err.println(wrapped);
        }
    }

    @Override
    public void saveTraceID(HttpServletRequest req) {
        String traceID = req.getHeader(TRACE_ID_KEY);
        if (StringUtils.isEmpty(traceID)) {
            traceID = UUID.randomUUID().toString();
        }
        MDC.put(TRACE_ID_KEY, traceID);
    }

    @Override
    public HTTP getHTTP() {
        String httpJson = MDC.get(HTTP_KEY);
        if (StringUtils.isEmpty(httpJson)) {
            return null;
        }
        try {
            return serialiser.unmarshallHTTP(httpJson);
        } catch (LoggingException ex) {
            LoggingException wrapped = new LoggingException(format(UNMARSHALL_ERR_FMT, getTraceID()), ex);
            System.err.println(wrapped);
        }
        return null;
    }

    @Override
    public String getTraceID() {
        return MDC.get(TRACE_ID_KEY);
    }
}