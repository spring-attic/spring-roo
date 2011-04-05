package org.springframework.roo.addon.jms;

/**
 * JMS providers known to the JMS add-on.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
public enum JmsProvider {
	ACTIVEMQ_IN_MEMORY("org.apache.activemq.store.memory.MemoryPersistenceAdapter");
	private String adapter;

	private JmsProvider(String adapter) {
		this.adapter = adapter;
	}

	public String getAdapter() {
		return adapter;
	}
}