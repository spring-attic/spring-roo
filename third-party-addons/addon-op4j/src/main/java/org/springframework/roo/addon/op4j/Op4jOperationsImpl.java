package org.springframework.roo.addon.op4j;

import static org.springframework.roo.model.RooJavaType.ROO_OP4J;

import java.util.ArrayList;
import java.util.List;

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
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;

/**
 * Implementation of commands that are available via the Roo shell.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component
@Service
public class Op4jOperationsImpl implements Op4jOperations {

    @Reference private ProjectOperations projectOperations;
    @Reference private TypeLocationService typeLocationService;
    @Reference private TypeManagementService typeManagementService;

    public void annotateType(final JavaType javaType) {
        Validate.notNull(javaType, "Java type required");

        final ClassOrInterfaceTypeDetails cid = typeLocationService
                .getTypeDetails(javaType);
        if (cid == null) {
            throw new IllegalArgumentException("Cannot locate source for '"
                    + javaType.getFullyQualifiedTypeName() + "'");
        }

        if (MemberFindingUtils.getAnnotationOfType(cid.getAnnotations(),
                ROO_OP4J) == null) {
            final AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(
                    ROO_OP4J);
            final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                    cid);
            cidBuilder.addAnnotation(annotationBuilder);
            typeManagementService.createOrUpdateTypeOnDisk(cid);
        }
    }

    public boolean isOp4jInstallationPossible() {
        return projectOperations.isFocusedProjectAvailable();
    }

    public void setup() {
        final Element configuration = XmlUtils.getConfiguration(getClass());

        final List<Dependency> dependencies = new ArrayList<Dependency>();
        final List<Element> op4jDependencies = XmlUtils.findElements(
                "/configuration/op4j/dependencies/dependency", configuration);
        for (final Element dependencyElement : op4jDependencies) {
            dependencies.add(new Dependency(dependencyElement));
        }
        projectOperations.addDependencies(
                projectOperations.getFocusedModuleName(), dependencies);
    }
}