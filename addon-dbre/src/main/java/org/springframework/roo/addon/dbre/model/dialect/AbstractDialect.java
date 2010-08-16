package org.springframework.roo.addon.dbre.model.dialect;

import org.springframework.roo.addon.dbre.model.Schema;

/**
 * Abstract base class for database {@link Dialect}s.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public abstract class AbstractDialect {

	public AbstractDialect() {
		super();
	}

	public boolean supportsSequences() {
		return false;
	}
	
	public String getQuerySequencesString(Schema schema) {
		throw new UnsupportedOperationException(getDatabaseName() + " does not support sequences");
	}

	public String getSequenceNextValString(Schema schema, String sequenceName) {
		throw new UnsupportedOperationException(getDatabaseName() + " does not support sequences");
	}
	
	protected String getDatabaseName() {
		return "";
	}
}