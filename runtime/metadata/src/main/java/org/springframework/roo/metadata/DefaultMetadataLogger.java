package org.springframework.roo.metadata;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.metadata.internal.StandardMetadataTimingStatistic;

/**
 * Default implementation of {@link MetadataLogger}.
 * 
 * @author Ben Alex
 * @since 1.1.2
 */
@Service
@Component
public class DefaultMetadataLogger implements MetadataLogger {

    private static class TimerEntry {
        long clockStartedOrResumed; // nanos
        long duration; // nanos
        String responsibleClass;
    }

    private long eventNumber = 0;
    private final Stack<Long> eventStack = new Stack<Long>();
    private FileWriter fileLog;
    /**
     * key: responsible class, value: number of times a timing record was
     * created for the responsible class
     */
    private final Map<String, Long> invocations = new HashMap<String, Long>();
    private final Class<DefaultMetadataLogger> mutex = DefaultMetadataLogger.class;
    private final Stack<TimerEntry> timerStack = new Stack<TimerEntry>();
    /** key: responsible class, value: nanos occupied */
    private final Map<String, Long> timings = new HashMap<String, Long>();

    private int traceLevel = 0;

    public DefaultMetadataLogger() {
        if (System.getProperty("roo.metadata.trace") != null) {
            traceLevel = 2;
        }
    }

    public SortedSet<MetadataTimingStatistic> getTimings() {
        final SortedSet<MetadataTimingStatistic> result = new TreeSet<MetadataTimingStatistic>();
        synchronized (mutex) {
            for (final String key : timings.keySet()) {
                result.add(new StandardMetadataTimingStatistic(key, timings
                        .get(key), invocations.get(key)));
            }
        }
        return result;
    }

    public int getTraceLevel() {
        return traceLevel;
    }

    public void log(final String message) {
        Validate.notBlank(message, "Message to log required");
        Validate.isTrue(eventStack.size() > 0,
                "Event stack is empty, so no logging should have been requested at this time");
        final StringBuilder sb = new StringBuilder("00000000");
        // Get the current event ID off the stack
        final Long eventIdentifier = eventStack.get(eventStack.size() - 1);
        // Figure out the indentation level
        final int indentationLevel = eventStack.size();
        final String hex = Long.toHexString(eventIdentifier);
        sb.replace(8 - hex.length(), 8, hex);
        for (int i = 0; i < indentationLevel; i++) {
            sb.append(" ");
        }
        sb.append(message);
        logToFile(sb.toString());
    }

    private void logToFile(final String line) {
        if (fileLog == null) {
            try {
                // Overwrite existing (don't append)
                fileLog = new FileWriter("metadata.log", false);
            }
            catch (final IOException ignore) {
            }
            if (fileLog == null) {
                // Still failing, so give up
                return;
            }
        }
        try {
            fileLog.write(line + "\n"); // Unix line endings only from Roo
            fileLog.flush(); // So tail -f will show it's working
        }
        catch (final IOException ignoreIt) {
        }
    }

    public void setTraceLevel(final int trace) {
        traceLevel = trace;
    }

    public void startEvent() {
        eventNumber++;
        eventStack.push(eventNumber);
    }

    public void startTimer(final String responsibleClass) {
        Validate.notBlank(responsibleClass, "Responsible class required");
        final long now = System.nanoTime();
        if (timerStack.size() > 0) {
            // There is an existing timer on the stack, so we need to stop the
            // clock for it
            final TimerEntry timerEntry = timerStack.get(timerStack.size() - 1);
            // Add the duration it ran to any existing duration
            timerEntry.duration = timerEntry.duration + now
                    - timerEntry.clockStartedOrResumed;
            timerEntry.clockStartedOrResumed = now;
        }
        // Start a new timer
        final TimerEntry timerEntry = new TimerEntry();
        timerEntry.responsibleClass = responsibleClass;
        timerEntry.clockStartedOrResumed = now;
        timerStack.push(timerEntry);
    }

    public void stopEvent() {
        Validate.isTrue(
                eventStack.size() > 0,
                "Event stack is empty, indicating a mismatched number of timer start/stop calls");
        eventStack.pop();
    }

    public void stopTimer() {
        Validate.isTrue(
                timerStack.size() > 0,
                "Timer stack is empty, indicating a mismatched number of timer start/stop calls");
        final long now = System.nanoTime();
        final TimerEntry timerEntry = timerStack.pop();
        timerEntry.duration = timerEntry.duration + now
                - timerEntry.clockStartedOrResumed;
        final String responsibleClass = timerEntry.responsibleClass;

        // Update the timings summary
        synchronized (mutex) {
            Long existingSummary = timings.get(responsibleClass);
            if (existingSummary == null) {
                existingSummary = timerEntry.duration;
            }
            else {
                existingSummary = existingSummary + timerEntry.duration;
            }
            timings.put(responsibleClass, existingSummary);

            // Update the invocation count
            Long existingInvocations = invocations.get(responsibleClass);
            if (existingInvocations == null) {
                existingInvocations = 0L;
            }
            existingInvocations++;
            invocations.put(responsibleClass, existingInvocations);
        }
    }
}
