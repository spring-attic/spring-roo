package org.springframework.roo.addon.dbre.addon.model.dialect;

import org.springframework.roo.addon.dbre.addon.model.Schema;

/**
 * An SQL dialect for the HSQLDB database.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class HSQLDialect extends AbstractDialect implements Dialect {

  public String getQuerySequencesString(final Schema schema) {
    return "select sequence_name from system_sequences";
  }
}
