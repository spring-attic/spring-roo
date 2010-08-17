package org.springframework.roo.addon.dbre.model.dialect;

import org.springframework.roo.addon.dbre.model.Schema;
import org.springframework.roo.support.util.Assert;

/**
 * An SQL dialect for the DB2 database.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class DB2Dialect extends AbstractDialect implements Dialect {

	public String getQuerySequencesString(Schema schema) {
		Assert.notNull(schema, "Schema required");
		return "SELELCT SEQNAME FROM SYSIBM.SYSSEQUENCES WHRE SEQSCHEMA = '" + schema.getName() + "'";
	}
}
