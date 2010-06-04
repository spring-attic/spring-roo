package org.springframework.roo.addon.dbre.db.metadata;

/**
 * Specifies methods to return database-specific artifacts such as sequences.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public interface Dialect {
	
	boolean supportsSequences();

	String getQuerySequencesString();
}
