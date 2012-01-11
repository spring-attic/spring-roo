package org.springframework.roo.addon.dbre.model.dialect;

import org.springframework.roo.addon.dbre.model.Schema;

/**
 * Represents a dialect of SQL implemented by a particular RDBMS.
 * <p>
 * Support for querying sequences is only provided at this stage.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public interface Dialect {

    String getQuerySequencesString(Schema schema) throws RuntimeException;

    boolean supportsSequences();
}
