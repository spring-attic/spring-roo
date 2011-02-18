package org.springframework.roo.addon.javabean;

import java.util.LinkedHashMap;
import java.util.Map;

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
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;

/**
 * Provides {@link JavaBeanMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component(immediate = true) 
@Service 
public final class JavaBeanMetadataProvider extends AbstractItdMetadataProvider {
	@Reference private ProjectOperations projectOperations;
	@Reference private TypeLocationService typeLocationService;
	
	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(new JavaType(RooJavaBean.class.getName()));
	}

	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(new JavaType(RooJavaBean.class.getName()));
	}

	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		// Work out the MIDs of the other metadata we depend on
		ProjectMetadata projectMetadata = projectOperations.getProjectMetadata();
		if (projectMetadata == null || !projectMetadata.isValid()) {
			return null;
		}

		Map<FieldMetadata, Boolean> declaredFields = new LinkedHashMap<FieldMetadata, Boolean>();
		PhysicalTypeDetails physicalTypeDetails = governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();
		if (physicalTypeDetails != null && physicalTypeDetails instanceof ClassOrInterfaceTypeDetails) {
			ClassOrInterfaceTypeDetails governorTypeDetails = (ClassOrInterfaceTypeDetails) physicalTypeDetails;
			for (FieldMetadata field : governorTypeDetails.getDeclaredFields()) {
				declaredFields.put(field, (projectMetadata.isGaeEnabled() && isGaeInterested(field)));
			}
		}

		return new JavaBeanMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, declaredFields);
	}

	private boolean isGaeInterested(FieldMetadata field) {
		JavaType fieldType = field.getFieldType();
		if (fieldType.isCommonCollectionType()) {
			fieldType = field.getFieldType().getParameters().get(0);
		}
		
		try {
			ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails = typeLocationService.getClassOrInterface(fieldType);
			return MemberFindingUtils.getTypeAnnotation(classOrInterfaceTypeDetails, new JavaType("org.springframework.roo.addon.entity.RooEntity")) != null;
		} catch (Exception e) {
			// Don't need to know what happened so just return false;
			return false;
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
}
