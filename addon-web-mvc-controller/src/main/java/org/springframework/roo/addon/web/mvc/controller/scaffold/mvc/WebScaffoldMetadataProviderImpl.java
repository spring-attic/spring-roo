package org.springframework.roo.addon.web.mvc.controller.scaffold.mvc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.web.mvc.controller.RooWebScaffold;
import org.springframework.roo.addon.web.mvc.controller.details.DateTimeFormatDetails;
import org.springframework.roo.addon.web.mvc.controller.details.JavaTypeMetadataDetails;
import org.springframework.roo.addon.web.mvc.controller.details.WebMetadataService;
import org.springframework.roo.addon.web.mvc.controller.scaffold.WebScaffoldAnnotationValues;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.util.Assert;

/**
 * Implementation of {@link WebScaffoldMetadataProvider}.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
@Component(immediate = true) 
@Service 
public final class WebScaffoldMetadataProviderImpl extends AbstractMemberDiscoveringItdMetadataProvider implements WebScaffoldMetadataProvider {
	private static final JavaType ROO_WEB_SCAFFOLD = new JavaType(RooWebScaffold.class.getName());
	@Reference private WebMetadataService webMetadataService;
	private Map<JavaType, String> entityToWebScaffoldMidMap = new LinkedHashMap<JavaType, String>();
	private Map<String, JavaType> webScaffoldMidToEntityMap = new LinkedHashMap<String, JavaType>();

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.addNotificationListener(this);
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(ROO_WEB_SCAFFOLD);
	}
	
	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.removeNotificationListener(this);
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(ROO_WEB_SCAFFOLD);
	}
	
	protected String getLocalMidToRequest(ItdTypeDetails itdTypeDetails) {
		// Determine the governor for this ITD, and whether any metadata is even hoping to hear about changes to that JavaType and its ITDs
		JavaType governor = itdTypeDetails.getName();
		String localMid = entityToWebScaffoldMidMap.get(governor);
		return localMid == null ? null : localMid;
	}
	
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		// We need to parse the annotation, which we expect to be present
		WebScaffoldAnnotationValues annotationValues = new WebScaffoldAnnotationValues(governorPhysicalTypeMetadata);
		if (!annotationValues.isAnnotationFound() || annotationValues.getFormBackingObject() == null || governorPhysicalTypeMetadata.getMemberHoldingTypeDetails() == null) {
			return null;
		}
		
		// Lookup the form backing object's metadata
		JavaType formBackingType = annotationValues.getFormBackingObject();
		
		PhysicalTypeMetadata formBackingObjectPhysicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(formBackingType, Path.SRC_MAIN_JAVA));
		Assert.notNull(formBackingObjectPhysicalTypeMetadata, "Unable to obtain physical type metadata for type " + formBackingType.getFullyQualifiedTypeName());
		MemberDetails formBackingObjectMemberDetails = getMemberDetails(formBackingObjectPhysicalTypeMetadata);
		
		MemberHoldingTypeDetails formBackingMemberHoldingTypeDetails = MemberFindingUtils.getMostConcreteMemberHoldingTypeDetailsWithTag(formBackingObjectMemberDetails, PersistenceCustomDataKeys.PERSISTENT_TYPE);
		if (formBackingMemberHoldingTypeDetails == null) {
			return null;
		}

		// We need to be informed if our dependent metadata changes
		metadataDependencyRegistry.registerDependency(formBackingMemberHoldingTypeDetails.getDeclaredByMetadataId(), metadataIdentificationString);

		// Remember that this entity JavaType matches up with this metadata identification string
		// Start by clearing the previous association
		JavaType oldEntity = webScaffoldMidToEntityMap.get(metadataIdentificationString);
		if (oldEntity != null) {
			entityToWebScaffoldMidMap.remove(oldEntity);
		}
		entityToWebScaffoldMidMap.put(formBackingType, metadataIdentificationString);
		webScaffoldMidToEntityMap.put(metadataIdentificationString, formBackingType);

		SortedMap<JavaType, JavaTypeMetadataDetails> relatedApplicationTypeMetadata = webMetadataService.getRelatedApplicationTypeMetadata(formBackingType, formBackingObjectMemberDetails, metadataIdentificationString);
		List<JavaTypeMetadataDetails> dependentApplicationTypeMetadata = webMetadataService.getDependentApplicationTypeMetadata(formBackingType, formBackingObjectMemberDetails, metadataIdentificationString);
		Map<JavaSymbolName, DateTimeFormatDetails> datePatterns = webMetadataService.getDatePatterns(formBackingType, formBackingObjectMemberDetails, metadataIdentificationString);

		MemberDetails memberDetails = getMemberDetails(governorPhysicalTypeMetadata);

		return new WebScaffoldMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, annotationValues, memberDetails, relatedApplicationTypeMetadata, dependentApplicationTypeMetadata, datePatterns);
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