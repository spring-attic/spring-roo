package org.springframework.roo.addon.web.mvc.controller.scaffold;

import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.PERSISTENT_TYPE;
import static org.springframework.roo.model.RooJavaType.ROO_WEB_SCAFFOLD;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.web.mvc.controller.details.DateTimeFormatDetails;
import org.springframework.roo.addon.web.mvc.controller.details.JavaTypeMetadataDetails;
import org.springframework.roo.addon.web.mvc.controller.details.WebMetadataService;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.layers.LayerCustomDataKeys;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;

/**
 * Implementation of {@link WebScaffoldMetadataProvider}.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
@Component(immediate = true) 
@Service 
public final class WebScaffoldMetadataProviderImpl extends AbstractMemberDiscoveringItdMetadataProvider implements WebScaffoldMetadataProvider {
	
	// Fields
	@Reference private WebMetadataService webMetadataService;
	@Reference private TypeLocationService typeLocationService;

	private final Map<JavaType, String> entityToWebScaffoldMidMap = new LinkedHashMap<JavaType, String>();
	private final Map<String, JavaType> webScaffoldMidToEntityMap = new LinkedHashMap<String, JavaType>();

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
		if (localMid != null) {
			return localMid;
		}

		MemberHoldingTypeDetails memberHoldingTypeDetails = typeLocationService.findClassOrInterface(itdTypeDetails.getGovernor().getName());
		if (memberHoldingTypeDetails != null && memberHoldingTypeDetails.getCustomData().get(LayerCustomDataKeys.LAYER_TYPE) != null) {
			@SuppressWarnings("unchecked")
			List<JavaType> domainTypes = (List<JavaType>) memberHoldingTypeDetails.getCustomData().get(LayerCustomDataKeys.LAYER_TYPE);
			if (domainTypes != null) {
				for (JavaType type : domainTypes) {
					String localMidType = entityToWebScaffoldMidMap.get(type);
					if (localMidType != null) {
						return localMidType;
					}
				}
			}
		}
		return null;
	}
	
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(final String metadataIdentificationString, final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {
		// We need to parse the annotation, which we expect to be present
		final WebScaffoldAnnotationValues annotationValues = new WebScaffoldAnnotationValues(governorPhysicalTypeMetadata);
		final JavaType formBackingType = annotationValues.getFormBackingObject();
		if (!annotationValues.isAnnotationFound() || formBackingType == null) {
			return null;
		}

		final MemberDetails formBackingObjectMemberDetails = getMemberDetails(formBackingType);
		if (formBackingObjectMemberDetails == null) {
			return null;
		}

		final MemberHoldingTypeDetails formBackingMemberHoldingTypeDetails = MemberFindingUtils.getMostConcreteMemberHoldingTypeDetailsWithTag(formBackingObjectMemberDetails, PERSISTENT_TYPE);
		if (formBackingMemberHoldingTypeDetails == null) {
			return null;
		}

		// We need to be informed if our dependent metadata changes
		metadataDependencyRegistry.registerDependency(formBackingMemberHoldingTypeDetails.getDeclaredByMetadataId(), metadataIdentificationString);

		// Remember that this entity JavaType matches up with this metadata identification string
		// Start by clearing the previous association
		final JavaType oldEntity = webScaffoldMidToEntityMap.get(metadataIdentificationString);
		if (oldEntity != null) {
			entityToWebScaffoldMidMap.remove(oldEntity);
		}
		entityToWebScaffoldMidMap.put(formBackingType, metadataIdentificationString);
		webScaffoldMidToEntityMap.put(metadataIdentificationString, formBackingType);

		final SortedMap<JavaType, JavaTypeMetadataDetails> relatedApplicationTypeMetadata = webMetadataService.getRelatedApplicationTypeMetadata(formBackingType, formBackingObjectMemberDetails, metadataIdentificationString);
		final List<JavaTypeMetadataDetails> dependentApplicationTypeMetadata = webMetadataService.getDependentApplicationTypeMetadata(formBackingType, formBackingObjectMemberDetails, metadataIdentificationString);
		final Map<JavaSymbolName, DateTimeFormatDetails> datePatterns = webMetadataService.getDatePatterns(formBackingType, formBackingObjectMemberDetails, metadataIdentificationString);
		final Map<String, MemberTypeAdditions> crudAdditions = webMetadataService.getCrudAdditions(formBackingType, metadataIdentificationString);
		
		return new WebScaffoldMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, annotationValues, relatedApplicationTypeMetadata, dependentApplicationTypeMetadata, datePatterns, crudAdditions);
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