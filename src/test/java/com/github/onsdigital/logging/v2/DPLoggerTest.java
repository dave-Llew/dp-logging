package com.github.onsdigital.logging.v2;

import com.github.onsdigital.logging.v2.config.Config;
import com.github.onsdigital.logging.v2.config.ShutdownHook;
import com.github.onsdigital.logging.v2.event.Severity;
import com.github.onsdigital.logging.v2.event.SimpleEvent;
import com.github.onsdigital.logging.v2.serializer.LogSerialiser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import java.io.PrintStream;

import static com.github.onsdigital.logging.v2.DPLogger.MARSHAL_FAILURE;
import static java.text.MessageFormat.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DPLoggerTest {

    @Mock
    private LogSerialiser serialiser;

    @Mock
    private PrintStream printStream;

    @Mock
    private ShutdownHook shutdownHook;

    @Mock
    private Logger logger;

    @Mock
    private Config config;

    @Before
    public void setUp() {
        when(config.getNamespace()).thenReturn("com.test");
        when(config.getSerialiser()).thenReturn(serialiser);
        when(config.getLogger()).thenReturn(logger);
        when(config.getShutdownHook()).thenReturn(shutdownHook);

        System.setOut(printStream);

        DPLogger.reload(config);
    }

    @Test
    public void testLogSuccessful() throws Exception {
        SimpleEvent event = new SimpleEvent("com.test", Severity.INFO, "One o'clock and all's well"); //Disney referernce :p
        String json = "{\"json\": \"value\"}";

        when(serialiser.toJsonRetriable(event))
                .thenReturn(json);

        ArgumentCaptor<SimpleEvent> toJsonCaptor = ArgumentCaptor.forClass(SimpleEvent.class);
        ArgumentCaptor<String> logInfoCaptor = ArgumentCaptor.forClass(String.class);

        DPLogger.log(event);

        verify(config, times(1)).getLogger();
        verify(serialiser, times(1)).toJsonRetriable(toJsonCaptor.capture());
        verify(logger, times(1)).info(logInfoCaptor.capture());
        verify(config, never()).getShutdownHook();
        verifyZeroInteractions(printStream, shutdownHook);
    }

    /**
     * Test scenario where marshalling the event fails, but retrying mashalling the error event is successful.
     */
    @Test
    public void testLogMarshalToJsonError() throws Exception {
        SimpleEvent event = new SimpleEvent("com.test", Severity.INFO, "Get to da choppa!");
        LoggingException ex = new LoggingException("failed to marshal event to json");

        when(serialiser.toJsonRetriable(any(SimpleEvent.class))).thenThrow(ex);

        ArgumentCaptor<SimpleEvent> eventCaptor = ArgumentCaptor.forClass(SimpleEvent.class);
        ArgumentCaptor<String> printStreamCaptor = ArgumentCaptor.forClass(String.class);

        DPLogger.log(event);

        verify(config, times(1)).getLogger();
        verify(serialiser, times(1)).toJsonRetriable(eventCaptor.capture());
        verify(printStream, times(1)).println(printStreamCaptor.capture());

        assertThat(eventCaptor.getValue(), equalTo(event));
        assertThat(printStreamCaptor.getValue(), equalTo(format(MARSHAL_FAILURE, event, ex)));
        verifyZeroInteractions(logger, shutdownHook);
    }


    @Test
    public void testEpicFailure() throws Exception {
        SimpleEvent event = new SimpleEvent("com.test", Severity.INFO, "Get to da choppa!");
        LoggingException ex = new LoggingException("failed to marshal event to json");

        when(serialiser.toJsonRetriable(any(SimpleEvent.class))).thenThrow(ex);
        when(printStream.checkError()).thenReturn(true);

        ArgumentCaptor<SimpleEvent> eventCaptor = ArgumentCaptor.forClass(SimpleEvent.class);
        ArgumentCaptor<String> printStreamCaptor = ArgumentCaptor.forClass(String.class);

        DPLogger.log(event);

        verify(config, times(1)).getLogger();
        verify(serialiser, times(1)).toJsonRetriable(eventCaptor.capture());
        verify(printStream, times(1)).println(printStreamCaptor.capture());
        verify(shutdownHook, times(1)).shutdown();

        assertThat(eventCaptor.getValue(), equalTo(event));
        assertThat(printStreamCaptor.getValue(), equalTo(format(MARSHAL_FAILURE, event, ex)));
        verifyZeroInteractions(logger);
    }
}