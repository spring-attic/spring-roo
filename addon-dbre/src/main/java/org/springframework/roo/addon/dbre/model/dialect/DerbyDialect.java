package org.springframework.roo.addon.dbre.model.dialect;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.addon.dbre.model.Schema;

/**
 * An SQL dialect for the Derby database.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class DerbyDialect extends AbstractDialect implements Dialect {

    public String getQuerySequencesString(final Schema schema) {
        Validate.notNull(schema, "Schema required");
        return "SELECT SEQUENCENAME FROM SYS.SYSSEQUENCES WHERE SEQUENCENAME = '"
                + schema.getName() + "'";
    }
}
