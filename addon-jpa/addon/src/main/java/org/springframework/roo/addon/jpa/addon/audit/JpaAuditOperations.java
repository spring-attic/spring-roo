package org.springframework.roo.addon.jpa.addon.audit;

import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Feature;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.maven.Pom;

/**
 * Interface that defines all the available operations for Jpa Audit feature.
 * 
 * @author Sergio Clares
 * @author Juan Carlos García
 * @since 2.0
 */
public interface JpaAuditOperations extends Feature {

  String AUDIT_FEATURE_NAME = FeatureNames.AUDIT;

  /**
   * Defines if Jpa Audit setup is available to be installed
   * 
   * @return true if JPA Audit is available to be installed on current project
   */
  boolean isJpaAuditSetupPossible();

  /**
   * Defines if is possible to audit some existing JPA Entity 
   * 
   * @return true if is possible to audit some existing entity
   */
  boolean isJpaAuditAddPossible();

  /**
   * Setups the necessary components to use JPA Audit
   * 
   * @param module where JPA Audit should be installed
   * 
   */
  void setupJpaAudit(Pom module);

  /**
   * 
   * Marks an entity as auditable and include the necessary fields inside the entity to 
   * be audited.
   * 
   * @param entity
   * @param createdDateColumn
   * @param modifiedDateColumn
   * @param createdByColumn
   * @param modifiedByColumn
   */
  void addJpaAuditToEntity(JavaType entity, String createdDateColumn, String modifiedDateColumn,
      String createdByColumn, String modifiedByColumn);
}
