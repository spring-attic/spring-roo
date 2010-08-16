package org.springframework.roo.addon.dbre.model.dialect;

import org.springframework.roo.addon.dbre.model.Schema;

/**
 * Represents a dialect of SQL implemented by a particular RDBMS.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public interface Dialect {
	
	boolean supportsSequences();
	
	String getQuerySequencesString(Schema schema) throws RuntimeException;

	String getSequenceNextValString(Schema schema, String sequenceName) throws RuntimeException;
}
