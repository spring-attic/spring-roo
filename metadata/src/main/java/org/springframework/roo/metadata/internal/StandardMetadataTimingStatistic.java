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
	
	public StandardMetadataTimingStatistic(String name, long time) {
		Assert.hasText(name, "Name required");
		this.name = name;
		this.time = time;
	}

	public String getName() {
		return name;
	}

	public long getTime() {
		return time;
	}

	public int compareTo(MetadataTimingStatistic o) {
		int result = new Long(time).compareTo(o.getTime());
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
		return new Long(time).hashCode() * name.hashCode();
	}

	@Override
	public String toString() {
		return time + ": " + name;
	}

}
