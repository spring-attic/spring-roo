package org.springframework.roo.addon.jsf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Implementation of {@link JsfManagedBeanFormMetadataProvider}.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
@Component(immediate = true) 
@Service 
public final class JsfManagedBeanFormMetadataProviderImpl extends AbstractMemberDiscoveringItdMetadataProvider implements JsfManagedBeanFormMetadataProvider {
	private Map<JavaType, String> entityToManagedBeandMidMap = new LinkedHashMap<JavaType, String>();
	private Map<String, JavaType> managedBeanMidToEntityMap = new LinkedHashMap<String, JavaType>();

	protected void activate(ComponentContext context) {
	//	metadataDependencyRegistry.addNotificationListener(this);
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(new JavaType(RooJsfManagedBean.class.getName()));
	}

	protected void deactivate(ComponentContext context) {
	//	metadataDependencyRegistry.removeNotificationListener(this);
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(new JavaType(RooJsfManagedBean.class.getName()));
	}

	protected String getLocalMidToRequest(ItdTypeDetails itdTypeDetails) {
		// Determine the governor for this ITD, and whether any metadata is even hoping to hear about changes to that JavaType and its ITDs
		String localMid = entityToManagedBeandMidMap.get(itdTypeDetails.getName());
		return localMid == null ? null : localMid;
	}

	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		// We need to parse the annotation, which we expect to be present
		JsfAnnotationValues annotationValues = new JsfAnnotationValues(governorPhysicalTypeMetadata);
		JavaType entityType = annotationValues.getEntity();
		if (!annotationValues.isAnnotationFound() || entityType == null) {
			return null;
		}

		// Lookup the entity's metadata
		MemberDetails memberDetails = getMemberDetails(entityType);
		if (memberDetails == null) {
			return null;
		}

		MemberHoldingTypeDetails persistenceMemberHoldingTypeDetails = MemberFindingUtils.getMostConcreteMemberHoldingTypeDetailsWithTag(memberDetails, PersistenceCustomDataKeys.PERSISTENT_TYPE);
		if (persistenceMemberHoldingTypeDetails == null) {
			return null;
		}

		// We need to be informed if our dependent metadata changes
		metadataDependencyRegistry.registerDependency(persistenceMemberHoldingTypeDetails.getDeclaredByMetadataId(), metadataIdentificationString);

		// Remember that this entity JavaType matches up with this metadata identification string
		// Start by clearing the previous association
		JavaType oldEntity = managedBeanMidToEntityMap.get(metadataIdentificationString);
		if (oldEntity != null) {
			entityToManagedBeandMidMap.remove(oldEntity);
		}
		entityToManagedBeandMidMap.put(entityType, metadataIdentificationString);
		managedBeanMidToEntityMap.put(metadataIdentificationString, entityType);

		PluralMetadata pluralMetadata = (PluralMetadata) metadataService.get(PluralMetadata.createIdentifier(entityType, Path.SRC_MAIN_JAVA));
		Assert.notNull(pluralMetadata, "Could not determine plural for '" + entityType.getSimpleTypeName() + "'");
		String plural = pluralMetadata.getPlural();

		Map<FieldMetadata, MethodMetadata> locatedFieldsAndMutators = findFieldsAndMutators(memberDetails, metadataIdentificationString);

		return new JsfManagedBeanFormMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, annotationValues, memberDetails, plural, locatedFieldsAndMutators);
	}

	private Map<FieldMetadata, MethodMetadata> findFieldsAndMutators(MemberDetails memberDetails, String metadataIdentificationString) {
		Map<FieldMetadata, MethodMetadata> locatedFieldsAndMutators = new LinkedHashMap<FieldMetadata,MethodMetadata>();
		
		for (FieldMetadata field : MemberFindingUtils.getFields(memberDetails)) {
			metadataDependencyRegistry.registerDependency(field.getDeclaredByMetadataId(), metadataIdentificationString);
			String capitalizedFieldName = StringUtils.capitalize(field.getFieldName().getSymbolName());
			String methodPrefix = field.getFieldType().equals(JavaType.BOOLEAN_PRIMITIVE) ? "is" : "get";
			MethodMetadata accessor = MemberFindingUtils.getMethod(memberDetails, new JavaSymbolName(methodPrefix + capitalizedFieldName), new ArrayList<JavaType>());
			if (accessor == null) {
				continue;
			}
			if (accessor.getCustomData().keySet().contains(PersistenceCustomDataKeys.IDENTIFIER_ACCESSOR_METHOD) || accessor.getCustomData().keySet().contains(PersistenceCustomDataKeys.VERSION_ACCESSOR_METHOD)) {
				continue; 
			}
			MethodMetadata mutator = MemberFindingUtils.getMethod(memberDetails, new JavaSymbolName("set" + capitalizedFieldName), Arrays.asList(field.getFieldType()));
			if (mutator == null) {
				continue;
			}
			locatedFieldsAndMutators.put(field, mutator);
		}
		return locatedFieldsAndMutators;
//		for (MethodMetadata method : MemberFindingUtils.getMethods(memberDetails)) {
//			// Track any changes to the method (eg it goes away)
//			metadataDependencyRegistry.registerDependency(method.getDeclaredByMetadataId(), metadataIdentificationString);
//			if (!(BeanInfoUtils.isMutatorMethod(method) || BeanInfoUtils.isAccessorMethod(method))) {
//				continue; 
//			}
//			if (method.getCustomData().keySet().contains(PersistenceCustomDataKeys.IDENTIFIER_ACCESSOR_METHOD) || method.getCustomData().keySet().contains(PersistenceCustomDataKeys.VERSION_ACCESSOR_METHOD)) {
//				return false; 
//			}
//			FieldMetadata field = BeanInfoUtils.getFieldForPropertyName(memberDetails, BeanInfoUtils.getPropertyNameForJavaBeanMethod(method));
//			if (field == null) {
//				return false;
//			}
//
//			if (isMethodOfInterest(method, memberDetails)) {
//				locatedMutators.add(method);
//			}
//		}
//		return locatedMutators;
	}

	private boolean isMethodOfInterest(MethodMetadata method, MemberDetails memberDetails) {
		if (!(BeanInfoUtils.isMutatorMethod(method) || BeanInfoUtils.isAccessorMethod(method))) {
			return false; 
		}
		if (method.getCustomData().keySet().contains(PersistenceCustomDataKeys.IDENTIFIER_ACCESSOR_METHOD) || method.getCustomData().keySet().contains(PersistenceCustomDataKeys.VERSION_ACCESSOR_METHOD)) {
			return false; 
		}
		FieldMetadata field = BeanInfoUtils.getFieldForPropertyName(memberDetails, BeanInfoUtils.getPropertyNameForJavaBeanMethod(method));
		if (field == null) {
			return false;
		}
		return true;
	}

	public boolean isApplicationType(JavaType javaType) {
		Assert.notNull(javaType, "Java type required");
		return metadataService.get(PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA)) != null;
	}

	public String getItdUniquenessFilenameSuffix() {
		return "ManagedBean_Form";
	}

	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = JsfManagedBeanFormMetadata.getJavaType(metadataIdentificationString);
		Path path = JsfManagedBeanFormMetadata.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}

	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return JsfManagedBeanFormMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return JsfManagedBeanFormMetadata.getMetadataIdentiferType();
	}
}