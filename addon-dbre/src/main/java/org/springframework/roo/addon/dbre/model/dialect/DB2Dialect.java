package org.springframework.roo.addon.dbre.model.dialect;

import org.springframework.roo.addon.dbre.model.Schema;

/**
 * An SQL dialect for the DB2 database.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class DB2Dialect extends AbstractDialect implements Dialect {

	public boolean supportsSequences() {
		return true;
	}

	public String getQuerySequencesString(Schema schema) {
		return "select seqname from sysibm.syssequences" + (schema != null ? " where seqschema = '" + schema.getName().toUpperCase() + "'" : "");
	}

	public String getSequenceNextValString(Schema schema, String sequenceName) {
		return "values nextval for " + (schema != null ? schema.getName().toUpperCase() + "." : "") + sequenceName;
	}
}
