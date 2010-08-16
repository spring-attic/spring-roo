package org.springframework.roo.addon.dbre.model.dialect;

import org.springframework.roo.addon.dbre.model.Schema;

/**
 * An SQL dialect for the PostgreSQL database.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class PostgreSQLDialect extends AbstractDialect implements Dialect {

	public boolean supportsSequences() {
		return true;
	}

	public String getQuerySequencesString(Schema schema) {
		return "select relname from pg_class where relkind = 'S'AND relnamespace IN ( SELECT oid FROM pg_namespace WHERE nspname = '" + schema.getName() + "')";
	}

	public String getSequenceNextValString(Schema schema, String sequenceName) {
		return "select nextval ('" + (schema != null ? schema.getName() + "." : "") + sequenceName + "')";
	}
}
