package org.springframework.roo.addon.dbre.model.dialect;

import org.springframework.roo.addon.dbre.model.Schema;

/**
 * An SQL dialect for the H2 database.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class H2Dialect extends AbstractDialect implements Dialect {

    public String getQuerySequencesString(final Schema schema) {
        return "select sequence_name from system_sequences";
    }
}
