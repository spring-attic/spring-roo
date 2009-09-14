package org.springframework.roo.metadata;

/**
 * Represents an immutable representation of a single timing statistic from {@link MetadataDependencyRegistry}.
 * 
 * @author Ben Alex
 *
 */
public interface MetadataTimingStatistic extends Comparable<MetadataTimingStatistic> {
	/**
	 * @return the number of milliseconds associated with this {@link #getName()}.
	 */
	long getTime();
	
	/**
	 * @return an identifier to differentiate this timing statistic from another (never null or empty)
	 */
	String getName();
}
