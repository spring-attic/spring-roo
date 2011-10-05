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

	private final String name;
	private final long time;
	private final long invocations;

	public StandardMetadataTimingStatistic(final String name, final long time, final long invocations) {
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

	public int compareTo(final MetadataTimingStatistic o) {
		int result = Long.valueOf(time).compareTo(o.getTime());
		if (result == 0) {
			result = Long.valueOf(invocations).compareTo(o.getInvocations());
		}
		if (result == 0) {
			result = name.compareTo(o.getName());
		}
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		return obj instanceof MetadataTimingStatistic && this.compareTo((MetadataTimingStatistic) obj) == 0;
	}

	@Override
	public int hashCode() {
		return Long.valueOf(time).hashCode() * Long.valueOf(invocations).hashCode() * name.hashCode();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (time < 1000000) {
			// Nanosecond precision
			sb.append(String.format("%06d", time)).append(" ns; ");
		} else {
			// Millisecond precision
			sb.append(String.format("%06d", time/1000000)).append(" ms; ");
		}
		sb.append(String.format("%06d", invocations)).append(" call(s): ");
		sb.append(name);
		return sb.toString();
	}
}
