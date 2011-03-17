package org.springframework.roo.addon.web.mvc.controller.converter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.addon.web.mvc.controller.RooConversionService;
import org.springframework.roo.addon.web.mvc.controller.RooWebScaffold;
import org.springframework.roo.addon.web.mvc.controller.details.WebMetadataUtils;
import org.springframework.roo.addon.web.mvc.controller.scaffold.WebScaffoldAnnotationValues;
import org.springframework.roo.addon.web.mvc.controller.scaffold.WebScaffoldMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
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
 * Metadata provider for {@link ConversionServiceMetadata}. Monitors
 * notifications for {@link RooConversionService} and {@link RooWebScaffold}
 * annotated types. Also listens for changes to the scaffolded domain types and
 * their associated domain types.
 * 
 * @author Rossen Stoyanchev
 * @author Stefan Schmidt
 * @since 1.1.1
 */
@Component(immediate = true)
@Service
public final class ConversionServiceMetadataProvider extends AbstractItdMetadataProvider {
	@Reference private TypeLocationService typeLocationService;
	
	// Stores the MID (as accepted by this ConversionServiceMetadataProvider) for the one (and only one) application-wide conversion service
	private String applicationConversionServiceFactoryBeanMid;
	
	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		metadataDependencyRegistry.registerDependency(WebScaffoldMetadata.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(new JavaType(RooConversionService.class.getName()));
	}

	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		metadataDependencyRegistry.deregisterDependency(WebScaffoldMetadata.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(new JavaType(RooConversionService.class.getName()));
	}

	@Override
	protected String resolveDownstreamDependencyIdentifier(String upstreamDependency) {
		String publishingProvider = MetadataIdentificationUtils.getMetadataClass(upstreamDependency);
		if (publishingProvider.equals(MetadataIdentificationUtils.getMetadataClass(WebScaffoldMetadata.getMetadataIdentiferType()))) {
			// A WebScaffoldMetadata upstream MID has changed or become available for the first time
			// It's OK to return null if we don't yet know the MID because its JavaType has never been found
			return applicationConversionServiceFactoryBeanMid;
		}
		// It wasn't a WebScaffoldMetadata, so we can let the superclass handle it
		// (it's expected it would be a PhysicalTypeIdentifier notification, as that's the only other thing we registered to receive)
		return super.resolveDownstreamDependencyIdentifier(upstreamDependency);
	}

	@Override
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		applicationConversionServiceFactoryBeanMid = metadataIdentificationString;

		// To get here we know the governor is the ApplicationConversionServiceFactoryBean so let's go ahead and create its ITD
		Map<JavaType, List<MethodMetadata>> relevantDomainTypes = findDomainTypesRequiringAConverter(metadataIdentificationString);
		if (relevantDomainTypes.isEmpty()) { 
			// No ITD needed
			return null;
		}
		
