package org.springframework.roo.addon.dbre.model.dialect;

import org.springframework.roo.addon.dbre.model.Schema;

/**
 * An SQL dialect for the HSQLDB database.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class HSQLDialect extends AbstractDialect implements Dialect {

	public String getQuerySequencesString(Schema schema) {
		return "select sequence_name from system_sequences";
	}
}
