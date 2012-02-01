package org.springframework.roo.addon.dbre.model.dialect;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.addon.dbre.model.Schema;

/**
 * An SQL dialect for the PostgreSQL database.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class PostgreSQLDialect extends AbstractDialect implements Dialect {

    public String getQuerySequencesString(final Schema schema) {
        Validate.notNull(schema, "Schema required");
        return "SELECT RELNAME FROM PG_CLASS WHERE RELKIND = 'S' AND RELNAMESPACE IN (SELECT OID FROM PG_NAMESPACE WHERE NSPNAME = '"
                + schema.getName() + "')";
    }
}
