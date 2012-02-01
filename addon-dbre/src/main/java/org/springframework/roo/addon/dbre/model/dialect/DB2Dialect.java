package org.springframework.roo.addon.dbre.model.dialect;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.addon.dbre.model.Schema;

/**
 * An SQL dialect for the DB2 database.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class DB2Dialect extends AbstractDialect implements Dialect {

    public String getQuerySequencesString(final Schema schema) {
        Validate.notNull(schema, "Schema required");
        return "SELELCT SEQNAME FROM SYSIBM.SYSSEQUENCES WHRE SEQSCHEMA = '"
                + schema.getName() + "'";
    }
}