		return new ConversionServiceMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, relevantDomainTypes);
	}
	
	protected Map<JavaType, List<MethodMetadata>> findDomainTypesRequiringAConverter(String metadataIdentificationString) {
		JavaType rooWebScaffold = new JavaType(RooWebScaffold.class.getName());
		Map<JavaType, List<MethodMetadata>> relevantDomainTypes = new HashMap<JavaType, List<MethodMetadata>>();
		for (JavaType controller : typeLocationService.findTypesWithAnnotation(rooWebScaffold)) {
			PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(controller, Path.SRC_MAIN_JAVA));
			Assert.notNull(physicalTypeMetadata, "Unable to obtain physical type metdata for type " + controller.getFullyQualifiedTypeName());
			WebScaffoldAnnotationValues webScaffoldAnnotationValues = new WebScaffoldAnnotationValues(physicalTypeMetadata);
			relevantDomainTypes.putAll(findRelevantTypes(webScaffoldAnnotationValues.getFormBackingObject(), metadataIdentificationString));
		}
		return relevantDomainTypes;
	}
	
	private Map<JavaType, List<MethodMetadata>> findRelevantTypes(JavaType type, String metadataIdentificationString) {
		PhysicalTypeMetadata formBackingObjectPhysicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(type, Path.SRC_MAIN_JAVA));
		Assert.notNull(formBackingObjectPhysicalTypeMetadata, "Unable to obtain physical type metdata for type " + type.getFullyQualifiedTypeName());
		MemberDetails memberDetails = memberDetailsScanner.getMemberDetails(getClass().getName(), (ClassOrInterfaceTypeDetails) formBackingObjectPhysicalTypeMetadata.getMemberHoldingTypeDetails());
	
		Map<JavaType, List<MethodMetadata>> types = new HashMap<JavaType, List<MethodMetadata>>();
		List<MethodMetadata> locatedAccessors = new ArrayList<MethodMetadata>();
		
		String entityMid = EntityMetadata.createIdentifier(type, Path.SRC_MAIN_JAVA);
		EntityMetadata entityMetadata = (EntityMetadata) metadataService.get(entityMid);
		metadataDependencyRegistry.registerDependency(entityMid, metadataIdentificationString);
		int counter = 0;
		for (MethodMetadata method : MemberFindingUtils.getMethods(memberDetails)) {
			if (counter < 4 && isMethodOfInterest(entityMetadata, method, memberDetails)) {
				counter++;
				locatedAccessors.add(method);
				// Track any changes to that method (eg it goes away)
				metadataDependencyRegistry.registerDependency(method.getDeclaredByMetadataId(), metadataIdentificationString);
			} 
			
			if (BeanInfoUtils.isAccessorMethod(method) && WebMetadataUtils.isApplicationType(method.getReturnType(), metadataService)) {
				// Track any related java types in the project
				metadataDependencyRegistry.registerDependency(method.getDeclaredByMetadataId(), metadataIdentificationString);
			}
		}
		
		if (locatedAccessors.size() > 0) {
			types.put(type, locatedAccessors);
		}
		return types;
	}
	
	private boolean isMethodOfInterest(EntityMetadata entityMetadata, MethodMetadata method, MemberDetails memberDetails) {
		if (! BeanInfoUtils.isAccessorMethod(method)) {
			return false; // Only interested in accessors
		}
		if (entityMetadata != null && (method.getMethodName().equals(entityMetadata.getIdentifierAccessor().getMethodName()) || (entityMetadata.getVersionAccessor() != null && method.getMethodName().equals(entityMetadata.getVersionAccessor().getMethodName())))) {
			return false; // Only interested in methods which are not accessors for persistence version or id fields
		}
		FieldMetadata field = BeanInfoUtils.getFieldForPropertyName(memberDetails, BeanInfoUtils.getPropertyNameForJavaBeanMethod(method));
		if (field == null) {
			return false;
		}
		JavaType fieldType = field.getFieldType();
		if (fieldType.isCommonCollectionType() || fieldType.isArray() // Exclude collections and arrays
				|| WebMetadataUtils.isApplicationType(fieldType, metadataService) // Exclude references to other domain objects as they are too verbose
				|| fieldType.equals(JavaType.BOOLEAN_PRIMITIVE) || fieldType.equals(JavaType.BOOLEAN_OBJECT) // Exclude boolean values as they would not be meaningful in this presentation
				|| WebMetadataUtils.isEmbeddedFieldType(field) /* Not interested in embedded types */) {
			return false;
		}
		return true;
	}

	public String getItdUniquenessFilenameSuffix() {
		return "ConversionService";
	}

	public String getProvidesType() {
		return MetadataIdentificationUtils.create(ConversionServiceMetadata.class.getName());
	}

	@Override
	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(ConversionServiceMetadata.class.getName(), javaType, path);
	}

	@Override
	protected String getGovernorPhysicalTypeIdentifier(String metadataId) {
		JavaType javaType = PhysicalTypeIdentifierNamingUtils.getJavaType(ConversionServiceMetadata.class.getName(), metadataId);
		Path path = PhysicalTypeIdentifierNamingUtils.getPath(ConversionServiceMetadata.class.getName(), metadataId);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}
}