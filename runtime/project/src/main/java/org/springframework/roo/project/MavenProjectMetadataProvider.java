package org.springframework.roo.project;

import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.file.monitor.event.FileEvent;
import org.springframework.roo.file.monitor.event.FileEventListener;
import org.springframework.roo.file.monitor.event.FileOperation;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataItem;
import org.springframework.roo.metadata.MetadataProvider;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.maven.Pom;

/**
 * Provides {@link ProjectMetadata}.
 * <p>
 * For simplicity of operation, this is the only implementation shipping with
 * ROO that supports {@link ProjectMetadata}.
 * 
 * @author Ben Alex
 * @author Stefan Schmidt
 * @author Alan Stewart
 * @since 1.0
 */
@Component
@Service
public class MavenProjectMetadataProvider implements MetadataProvider, FileEventListener {

  static final String POM_RELATIVE_PATH = "/pom.xml";

  private static final String PROVIDES_TYPE =
      MetadataIdentificationUtils.create(MetadataIdentificationUtils
          .getMetadataClass(ProjectMetadata.getProjectIdentifier("")));

  @Reference
  FileManager fileManager;
  @Reference
  private PomManagementService pomManagementService;

  public MetadataItem get(final String metadataId) {
    Validate.isTrue(ProjectMetadata.isValid(metadataId),
        "Unexpected metadata request '%s' for this provider", metadataId);
    // Just rebuild on demand. We always do this as we expect
    // MetadataService to cache on our behalf

    final Pom pom =
        pomManagementService.getPomFromModuleName(ProjectMetadata.getModuleName(metadataId));
    // Read the file, if it is available
    if (pom == null || !fileManager.exists(pom.getPath())) {
      return null;
    }

    return new ProjectMetadata(pom);
  }

  public String getProvidesType() {
    return PROVIDES_TYPE;
  }

  public void onFileEvent(final FileEvent fileEvent) {
    Validate.notNull(fileEvent, "File event required");

    if (fileEvent.getFileDetails().getCanonicalPath().endsWith(POM_RELATIVE_PATH)) {
      // Something happened to the POM

      // Don't notify if we're shutting down
      if (fileEvent.getOperation() == FileOperation.MONITORING_FINISH) {
        return;
      }

      // Retrieval will cause an eviction and notification
      pomManagementService.getPomFromPath(fileEvent.getFileDetails().getCanonicalPath());
    }
  }
}
