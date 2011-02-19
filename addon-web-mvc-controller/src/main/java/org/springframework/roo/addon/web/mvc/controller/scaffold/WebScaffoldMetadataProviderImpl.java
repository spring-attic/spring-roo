package org.springframework.roo.addon.web.mvc.controller.scaffold;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.addon.json.JsonMetadata;
import org.springframework.roo.addon.web.mvc.controller.RooConversionService;
import org.springframework.roo.addon.web.mvc.controller.RooWebScaffold;
import org.springframework.roo.addon.web.mvc.controller.converter.ConversionServiceOperations;
import org.springframework.roo.addon.web.mvc.controller.details.WebMetadataUtils;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.util.Assert;

/**
 * Provides {@link WebScaffoldMetadata}.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
@Component(immediate = true) 
@Service 
public final class WebScaffoldMetadataProviderImpl extends AbstractItdMetadataProvider implements WebScaffoldMetadataProvider {
	@Reference private TypeLocationService typeLocationService;
	@Reference private ConversionServiceOperations conversionServiceOperations;

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(new JavaType(RooWebScaffold.class.getName()));
	}
	
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		// We need to parse the annotation, which we expect to be present
		WebScaffoldAnnotationValues annotationValues = new WebScaffoldAnnotationValues(governorPhysicalTypeMetadata);
		if (!annotationValues.isAnnotationFound() || annotationValues.formBackingObject == null || governorPhysicalTypeMetadata.getMemberHoldingTypeDetails() == null) {
			return null;
		}
		
		// Lookup the form backing object's metadata
		JavaType formBackingType = annotationValues.formBackingObject;
		Path path = Path.SRC_MAIN_JAVA;
		String entityMetadataKey = EntityMetadata.createIdentifier(formBackingType, path);
		
		// We need to lookup the metadata we depend on
		EntityMetadata entityMetadata = (EntityMetadata) metadataService.get(entityMetadataKey);
		
		// We need to abort if we couldn't find dependent metadata
		if (entityMetadata == null || !entityMetadata.isValid()) {
			return null;
		}
		
		// We need to be informed if our dependent metadata changes
		metadataDependencyRegistry.registerDependency(entityMetadataKey, metadataIdentificationString);
		
		installConversionService(governorPhysicalTypeMetadata.getMemberHoldingTypeDetails().getName());
		
		ClassOrInterfaceTypeDetails controllerClassOrInterfaceDetails = (ClassOrInterfaceTypeDetails) governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();
		MemberDetails controllerMemberDetails = memberDetailsScanner.getMemberDetails(getClass().getName(), controllerClassOrInterfaceDetails);
		
		PhysicalTypeMetadata formBackingObjectPhysicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(formBackingType, path));
		Assert.notNull(formBackingObjectPhysicalTypeMetadata, "Unable to obtain physical type metdata for type " + formBackingType.getFullyQualifiedTypeName());
		ClassOrInterfaceTypeDetails formbackingClassOrInterfaceDetails = (ClassOrInterfaceTypeDetails) formBackingObjectPhysicalTypeMetadata.getMemberHoldingTypeDetails();
		MemberDetails formBackingObjectMemberDetails = memberDetailsScanner.getMemberDetails(getClass().getName(), formbackingClassOrInterfaceDetails);
		
		JsonMetadata jsonMetadata = null;
		
		if (annotationValues.isExposeJson()) {
			jsonMetadata = (JsonMetadata) metadataService.get(JsonMetadata.createIdentifier(formBackingType, path));
		}
		
		
		// We do not need to monitor the parent, as any changes to the java type associated with the parent will trickle down to the governing java type
		return new WebScaffoldMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, annotationValues, 
				MemberFindingUtils.getMethods(controllerMemberDetails), 
				WebMetadataUtils.getRelatedApplicationTypeMetadata(formBackingType, formBackingObjectMemberDetails, metadataService, typeLocationService, metadataIdentificationString, metadataDependencyRegistry), 
				WebMetadataUtils.getDependentApplicationTypeMetadata(formBackingType, formBackingObjectMemberDetails, metadataService, typeLocationService, metadataIdentificationString, metadataDependencyRegistry), 
				WebMetadataUtils.getDatePatterns(formBackingType, formBackingObjectMemberDetails, metadataService, metadataIdentificationString, metadataDependencyRegistry), 
				WebMetadataUtils.getDynamicFinderMethodsAndFields(formBackingType, formBackingObjectMemberDetails, metadataService, metadataIdentificationString, metadataDependencyRegistry),
				jsonMetadata);
	}
	
	void installConversionService(JavaType governor) {
		JavaType rooConversionService = new JavaType(RooConversionService.class.getName());
		if (typeLocationService.findTypesWithAnnotation(rooConversionService).size() > 0) {
			return;
		}
		JavaType rooWebScaffold = new JavaType(RooWebScaffold.class.getName());
		for (ClassOrInterfaceTypeDetails controller : typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(rooWebScaffold)) {
			AnnotationMetadata annotation = MemberFindingUtils.getTypeAnnotation(controller, rooWebScaffold);
			AnnotationAttributeValue<?> attr = annotation.getAttribute(new JavaSymbolName("registerConverters"));
			if (attr != null) {
				if (Boolean.FALSE.equals(attr.getValue())) {
					throw new IllegalStateException("Found registerConverters=false in scaffolded controller " + controller + ". " +
							"Remove this property from all controllers and let Spring ROO install the new application-wide ApplicationConversionServiceFactoryBean. " +
							"Then move your custom getXxxConverter() methods to it, delete the GenericConversionService field and the @PostContruct method.");
				}
			}
		}
		conversionServiceOperations.installConversionService(governor.getPackage());
	}
	
	public String getItdUniquenessFilenameSuffix() {
		return "Controller";
	}
	
	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = WebScaffoldMetadata.getJavaType(metadataIdentificationString);
		Path path = WebScaffoldMetadata.getPath(metadataIdentificationString);
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(javaType, path);
		return physicalTypeIdentifier;
	}
	
	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return WebScaffoldMetadata.createIdentifier(javaType, path);
	}
	
	public String getProvidesType() {
		return WebScaffoldMetadata.getMetadataIdentiferType();
	}
}