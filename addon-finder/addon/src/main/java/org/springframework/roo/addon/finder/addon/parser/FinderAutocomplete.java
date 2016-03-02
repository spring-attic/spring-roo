package org.springframework.roo.addon.finder.addon.parser;

import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaType;

/**
 * Interface that provides necessary API to obtain useful 
 * information during finder autocomplete
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public interface FinderAutocomplete {

  /**
   * Operation to obtain entity details
   * 
   * @param entity
   * @return MemberDetails that contains entity information
   */
  MemberDetails getEntityDetails(JavaType entity);

}
