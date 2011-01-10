package org.springframework.roo.metadata.internal;

import org.springframework.roo.metadata.MetadataTimingStatistic;
import org.springframework.roo.support.util.Assert;

/**
 * Standard implementation of {@link MetadataTimingStatistic}.
 * 
 * @author Ben Alex
 *
 */
public class StandardMetadataTimingStatistic implements MetadataTimingStatistic {

	private String name;
	private long time;
	private long invocations;
	
	public StandardMetadataTimingStatistic(String name, long time, long invocations) {
		Assert.hasText(name, "Name required");
		this.name = name;
		this.time = time;
		this.invocations = invocations;
	}

	public String getName() {
		return name;
	}

	public long getTime() {
		return time;
	}

	public long getInvocations() {
		return invocations;
	}

	public int compareTo(MetadataTimingStatistic o) {
		int result = new Long(time).compareTo(o.getTime());
		if (result == 0) {
			result = new Long(invocations).compareTo(o.getInvocations());
		}
		if (result == 0) {
			result = name.compareTo(o.getName());
		}
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		return obj != null && obj instanceof MetadataTimingStatistic && this.compareTo((MetadataTimingStatistic)obj) == 0;
	}

	@Override
	public int hashCode() {
		return new Long(time).hashCode() * new Long(invocations).hashCode() * name.hashCode();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (time < 1000000) {
			// nanosecond precision
			sb.append(String.format("%06d", time)).append(" ns; ");
		} else {
			// millisecond precision
			sb.append(String.format("%06d", time/1000000)).append(" ms; ");
		}
		sb.append(String.format("%06d", invocations)).append(" call(s): ");
		sb.append(name);
		return sb.toString();
	}

}
