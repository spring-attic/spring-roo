package org.springframework.roo.addon.dbre.model.dialect;

import org.springframework.roo.addon.dbre.model.Schema;
import org.springframework.roo.support.util.Assert;

/**
 * An SQL dialect for the Derby database.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class DerbyDialect extends AbstractDialect implements Dialect {
	
	public String getQuerySequencesString(Schema schema) {
		Assert.notNull(schema, "Schema required");
		return "SELECT SEQUENCENAME FROM SYS.SYSSEQUENCES WHERE SEQUENCENAME = '" + schema.getName() + "'";
	}
}
