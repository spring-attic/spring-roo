package org.springframework.roo.addon.dbre.model.dialect;

import org.springframework.roo.addon.dbre.model.Schema;
import org.springframework.roo.support.util.Assert;

/**
 * An SQL dialect for the PostgreSQL database.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class PostgreSQLDialect extends AbstractDialect implements Dialect {

	public String getQuerySequencesString(Schema schema) {
		Assert.notNull(schema, "Schema required");
		return "SELECT RELNAME FROM PG_CLASS WHERE RELKIND = 'S' AND RELNAMESPACE IN (SELECT OID FROM PG_NAMESPACE WHERE NSPNAME = '" + schema.getName() + "')";
	}
}
