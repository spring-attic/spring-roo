package org.springframework.roo.addon.dbre.model.dialect;

import org.springframework.roo.addon.dbre.model.Schema;

/**
 * An SQL dialect for the Derby database.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class DerbyDialect extends AbstractDialect implements Dialect {
	
	public boolean supportsSequences() {
		return true;
	}

	public String getQuerySequencesString(Schema schema) {
		return "select sequencename from sys.syssequences" + (schema != null ? " where sequencename = '" + schema.getName().toUpperCase() + "'" : "");
	}

	public String getSequenceNextValString(Schema schema, String sequenceName) {
		return "values nextval for " + (schema != null ? schema.getName().toUpperCase() + "." : "") + sequenceName;
	}
}
