package org.springframework.roo.addon.javabean;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.StringUtils;

/**
 * Provides {@link JavaBeanMetadata}.
 *
 * @author Ben Alex
 * @since 1.0
 */
@Component(immediate = true)
@Service
public final class JavaBeanMetadataProvider extends AbstractItdMetadataProvider {
	private static final JavaType ROO_ENTITY = new JavaType("org.springframework.roo.addon.entity.RooEntity");
	@Reference private ProjectOperations projectOperations;
	@Reference private TypeLocationService typeLocationService;
	private Set<String> producedMids = new LinkedHashSet<String>();
	private Boolean wasGaeEnabled = null;

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.addNotificationListener(this);
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(new JavaType(RooJavaBean.class.getName()));
	}

	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.removeNotificationListener(this);
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(new JavaType(RooJavaBean.class.getName()));
	}

	// We need to notified when ProjectMetadata changes in order to handle JPA <-> GAE persistence changes
	@Override
	protected void notifyForGenericListener(String upstreamDependency) {
		// If the upstream dependency is null or invalid do not continue
		if (!StringUtils.hasText(upstreamDependency) || !MetadataIdentificationUtils.isValid(upstreamDependency)) {
			return;
		}
		// If the upstream dependency isn't ProjectMetadata do not continue
		if (!upstreamDependency.equals(ProjectMetadata.getProjectIdentifier())) {
			return;
		}
		ProjectMetadata projectMetadata = projectOperations.getProjectMetadata();
		// If ProjectMetadata isn't valid do not continue
		if (projectMetadata == null || !projectMetadata.isValid()) {
			return;
		}
		boolean isGaeEnabled = projectMetadata.isGaeEnabled();
		// We need to determine if the persistence state has changed, we do this by comparing the last known state to the current state
		boolean hasGaeStateChanged = wasGaeEnabled == null || isGaeEnabled != wasGaeEnabled;
		if (hasGaeStateChanged) {
			wasGaeEnabled = isGaeEnabled;
			for (String producedMid : producedMids) {
				metadataService.get(producedMid, true);
			}
		}
	}

	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		JavaBeanAnnotationValues annotationValues = new JavaBeanAnnotationValues(governorPhysicalTypeMetadata);
		if (!annotationValues.isAnnotationFound()) {
			return null;
		}

		// Work out the MIDs of the other metadata we depend on
		ProjectMetadata projectMetadata = projectOperations.getProjectMetadata();
		if (projectMetadata == null || !projectMetadata.isValid()) {
			return null;
		}

		Map<FieldMetadata, FieldMetadata> declaredFields = new LinkedHashMap<FieldMetadata, FieldMetadata>();
		PhysicalTypeDetails physicalTypeDetails = governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();
		if (physicalTypeDetails != null && physicalTypeDetails instanceof ClassOrInterfaceTypeDetails) {
			ClassOrInterfaceTypeDetails governorTypeDetails = (ClassOrInterfaceTypeDetails) physicalTypeDetails;
			for (FieldMetadata field : governorTypeDetails.getDeclaredFields()) {
				declaredFields.put(field, isGaeInterested(field));
			}
		}

		// In order to handle switching between GAE and JPA produced MIDs need to be remembered so they can be regenerated on JPA <-> GAE switch
		producedMids.add(metadataIdentificationString);
		
		return new JavaBeanMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, annotationValues, declaredFields);
	}

	private FieldMetadata isGaeInterested(FieldMetadata field) {
		// We are not interested if the field is annotated with @javax.persistence.Transient
		for (AnnotationMetadata annotationMetadata : field.getAnnotations()) {
			if (annotationMetadata.getAnnotationType().getFullyQualifiedTypeName().equals("javax.persistence.Transient")) {
				return null;
			}
		}
		JavaType fieldType = field.getFieldType();
		// If the field is a common collection type we need to get the element type
		if (fieldType.isCommonCollectionType()) {
			if (fieldType.getParameters().isEmpty()) {
				return null;
			}
			fieldType = fieldType.getParameters().get(0);
		}

		try {
			ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails = typeLocationService.getClassOrInterface(fieldType);
			FieldMetadata identifierField = null;
			if (projectOperations.getProjectMetadata().isGaeEnabled() && MemberFindingUtils.getTypeAnnotation(classOrInterfaceTypeDetails, ROO_ENTITY) != null) {
				identifierField = getIdentifierField(classOrInterfaceTypeDetails);
			}
			return identifierField;
		} catch (Exception e) {
			// Don't need to know what happened so just return false;
			return null;
		}
	}

	public String getItdUniquenessFilenameSuffix() {
		return "JavaBean";
	}

	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = JavaBeanMetadata.getJavaType(metadataIdentificationString);
		Path path = JavaBeanMetadata.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}

	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return JavaBeanMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return JavaBeanMetadata.getMetadataIdentiferType();
	}

	private FieldMetadata getIdentifierField(ClassOrInterfaceTypeDetails governorTypeDetails) {
		for (AnnotationMetadata annotation : governorTypeDetails.getAnnotations()) {
			if (!annotation.getAnnotationType().getFullyQualifiedTypeName().equals(ROO_ENTITY.getFullyQualifiedTypeName())) {
				continue;
			}
			AnnotationAttributeValue<?> value = annotation.getAttribute(new JavaSymbolName("identifierField"));
			if (value != null) {
				FieldMetadata possibleIdentifierField = MemberFindingUtils.getField(governorTypeDetails, new JavaSymbolName(String.valueOf(value.getValue())));
				for (AnnotationMetadata fieldAnnotation : possibleIdentifierField.getAnnotations()) {
					if (fieldAnnotation.getAnnotationType().getFullyQualifiedTypeName().equals("javax.persistence.Id")) {
						return possibleIdentifierField;
					}
				}
			} else {
				for (MemberHoldingTypeDetails member : getMemberDetails(governorTypeDetails.getName()).getDetails()) {
					for (FieldMetadata field : member.getDeclaredFields()) {
						if (field.getFieldName().getSymbolName().equals("id")) {
							return field;
						}
					}
				}
			}
		}
		return null;
	}
}
