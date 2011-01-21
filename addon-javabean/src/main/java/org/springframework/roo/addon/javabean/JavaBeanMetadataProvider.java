package org.springframework.roo.addon.javabean;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.beaninfo.BeanInfoMetadataProvider;
import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.operations.ClasspathOperations;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;

/**
 * Provides {@link JavaBeanMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component(immediate = true) 
@Service 
public final class JavaBeanMetadataProvider extends AbstractItdMetadataProvider {
	@Reference private ClasspathOperations classpathOperations;
	@Reference private BeanInfoMetadataProvider beanInfoMetadataProvider;

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		beanInfoMetadataProvider.addMetadataTrigger(new JavaType(RooJavaBean.class.getName()));
		addMetadataTrigger(new JavaType(RooJavaBean.class.getName()));
	}

	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		beanInfoMetadataProvider.removeMetadataTrigger(new JavaType(RooJavaBean.class.getName()));
		removeMetadataTrigger(new JavaType(RooJavaBean.class.getName()));
	}

	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		// Work out the MIDs of the other metadata we depend on
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		if (projectMetadata == null || !projectMetadata.isValid()) {
			return null;
		}

		List<JavaSymbolName> gaeFieldsOfInterest = new ArrayList<JavaSymbolName>();
		if (projectMetadata.isGaeEnabled()) {
			PhysicalTypeDetails physicalTypeDetails = governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();
			if (physicalTypeDetails != null && physicalTypeDetails instanceof ClassOrInterfaceTypeDetails) {
				ClassOrInterfaceTypeDetails governorTypeDetails = (ClassOrInterfaceTypeDetails) physicalTypeDetails;
				for (FieldMetadata field : governorTypeDetails.getDeclaredFields()) {
					if (isGaeInterested(field)) {
						gaeFieldsOfInterest.add(field.getFieldName());
					}
				}
			}
		}

		return new JavaBeanMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, gaeFieldsOfInterest);
	}

	private boolean isGaeInterested(FieldMetadata field) {
		boolean gaeInterested = false;
		boolean isTransient = false;

		// Check to see that the field is to the persisted and is not transient
		for (AnnotationMetadata annotation : field.getAnnotations()) {
			if (annotation.getAnnotationType().equals(new JavaType("javax.persistence.Transient"))) {
				isTransient = true;
				break;
			}
		}

		// Check to see that the field type is one that supports a relationship
		JavaType fieldType = field.getFieldType();
		if (!isTransient && !fieldType.isPrimitive() && !isBasicJavaType(fieldType)) {
			if (fieldType.isCommonCollectionType()) {
				fieldType = field.getFieldType().getParameters().get(0);
			}

			ClassOrInterfaceTypeDetails fieldTypeDetails = classpathOperations.getClassOrInterface(fieldType);
			for (AnnotationMetadata annotation : fieldTypeDetails.getAnnotations()) {
				// Have to check to see if field type is a RooEntity
				if (annotation.getAnnotationType().equals(new JavaType("org.springframework.roo.addon.entity.RooEntity"))) {
					gaeInterested = true;
					break;
				}
			}
		}

		return gaeInterested;
	}

	private boolean isBasicJavaType(JavaType javaType) {
		return JavaType.BOOLEAN_OBJECT.equals(javaType) || JavaType.CHAR_OBJECT.equals(javaType) || JavaType.STRING_OBJECT.equals(javaType) || JavaType.BYTE_OBJECT.equals(javaType) || JavaType.SHORT_OBJECT.equals(javaType) || JavaType.INT_OBJECT.equals(javaType) || JavaType.LONG_OBJECT.equals(javaType) || JavaType.FLOAT_OBJECT.equals(javaType) || JavaType.DOUBLE_OBJECT.equals(javaType) || new JavaType("java.util.Date").equals(javaType) || new JavaType("java.math.BigDecimal").equals(javaType);
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
