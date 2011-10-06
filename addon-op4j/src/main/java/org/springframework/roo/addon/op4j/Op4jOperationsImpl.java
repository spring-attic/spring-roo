package org.springframework.roo.addon.op4j;

import static org.springframework.roo.model.RooJavaType.ROO_OP4J;

import java.util.ArrayList;
import java.util.List;

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
import org.springframework.roo.support.util.Assert;
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

	// Fields
	@Reference private ProjectOperations projectOperations;
	@Reference private TypeLocationService typeLocationService;
	@Reference private TypeManagementService typeManagementService;

	public boolean isOp4jAvailable() {
		return projectOperations.isProjectAvailable();
	}

	public void annotateType(final JavaType javaType) {
		Assert.notNull(javaType, "Java type required");

		ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails = typeLocationService.findClassOrInterface(javaType);
		if (classOrInterfaceTypeDetails == null) {
			throw new IllegalArgumentException("Cannot locate source for '" + javaType.getFullyQualifiedTypeName() + "'");
		}

		if (MemberFindingUtils.getAnnotationOfType(classOrInterfaceTypeDetails.getAnnotations(), ROO_OP4J) == null) {
			AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(ROO_OP4J);
			ClassOrInterfaceTypeDetailsBuilder classOrInterfaceTypeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(classOrInterfaceTypeDetails);
			classOrInterfaceTypeDetailsBuilder.addAnnotation(annotationBuilder);
			typeManagementService.createOrUpdateTypeOnDisk(classOrInterfaceTypeDetails);
		}
	}

	public void setup() {
		Element configuration = XmlUtils.getConfiguration(getClass());

		List<Dependency> dependencies = new ArrayList<Dependency>();
		List<Element> op4jDependencies = XmlUtils.findElements("/configuration/op4j/dependencies/dependency", configuration);
		for (Element dependencyElement : op4jDependencies) {
			dependencies.add(new Dependency(dependencyElement));
		}
		projectOperations.addDependencies(dependencies);
	}
}