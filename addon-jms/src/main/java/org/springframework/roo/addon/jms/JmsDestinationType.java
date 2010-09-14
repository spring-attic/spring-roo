package org.springframework.roo.addon.jms;

import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * JMS destination types known to the JMS add-on.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
public class JmsDestinationType implements Comparable<JmsDestinationType> {
	public static final JmsDestinationType TOPIC = new JmsDestinationType("TOPIC");
	public static final JmsDestinationType DURABLE_TOPIC = new JmsDestinationType("DURABLE_TOPIC");
	public static final JmsDestinationType QUEUE = new JmsDestinationType("QUEUE");
	private String type;

	public JmsDestinationType(String type) {
		Assert.notNull(type, "Type required");
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public final boolean equals(Object obj) {
		return obj != null && obj instanceof JmsDestinationType && this.compareTo((JmsDestinationType) obj) == 0;
	}

	public final int compareTo(JmsDestinationType o) {
		if (o == null) return -1;
		int result = this.type.compareTo(o.type);

		return result;
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("type", type);
		return tsc.toString();
	}

	public String getKey() {
		return this.type;
	}
}