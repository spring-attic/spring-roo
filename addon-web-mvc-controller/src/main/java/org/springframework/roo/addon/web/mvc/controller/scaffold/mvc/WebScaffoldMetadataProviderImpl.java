package org.springframework.roo.addon.web.mvc.controller.scaffold.mvc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.web.mvc.controller.RooConversionService;
import org.springframework.roo.addon.web.mvc.controller.RooWebScaffold;
import org.springframework.roo.addon.web.mvc.controller.converter.ConversionServiceOperations;
import org.springframework.roo.addon.web.mvc.controller.details.DateTimeFormatDetails;
import org.springframework.roo.addon.web.mvc.controller.details.JavaTypeMetadataDetails;
import org.springframework.roo.addon.web.mvc.controller.details.WebMetadataService;
import org.springframework.roo.addon.web.mvc.controller.scaffold.WebScaffoldAnnotationValues;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.util.Assert;

/**
 * Implementation of  {@link WebScaffoldMetadataProvider}.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
@Component(immediate = true) 
@Service 
public final class WebScaffoldMetadataProviderImpl extends AbstractMemberDiscoveringItdMetadataProvider implements WebScaffoldMetadataProvider {
	@Reference private TypeLocationService typeLocationService;
	@Reference private ConversionServiceOperations conversionServiceOperations;
	@Reference private WebMetadataService webMetadataService;
	private Map<JavaType, String> entityToWebScaffoldMidMap = new LinkedHashMap<JavaType, String>();
	private Map<String, JavaType> webScaffoldMidToEntityMap = new LinkedHashMap<String, JavaType>();

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.addNotificationListener(this);
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(new JavaType(RooWebScaffold.class.getName()));
	}
	
	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.removeNotificationListener(this);
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(new JavaType(RooWebScaffold.class.getName()));
	}
	
	protected String getLocalMidToRequest(ItdTypeDetails itdTypeDetails) {
		// Determine the governor for this ITD, and whether any metadata is even hoping to hear about changes to that JavaType and its ITDs
		JavaType governor = itdTypeDetails.getName();
		String localMid = entityToWebScaffoldMidMap.get(governor);
		if (localMid == null) {
			// Not interested in this JavaType, so let's move on
			return null;
		}
		
		// Because localMid is not null, we know that it is a 
		return localMid;
	}
	
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		// We need to parse the annotation, which we expect to be present
		WebScaffoldAnnotationValues annotationValues = new WebScaffoldAnnotationValues(governorPhysicalTypeMetadata);
		if (!annotationValues.isAnnotationFound() || annotationValues.getFormBackingObject() == null || governorPhysicalTypeMetadata.getMemberHoldingTypeDetails() == null) {
			return null;
		}
		
		// Remember that this entity JavaType matches up with this metadata identification string
		// Start by clearing the previous association
		JavaType oldEntity = webScaffoldMidToEntityMap.get(metadataIdentificationString);
		if (oldEntity != null) {
			entityToWebScaffoldMidMap.remove(oldEntity);
		}
		entityToWebScaffoldMidMap.put(annotationValues.getFormBackingObject(), metadataIdentificationString);
		webScaffoldMidToEntityMap.put(metadataIdentificationString, annotationValues.getFormBackingObject());
		
		// Lookup the form backing object's metadata
		JavaType formBackingType = annotationValues.getFormBackingObject();
		
		installConversionService(governorPhysicalTypeMetadata.getMemberHoldingTypeDetails().getName());
		
		PhysicalTypeMetadata formBackingObjectPhysicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(formBackingType, Path.SRC_MAIN_JAVA));
		Assert.notNull(formBackingObjectPhysicalTypeMetadata, "Unable to obtain physical type metadata for type " + formBackingType.getFullyQualifiedTypeName());
		MemberDetails formBackingObjectMemberDetails = getMemberDetails(formBackingObjectPhysicalTypeMetadata);
		
		MemberHoldingTypeDetails memberHoldingTypeDetails = MemberFindingUtils.getMostConcreteMemberHoldingTypeDetailsWithTag(formBackingObjectMemberDetails, PersistenceCustomDataKeys.PERSISTENT_TYPE);
		if (memberHoldingTypeDetails == null) {
			return null;
		}
		
		// We need to be informed if our dependent metadata changes
		metadataDependencyRegistry.registerDependency(memberHoldingTypeDetails.getDeclaredByMetadataId(), metadataIdentificationString);
		
		SortedMap<JavaType, JavaTypeMetadataDetails> relatedApplicationTypeMetadata = webMetadataService.getRelatedApplicationTypeMetadata(formBackingType, formBackingObjectMemberDetails, metadataIdentificationString);
		List<JavaTypeMetadataDetails> dependentApplicationTypeMetadata = webMetadataService.getDependentApplicationTypeMetadata(formBackingType, formBackingObjectMemberDetails, metadataIdentificationString);
		Map<JavaSymbolName, DateTimeFormatDetails> datePatterns = webMetadataService.getDatePatterns(formBackingType, formBackingObjectMemberDetails, metadataIdentificationString);

		MemberDetails memberDetails = getMemberDetails(governorPhysicalTypeMetadata);

		return new WebScaffoldMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, annotationValues, memberDetails, relatedApplicationTypeMetadata, dependentApplicationTypeMetadata, datePatterns);
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
					StringBuilder sb = new StringBuilder();
					sb.append("Found registerConverters=false in scaffolded controller ");
					sb.append(controller).append(". ");
					sb.append("Remove this property from all controllers and let Spring ROO install the new application-wide ApplicationConversionServiceFactoryBean. ");
					sb.append("Then move your custom getXxxConverter() methods to it, delete the GenericConversionService field and the @PostContruct method.");
					throw new IllegalStateException(sb.toString());
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
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}
	
	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return WebScaffoldMetadata.createIdentifier(javaType, path);
	}
	
	public String getProvidesType() {
		return WebScaffoldMetadata.getMetadataIdentiferType();
	}
}