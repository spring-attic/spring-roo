package org.springframework.roo.addon.json;

import static org.springframework.roo.model.RooJavaType.ROO_JAVA_BEAN;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.ProjectOperations;

/**
 * Implementation of addon-json operations interface.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component
@Service
public class JsonOperationsImpl implements JsonOperations {

    @Reference private ProjectOperations projectOperations;
    @Reference private TypeLocationService typeLocationService;
    @Reference private TypeManagementService typeManagementService;

    public void annotateAll() {
        annotateAll(false, false);
    }

    public void annotateAll(final boolean deepSerialize) {
        annotateAll(deepSerialize, false);
    }

    public void annotateAll(final boolean deepSerialize,
            final boolean iso8601Dates) {
        for (final JavaType type : typeLocationService
                .findTypesWithAnnotation(ROO_JAVA_BEAN)) {
            annotateType(type, "", deepSerialize, iso8601Dates);
        }
    }

    public void annotateType(final JavaType javaType, final String rootName) {
        annotateType(javaType, rootName, false);
    }

    public void annotateType(final JavaType javaType, final String rootName,
            final boolean deepSerialize) {
        annotateType(javaType, rootName, false, false);
    }

    public void annotateType(final JavaType javaType, final String rootName,
            final boolean deepSerialize, final boolean iso8601Dates) {
        Validate.notNull(javaType, "Java type required");

        final ClassOrInterfaceTypeDetails cid = typeLocationService
                .getTypeDetails(javaType);
        if (cid == null) {
            throw new IllegalArgumentException("Cannot locate source for '"
                    + javaType.getFullyQualifiedTypeName() + "'");
        }

        if (MemberFindingUtils.getAnnotationOfType(cid.getAnnotations(),
                RooJavaType.ROO_JSON) == null) {
            final AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(
                    RooJavaType.ROO_JSON);
            if (rootName != null && rootName.length() > 0) {
                annotationBuilder.addStringAttribute("rootName", rootName);
            }
            if (deepSerialize) {
                annotationBuilder.addBooleanAttribute("deepSerialize", true);
            }
            if (iso8601Dates) {
                annotationBuilder.addBooleanAttribute("iso8601Dates", true);
            }
            final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                    cid);
            cidBuilder.addAnnotation(annotationBuilder);
            typeManagementService.createOrUpdateTypeOnDisk(cidBuilder.build());
        }
    }

    public boolean isJsonInstallationPossible() {
        return projectOperations.isFocusedProjectAvailable();
    }
}