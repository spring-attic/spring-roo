package org.springframework.roo.addon.web.mvc.controller.finder;

import java.util.HashSet;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.web.mvc.controller.ControllerOperations;
import org.springframework.roo.addon.web.mvc.controller.scaffold.WebScaffoldAnnotationValues;
import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.uaa.client.util.Assert;

/**
 * Implementation of {@link WebFinderOperations}
 *
 * @author Stefan Schmidt
 * @since 1.2.0
 */
@Component
@Service
public class WebFinderOperationsImpl implements WebFinderOperations {

	// Fields
	@Reference private ControllerOperations controllerOperations;
	@Reference private MetadataService metadataService;
	@Reference private TypeLocationService typeLocationService;
	@Reference private TypeManagementService typeManagementService;

	public boolean isWebFinderInstallationPossible() {
		return controllerOperations.isControllerInstallationPossible();	}

	public void annotateAll() {
		// First, find all entities with finders.
		Set<JavaType> finderEntities = new HashSet<JavaType>();
		for (ClassOrInterfaceTypeDetails cod : typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_JPA_ACTIVE_RECORD)) {
			if (MemberFindingUtils.getAnnotationOfType(cod.getAnnotations(), RooJavaType.ROO_JPA_ACTIVE_RECORD).getAttribute("finders") != null) {
				finderEntities.add(cod.getName());
			}
		}

		// Second, find controllers for those entities.
		for (ClassOrInterfaceTypeDetails cod : typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_WEB_SCAFFOLD)) {
			PhysicalTypeMetadata ptm = (PhysicalTypeMetadata) metadataService.get(typeLocationService.getPhysicalTypeIdentifier(cod.getName()));
			Assert.notNull(ptm, "Java source code unavailable for type " + cod.getName().getFullyQualifiedTypeName());
			WebScaffoldAnnotationValues webScaffoldAnnotationValues = new WebScaffoldAnnotationValues(ptm);
			for (JavaType finderEntity : finderEntities) {
				if (finderEntity.equals(webScaffoldAnnotationValues.getFormBackingObject())) {
					annotateType(cod.getName(), finderEntity);
					break;
				}
			}
		}
	}

	public void annotateType(final JavaType controllerType, final JavaType entityType) {
		Assert.notNull(controllerType, "Controller type required");
		Assert.notNull(entityType, "Entity type required");
		
		String id = typeLocationService.getPhysicalTypeIdentifier(controllerType);
		if (id == null) {
			throw new IllegalArgumentException("Cannot locate source for '" + controllerType.getFullyQualifiedTypeName() + "'");
		}

		// Obtain the physical type and itd mutable details
		PhysicalTypeMetadata ptm = (PhysicalTypeMetadata) metadataService.get(id);
		Assert.notNull(ptm, "Java source code unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(id));
		WebScaffoldAnnotationValues webScaffoldAnnotationValues = new WebScaffoldAnnotationValues(ptm);
		if (!webScaffoldAnnotationValues.isAnnotationFound() || !webScaffoldAnnotationValues.getFormBackingObject().equals(entityType)) {
			throw new IllegalArgumentException("Aborting, this controller type does not manage the " + entityType.getSimpleTypeName() + " form backing type.");
		}
		
		PhysicalTypeDetails ptd = ptm.getMemberHoldingTypeDetails();
		Assert.notNull(ptd, "Java source code details unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(id));
		ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails = (ClassOrInterfaceTypeDetails) ptd;
		if (null == MemberFindingUtils.getAnnotationOfType(classOrInterfaceTypeDetails.getAnnotations(), RooJavaType.ROO_WEB_FINDER)) {
			ClassOrInterfaceTypeDetailsBuilder classOrInterfaceTypeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(classOrInterfaceTypeDetails);
			classOrInterfaceTypeDetailsBuilder.addAnnotation(new AnnotationMetadataBuilder(RooJavaType.ROO_WEB_FINDER));
			typeManagementService.createOrUpdateTypeOnDisk(classOrInterfaceTypeDetailsBuilder.build());
		}
	}
}
