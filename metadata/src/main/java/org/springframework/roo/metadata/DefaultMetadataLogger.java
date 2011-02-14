package org.springframework.roo.metadata;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.metadata.internal.StandardMetadataTimingStatistic;
import org.springframework.roo.support.util.Assert;

/**
 * Default implementation of {@link MetadataLogger}.
 * 
 * @author Ben Alex
 * @since 1.1.2
 */
@Service
@Component
public class DefaultMetadataLogger implements MetadataLogger {
	private int traceLevel = 0;
	private long eventNumber = 0;
	private FileWriter fileLog;
	private Stack<Long> eventStack = new Stack<Long>();
	private Stack<TimerEntry> timerStack = new Stack<TimerEntry>();
	/** key: responsible class, value: nanos occupied */
	private Map<String, Long> timings = new HashMap<String, Long>();
	/** key: responsible class, value: number of times a timing record was created for the responsible class */
	private Map<String, Long> invocations = new HashMap<String, Long>();
	private final Class<DefaultMetadataLogger> mutex = DefaultMetadataLogger.class;
	
	public DefaultMetadataLogger() {
		if (System.getProperty("roo.metadata.trace") != null) {
			traceLevel = 2;
		}
	}
	
	public int getTraceLevel() {
		return traceLevel;
	}

	public void setTraceLevel(int trace) {
		this.traceLevel = trace;
	}

	public SortedSet<MetadataTimingStatistic> getTimings() {
		SortedSet<MetadataTimingStatistic> result = new TreeSet<MetadataTimingStatistic>();
		synchronized (mutex) {
			for (String key : timings.keySet()) {
				result.add(new StandardMetadataTimingStatistic(key, timings.get(key), invocations.get(key)));
			}
		}
		return result;
	}

	public void startEvent() {
		eventNumber++;
		eventStack.push(eventNumber);
	}
	
	public void stopEvent() {
		Assert.isTrue(eventStack.size() > 0, "Event stack is empty, indicating a mismatched number of timer start/stop calls");
		eventStack.pop();
	}
	
	public void log(String message) {
		Assert.hasText(message, "Message to log required");
		Assert.isTrue(eventStack.size() > 0, "Event stack is empty, so no logging should have been requested at this time");
		StringBuilder sb = new StringBuilder("00000000");
		// Get the current event ID off the stack
		Long eventIdentifier = eventStack.get(eventStack.size() - 1);
		// Figure out the indentation level
		int indentationLevel = eventStack.size();
		String hex = Long.toHexString(eventIdentifier);
		sb.replace(8 - hex.length(), 8, hex);
		for (int i = 0; i < indentationLevel; i++) {
			sb.append(" ");
		}
		sb.append(message);
		logToFile(sb.toString());
	}
	
	public void startTimer(String responsibleClass) {
		Assert.hasText(responsibleClass, "Responsible class required");
		long now = System.nanoTime();
		if (timerStack.size() > 0) {
			// There is an existing timer on the stack, so we need to stop the clock for it
			TimerEntry timerEntry = timerStack.get(timerStack.size()-1);
			// Add the duration it ran to any existing duration
			timerEntry.duration = timerEntry.duration + (now - timerEntry.clockStartedOrResumed);
			timerEntry.clockStartedOrResumed = now;
		}
		// Start a new timer
		TimerEntry timerEntry = new TimerEntry();
		timerEntry.responsibleClass = responsibleClass;
		timerEntry.clockStartedOrResumed = now;
		timerStack.push(timerEntry);
	}
	
	public void stopTimer() {
		Assert.isTrue(timerStack.size() > 0, "Timer stack is empty, indicating a mismatched number of timer start/stop calls");
		long now = System.nanoTime();
		TimerEntry timerEntry = timerStack.pop();
		timerEntry.duration = timerEntry.duration + (now - timerEntry.clockStartedOrResumed);
		String responsibleClass = timerEntry.responsibleClass;
		
		// Update the timings summary
		synchronized (mutex) {
			Long existingSummary = timings.get(responsibleClass);
			if (existingSummary == null) {
				existingSummary = timerEntry.duration;
			} else {
				existingSummary = existingSummary + timerEntry.duration;
			}
			timings.put(responsibleClass, existingSummary);
			
			// Update the invocation count
			Long existingInvocations = invocations.get(responsibleClass);
			if (existingInvocations == null) {
				existingInvocations = new Long(0);
			}
			existingInvocations++;
			invocations.put(responsibleClass, existingInvocations);
		}
	}
	
	private void logToFile(String line) {
		if (fileLog == null) {
			try {
				fileLog = new FileWriter("metadata.log", false); // Overwrite existing (don't append)
			} catch (IOException ignore) {}
			if (fileLog == null) {
				// Still failing, so give up
				return;
			}
		}
		try {
			fileLog.write(line + "\n"); // Unix line endings only from Roo
			fileLog.flush(); // So tail -f will show it's working
		} catch (IOException ignoreIt) {}
	}
	
	private class TimerEntry {
		String responsibleClass;
		long clockStartedOrResumed = 0; // nanos
		long duration = 0; // nanos
	}
}
