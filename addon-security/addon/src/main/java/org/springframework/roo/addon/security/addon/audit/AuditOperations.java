package org.springframework.roo.addon.security.addon.audit;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.project.Feature;
import org.springframework.roo.project.FeatureNames;

/**
 * Interface for {@link AuditOperationsImpl}.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
public interface AuditOperations extends Feature {

  String AUDIT_FEATURE_NAME = FeatureNames.AUDIT;

  boolean isAuditSetupPossible();

  void setupAudit(JavaPackage javaPackage);
}
