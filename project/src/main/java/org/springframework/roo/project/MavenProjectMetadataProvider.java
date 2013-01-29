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
import org.springframework.roo.uaa.UaaRegistrationService;
import org.springframework.uaa.client.UaaDetectedProducts;
import org.springframework.uaa.client.UaaDetectedProducts.ProductInfo;
import org.springframework.uaa.client.VersionHelper;
import org.springframework.uaa.client.protobuf.UaaClient.Product;

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
@Component(immediate = true)
@Service
public class MavenProjectMetadataProvider implements MetadataProvider,
        FileEventListener {

    static final String POM_RELATIVE_PATH = "/pom.xml";

    private static final String PROVIDES_TYPE = MetadataIdentificationUtils
            .create(MetadataIdentificationUtils
                    .getMetadataClass(ProjectMetadata.getProjectIdentifier("")));

    @Reference FileManager fileManager;
    @Reference private PomManagementService pomManagementService;
    @Reference private UaaDetectedProducts uaaDetectedProducts;
    @Reference private UaaRegistrationService uaaRegistrationService;

    public MetadataItem get(final String metadataId) {
        Validate.isTrue(ProjectMetadata.isValid(metadataId),
                "Unexpected metadata request '%s' for this provider",
                metadataId);
        // Just rebuild on demand. We always do this as we expect
        // MetadataService to cache on our behalf

        final Pom pom = pomManagementService
                .getPomFromModuleName(ProjectMetadata.getModuleName(metadataId));
        // Read the file, if it is available
        if (pom == null || !fileManager.exists(pom.getPath())) {
            return null;
        }

        final JavaPackage topLevelPackage = new JavaPackage(pom.getGroupId());
        // Update UAA with the project name
        uaaRegistrationService.registerProject(
                UaaRegistrationService.SPRING_ROO,
                topLevelPackage.getFullyQualifiedPackageName());

        // Update UAA with the well-known Spring-related open source
        // dependencies
        for (final ProductInfo productInfo : uaaDetectedProducts
                .getDetectedProductInfos()) {
            if (productInfo.getProductName().equals(
                    UaaRegistrationService.SPRING_ROO.getName())) {
                // No need to register with a less robust pom.xml-declared
                // dependency metadata when we did it ourselves with a proper
                // bundle version number lookup a moment ago...
                continue;
            }
            if (productInfo.getProductName().equals(
                    UaaDetectedProducts.SPRING_UAA.getProductName())) {
                // No need to register Spring UAA as this happens automatically
                // internal to UAA
                continue;
            }
            final Dependency dependency = new Dependency(
                    productInfo.getGroupId(), productInfo.getArtifactId(),
                    "version_is_ignored_for_searching");
            final Set<Dependency> dependenciesExcludingVersion = pom
                    .getDependenciesExcludingVersion(dependency);
            if (!dependenciesExcludingVersion.isEmpty()) {
                // This dependency was detected
                final Dependency first = dependenciesExcludingVersion
                        .iterator().next();
                // Convert the detected dependency into a Product as best we can
                String versionSequence = first.getVersion();
                // Version sequence given; see if it looks like a property
                if (versionSequence != null && versionSequence.startsWith("${")
                        && versionSequence.endsWith("}")) {
                    // Strip the ${ } from the version sequence
                    final String propertyName = versionSequence.replace("${",
                            "").replace("}", "");
                    final Set<Property> prop = pom
                            .getPropertiesExcludingValue(new Property(
                                    propertyName));
                    if (!prop.isEmpty()) {
                        // Take the first one's value and treat that as the
                        // version sequence
                        versionSequence = prop.iterator().next().getValue();
                    }
                }
                // Handle there being no version sequence
                if (versionSequence == null || "".equals(versionSequence)) {
                    versionSequence = "0.0.0.UNKNOWN";
                }
                final Product product = VersionHelper.getProduct(
                        productInfo.getProductName(), versionSequence);
                // Register the Spring Product with UAA
                uaaRegistrationService.registerProject(product,
                        topLevelPackage.getFullyQualifiedPackageName());
            }
        }

        return new ProjectMetadata(pom);
    }

    public String getProvidesType() {
        return PROVIDES_TYPE;
    }

    public void onFileEvent(final FileEvent fileEvent) {
        Validate.notNull(fileEvent, "File event required");

        if (fileEvent.getFileDetails().getCanonicalPath()
                .endsWith(POM_RELATIVE_PATH)) {
            // Something happened to the POM

            // Don't notify if we're shutting down
            if (fileEvent.getOperation() == FileOperation.MONITORING_FINISH) {
                return;
            }

            // Retrieval will cause an eviction and notification
            pomManagementService.getPomFromPath(fileEvent.getFileDetails()
                    .getCanonicalPath());
        }
    }
}