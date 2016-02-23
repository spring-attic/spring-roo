package org.springframework.roo.project.packaging;

import java.util.Collection;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;

/**
 * The Maven "esa" {@link PackagingProvider}
 * 
 * @author Juan Carlos García
 * @since 2.0.0
 */
@Component
@Service
public class EsaPackaging extends AbstractCorePackagingProvider {

  /**
   * Constructor
   */
  public EsaPackaging() {
    super("esa", "parent-esa-template.xml");
  }

  @Override
  protected void createOtherArtifacts(final JavaPackage topLevelPackage, final String module,
      final ProjectOperations projectOperations) {
    // No artifacts are applicable for POM modules
  }

  public Collection<Path> getPaths() {
    return null;
  }
}
