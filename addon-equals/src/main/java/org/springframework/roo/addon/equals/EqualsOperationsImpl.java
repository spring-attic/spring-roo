package org.springframework.roo.addon.equals;

import static org.springframework.roo.model.RooJavaType.ROO_EQUALS;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ArrayAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.CollectionUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;

/**
 * Implementation of {@link EqualsOperations}.
 *
 * @author Alan Stewart
 * @since 1.2.0
 */
@Component
@Service
public class EqualsOperationsImpl implements EqualsOperations {

	// Fields
	@Reference private ProjectOperations projectOperations;
	@Reference private TypeLocationService typeLocationService;
	@Reference private TypeManagementService typeManagementService;

	public void addEqualsAndHashCodeMethods(final JavaType javaType, final boolean appendSuper, final Set<String> excludeFields) {
		// Update pom.xml
		updateConfiguration();

		// Add @RooEquals annotation to class if not yet present
		ClassOrInterfaceTypeDetails cid = typeLocationService.getTypeDetails(javaType);
		if (cid == null || cid.getTypeAnnotation(ROO_EQUALS) != null) {
			return;
		}

		final AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(ROO_EQUALS);
		if (appendSuper) {
			annotationBuilder.addBooleanAttribute("appendSuper", appendSuper);
		}
		if (!CollectionUtils.isEmpty(excludeFields)) {
			List<StringAttributeValue> attributes = new ArrayList<StringAttributeValue>();
			for (String excludeField : excludeFields) {
				attributes.add(new StringAttributeValue(new JavaSymbolName("value"), excludeField));
			}
			annotationBuilder.addAttribute(new ArrayAttributeValue<StringAttributeValue>(new JavaSymbolName("excludeFields"), attributes));
		}

		final ClassOrInterfaceTypeDetailsBuilder classOrInterfaceTypeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(cid);
		classOrInterfaceTypeDetailsBuilder.addAnnotation(annotationBuilder.build());
		typeManagementService.createOrUpdateTypeOnDisk(classOrInterfaceTypeDetailsBuilder.build());
	}

	public void updateConfiguration() {
		// Update pom.xml with commons-lang dependency
		final Element configuration = XmlUtils.getConfiguration(getClass());
		final Element dependencyElement = XmlUtils.findFirstElement("/configuration/equals/dependencies/dependency", configuration);
		if (dependencyElement != null) {
			final Dependency dependency = new Dependency(dependencyElement);
			projectOperations.addDependency(projectOperations.getFocusedModuleName(), dependency);
		}
	}
}
