package org.springframework.roo.addon.jsf;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.util.Assert;

/**
 * Provides {@link JsfMenuBeanMetadata}.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
@Component(immediate = true) 
@Service 
public final class JsfMenuBeanMetadataProviderImpl extends AbstractItdMetadataProvider implements JsfMenuBeanMetadataProvider {
	@Reference private TypeLocationService typeLocationService;
	private String menuBeanMid;

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		metadataDependencyRegistry.registerDependency(JsfManagedBeanMetadata.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(new JavaType(RooJsfMenuBean.class.getName()));
	}

	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		metadataDependencyRegistry.deregisterDependency(JsfManagedBeanMetadata.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(new JavaType(RooJsfMenuBean.class.getName()));
	}
	
	@Override
	protected String resolveDownstreamDependencyIdentifier(String upstreamDependency) {
		String publishingProvider = MetadataIdentificationUtils.getMetadataClass(upstreamDependency);
		if (publishingProvider.equals(MetadataIdentificationUtils.getMetadataClass(JsfManagedBeanMetadata.getMetadataIdentiferType()))) {
			// A JsfManagedBeanMetadata upstream MID has changed or become available for the first time
			// It's OK to return null if we don't yet know the MID because its JavaType has never been found
			return menuBeanMid;
		}
		// It wasn't a JsfManagedBeanMetadata, so we can let the superclass handle it
		// (it's expected it would be a PhysicalTypeIdentifier notification, as that's the only other thing we registered to receive)
		return super.resolveDownstreamDependencyIdentifier(upstreamDependency);
	}

	@Override
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		menuBeanMid = metadataIdentificationString;
		
		// To get here we know the governor is the MenuBean so let's go ahead and create its ITD
		
		Set<JavaType> managedBeans = typeLocationService.findTypesWithAnnotation(new JavaType(RooJsfManagedBean.class.getName()));
	//	Map<JavaType, List<MethodMetadata>> relevantDomainTypes = findDomainTypesRequiringAConverter(metadataIdentificationString, controllers);
	//	Map<JavaType, Map<Object, JavaSymbolName>> compositePrimaryKeyTypes = findCompositePrimaryKeyTypesRequiringAConverter(metadataIdentificationString, controllers);
	//	if (relevantDomainTypes.isEmpty() && compositePrimaryKeyTypes.isEmpty()) { 
	//		// No ITD needed
	//		return null;
	//	}
		return new JsfMenuBeanMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata);
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
		return "MenuBean";
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