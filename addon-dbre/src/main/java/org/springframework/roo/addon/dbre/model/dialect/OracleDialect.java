package org.springframework.roo.addon.dbre.model.dialect;

import org.springframework.roo.addon.dbre.model.Schema;
import org.springframework.roo.support.util.Assert;

/**
 * An SQL dialect for the Oracle 10 and 11 databases.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class OracleDialect extends AbstractDialect implements Dialect {

	public String getQuerySequencesString(Schema schema) {
		Assert.notNull(schema, "Schema required");
		return "SELECT SEQUENCE_NAME FROM ALL_SEQUENCES WHERE SEQUENCE_OWNER = '" + schema.getName() + "'";
	}
}
