package org.springframework.roo.metadata;

import java.util.SortedSet;

/**
 * Simplifies the creation of nested event logs and metadata timing statistics.
 * <p>
 * This interface is intended for concurrent use between
 * {@link MetadataDependencyRegistry} and {@link MetadataService}.
 * <p>
 * Metadata timing is the simplest operation. Before a metadata provider is
 * invoked, the {@link MetadataLogger} identifies the metadata provider as the
 * "responsible class" and commences timing via {@link #startTimer(String)}. The
 * metadata provider is free to then perform its work, which may include calling
 * for more metadata. If more metadata is requested, new
 * {@link #startTimer(String)} invocations will take place. As each metadata
 * provider completes its work, the {@link #stopTimer()} method is called. This
 * aggregates timing information and makes it available via
 * {@link #getTimings()}.
 * <p>
 * Metadata logging is similar. Before logging can take place,
 * {@link #startEvent()} should be invoked. The definition of an "event" varies,
 * but generally a series of related logging calls should be the same "event".
 * Once an event has been started, calls to {@link #log(String)} will log
 * messages and associate them with the active event. After an event concludes,
 * the {@link #stopEvent()} method must be called. Nesting is automatically
 * handled by an implementation, with successive start event calls being
 * indented in the logging output, and the logging output correctly reporting
 * the correlating event ID. Callers should not invoke {@link #log(String)}
 * unless a suitable {@link #getTraceLevel()} is desired by the user.
 * <p>
 * It is important to always call the "stop" method once for every "start"
 * method that was called. Failure to observe this requirement will lead to
 * exceptions as the stack is managed and an invalid state detected.
 * <p>
 * Implementations are free to store metadata logging output in any file they
 * wish. This file should be created on the first call to {@link #log(String)}.
 * 
 * @author Ben Alex
 * @since 1.1.2
 */
public interface MetadataLogger {

    /**
     * @return a snapshot of timing statistics that have been collated so far
     *         (never null, but may be empty)
     */
    SortedSet<MetadataTimingStatistic> getTimings();

    /**
     * @return the currently active trace level (0 = none, 1 = major events, 2 =
     *         all events)
     */
    int getTraceLevel();

    /**
     * Logs a message against the given event identifier.
     * 
     * @param message to log (required)
     */
    void log(String message);

    /**
     * Enable low-level tracing of event delivery information. Defaults to level
     * 0 (none).
     * 
     * @param trace the level (0 = none, 1 = major events, 2 = all events)
     */
    void setTraceLevel(int trace);

    /**
     * Increments the current stack level. The current stack level determines
     * the indentation of logged messages. It is required that for every
     * increment, a corresponding {@link #decrementLevel()} is invoked.
     */
    void startEvent();

    /**
     * Starts the timer counting against the responsible class. The timer must
     * eventually be {@link #stopTimer()}, but timings will cease being counted
     * against the responsible class when a new {@link #startTimer(String)} is
     * invoked.
     * 
     * @param responsibleClass the class responsible for this timing (required)
     */
    void startTimer(String responsibleClass);

    /**
     * Decrements the current stack level.
     */
    void stopEvent();

    /**
     * Stops the most recently started timer. This is mandatory and must be in
     * the reverse order timers were started. When a timer stops is also when we
     * update its timings.
     */
    void stopTimer();
}