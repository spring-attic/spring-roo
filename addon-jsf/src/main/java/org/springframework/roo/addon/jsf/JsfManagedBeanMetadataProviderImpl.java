package org.springframework.roo.addon.jsf;

import java.util.LinkedList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.util.Assert;

/**
 * Implementation of {@link JsfManagedBeanMetadataProvider}.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
@Component(immediate = true) 
@Service 
public final class JsfManagedBeanMetadataProviderImpl extends AbstractItdMetadataProvider implements JsfManagedBeanMetadataProvider {

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(new JavaType(RooJsfManagedBean.class.getName()));
	}

	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(new JavaType(RooJsfManagedBean.class.getName()));
	}

	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		// We need to parse the annotation, which we expect to be present
		JsfAnnotationValues annotationValues = new JsfAnnotationValues(governorPhysicalTypeMetadata);
		JavaType entityType = annotationValues.getEntity();
		if (!annotationValues.isAnnotationFound() || entityType == null) {
			return null;
		}

		// Lookup the form backing object's metadata
		MemberDetails memberDetails = getMemberDetails(entityType);
		if (memberDetails == null) {
			return null;
		}

		MemberHoldingTypeDetails persistenceMemberHoldingTypeDetails = MemberFindingUtils.getMostConcreteMemberHoldingTypeDetailsWithTag(memberDetails, PersistenceCustomDataKeys.PERSISTENT_TYPE);
		if (persistenceMemberHoldingTypeDetails == null) {
			return null;
		}
		
		PluralMetadata pluralMetadata = (PluralMetadata) metadataService.get(PluralMetadata.createIdentifier(entityType, Path.SRC_MAIN_JAVA));
		Assert.notNull(pluralMetadata, "Could not determine plural for '" + entityType.getSimpleTypeName() + "'");
		String plural = pluralMetadata.getPlural();
		
		List<MethodMetadata> locatedAccessors = findAccessors(memberDetails, metadataIdentificationString);
		
		// We need to be informed if our dependent metadata changes
		metadataDependencyRegistry.registerDependency(persistenceMemberHoldingTypeDetails.getDeclaredByMetadataId(), metadataIdentificationString);

		return new JsfManagedBeanMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, annotationValues, memberDetails, plural, locatedAccessors);
	}

	private List<MethodMetadata> findAccessors(MemberDetails memberDetails, String metadataIdentificationString) {
		List<MethodMetadata> locatedAccessors = new LinkedList<MethodMetadata>();
		
		int counter = 0;
		for (MethodMetadata method : MemberFindingUtils.getMethods(memberDetails)) {
			if (counter < 4 && isMethodOfInterest(method, memberDetails)) {
				counter++;
				locatedAccessors.add(method);
				// Track any changes to that method (eg it goes away)
				metadataDependencyRegistry.registerDependency(method.getDeclaredByMetadataId(), metadataIdentificationString);
			} 
			
			if (BeanInfoUtils.isAccessorMethod(method) && isApplicationType(method.getReturnType())) {
				// Track any related java types in the project
				metadataDependencyRegistry.registerDependency(method.getDeclaredByMetadataId(), metadataIdentificationString);
			}
		}
		
		return locatedAccessors;
	}
	
	private boolean isMethodOfInterest(MethodMetadata method, MemberDetails memberDetails) {
		if (!BeanInfoUtils.isAccessorMethod(method)) {
			return false; // Only interested in accessors
		}
		if (method.getCustomData().keySet().contains(PersistenceCustomDataKeys.IDENTIFIER_ACCESSOR_METHOD) || method.getCustomData().keySet().contains(PersistenceCustomDataKeys.VERSION_ACCESSOR_METHOD)) {
			return false; // Only interested in methods which are not accessors for persistence version or id fields
		}
		FieldMetadata field = BeanInfoUtils.getFieldForPropertyName(memberDetails, BeanInfoUtils.getPropertyNameForJavaBeanMethod(method));
		if (field == null) {
			return false;
		}
		JavaType fieldType = field.getFieldType();
		if (fieldType.isCommonCollectionType() || fieldType.isArray() // Exclude collections and arrays
				|| isApplicationType(fieldType) // Exclude references to other domain objects as they are too verbose
				|| fieldType.equals(JavaType.BOOLEAN_PRIMITIVE) || fieldType.equals(JavaType.BOOLEAN_OBJECT) // Exclude boolean values as they would not be meaningful in this presentation
				|| field.getCustomData().keySet().contains(PersistenceCustomDataKeys.EMBEDDED_FIELD) /* Not interested in embedded types */) {
			return false;
		}
		return true;
	}

	public boolean isApplicationType(JavaType javaType) {
		Assert.notNull(javaType, "Java type required");
		return metadataService.get(PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA)) != null;
	}

	public String getItdUniquenessFilenameSuffix() {
		return "ManagedBean";
	}

	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = JsfManagedBeanMetadata.getJavaType(metadataIdentificationString);
		Path path = JsfManagedBeanMetadata.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}

	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return JsfManagedBeanMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return JsfManagedBeanMetadata.getMetadataIdentiferType();
	}
}