package org.springframework.roo.addon.jms;

import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * JMS providers known to the JMS add-on.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
public class JmsProvider implements Comparable<JmsProvider> {
	public static final JmsProvider ACTIVEMQ_IN_MEMORY = new JmsProvider("ACTIVEMQ_IN_MEMORY", "org.apache.activemq.store.memory.MemoryPersistenceAdapter");
	private String provider;
	private String adapter;

	public JmsProvider(String provider, String adapter) {
		Assert.notNull(provider, "Provider required");
		Assert.notNull(adapter, "Adapter required");
		this.provider = provider;
		this.adapter = adapter;
	}

	public String getAdapter() {
		return adapter;
	}

	public final boolean equals(Object obj) {
		return obj != null && obj instanceof JmsProvider && this.compareTo((JmsProvider) obj) == 0;
	}

	public final int compareTo(JmsProvider o) {
		if (o == null) return -1;
		int result = this.provider.compareTo(o.provider);

		return result;
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("provider", provider);
		return tsc.toString();
	}

	public String getKey() {
		return this.provider;
	}
}