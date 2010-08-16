package org.springframework.roo.addon.dbre.model.dialect;

import org.springframework.roo.addon.dbre.model.Schema;

/**
 * An SQL dialect for the Oracle 10 and 11 databases.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class OracleDialect extends AbstractDialect implements Dialect {

	public boolean supportsSequences() {
		return true;
	}

	public String getQuerySequencesString(Schema schema) {
		return "select sequence_name from user_sequences";
	}

	public String getSequenceNextValString(Schema schema, String sequenceName) {
		return "select " + (schema != null ? schema.getName() + "." : "") + sequenceName + ".nextval from dual";
	}
}
