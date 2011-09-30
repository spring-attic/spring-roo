package org.springframework.roo.addon.web.mvc.controller.json;

import static org.springframework.roo.model.RooJavaType.ROO_WEB_JSON;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.json.JsonMetadata;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.addon.web.mvc.controller.details.FinderMetadataDetails;
import org.springframework.roo.addon.web.mvc.controller.details.JavaTypePersistenceMetadataDetails;
import org.springframework.roo.addon.web.mvc.controller.details.WebMetadataService;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.customdata.CustomDataKeys;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Implementation of {@link WebJsonMetadataProvider}.
 * 
 * @author Stefan Schmidt
 * @since 1.1.3
 */
@Component(immediate = true) 
@Service 
public final class WebJsonMetadataProviderImpl extends AbstractItdMetadataProvider implements WebJsonMetadataProvider {

	// Fields
	@Reference private WebMetadataService webMetadataService;
	@Reference private TypeLocationService typeLocationService;

	private final Map<JavaType, String> managedEntityTypes = new HashMap<JavaType, String>();

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.addNotificationListener(this);
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(ROO_WEB_JSON);
	}

	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.removeNotificationListener(this);
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(ROO_WEB_JSON);
	}
	
	// We need to notified when ProjectMetadata changes in order to handle JPA <-> GAE persistence changes
	@Override
	protected void notifyForGenericListener(String upstreamDependency) {
		// If the upstream dependency is null or invalid do not continue
		if (!StringUtils.hasText(upstreamDependency) || !MetadataIdentificationUtils.isValid(upstreamDependency)) {
			return;
		}
		
		// We do need to be informed if a new layer is available to see if we should use that
		if (PhysicalTypeIdentifier.isValid(upstreamDependency)) {
			MemberHoldingTypeDetails memberHoldingTypeDetails = typeLocationService.findClassOrInterface(PhysicalTypeIdentifier.getJavaType(upstreamDependency));
			if (memberHoldingTypeDetails != null && memberHoldingTypeDetails.getCustomData().get(CustomDataKeys.LAYER_TYPE) != null) {
				@SuppressWarnings("unchecked")
				List<JavaType> domainTypes = (List<JavaType>) memberHoldingTypeDetails.getCustomData().get(CustomDataKeys.LAYER_TYPE);
				if (domainTypes != null) {
					for (JavaType type : domainTypes) {
						String localMidType = managedEntityTypes.get(type);
						if (localMidType != null) {
							metadataService.get(localMidType);
						}
					}
				}
			}
		}
	}

	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		// We need to parse the annotation, which we expect to be present
		WebJsonAnnotationValues annotationValues = new WebJsonAnnotationValues(governorPhysicalTypeMetadata);
		if (!annotationValues.isAnnotationFound() || annotationValues.getJsonObject() == null || governorPhysicalTypeMetadata.getMemberHoldingTypeDetails() == null) {
			return null;
		}
		
		// Lookup the form backing object's metadata
		JavaType jsonObject = annotationValues.getJsonObject();
		JsonMetadata jsonMetadata = (JsonMetadata) metadataService.get(JsonMetadata.createIdentifier(jsonObject, Path.SRC_MAIN_JAVA));
		if (jsonMetadata == null) {
			return null;
		}
		
		PhysicalTypeMetadata backingObjectPhysicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(jsonObject, Path.SRC_MAIN_JAVA));
		Assert.notNull(backingObjectPhysicalTypeMetadata, "Unable to obtain physical type metadata for type " + jsonObject.getFullyQualifiedTypeName());
		MemberDetails formBackingObjectMemberDetails = getMemberDetails(backingObjectPhysicalTypeMetadata);
		
		MemberHoldingTypeDetails backingMemberHoldingTypeDetails = MemberFindingUtils.getMostConcreteMemberHoldingTypeDetailsWithTag(formBackingObjectMemberDetails, CustomDataKeys.PERSISTENT_TYPE);
		if (backingMemberHoldingTypeDetails == null) {
			return null;
		}
		
		// We need to be informed if our dependent metadata changes
		metadataDependencyRegistry.registerDependency(backingMemberHoldingTypeDetails.getDeclaredByMetadataId(), metadataIdentificationString);
		
		Set<FinderMetadataDetails> finderDetails = webMetadataService.getDynamicFinderMethodsAndFields(jsonObject, formBackingObjectMemberDetails, metadataIdentificationString);
		
		final Map<String, MemberTypeAdditions> persistenceAdditions = webMetadataService.getCrudAdditions(jsonObject, metadataIdentificationString);
		JavaTypePersistenceMetadataDetails javaTypePersistenceMetadataDetails = webMetadataService.getJavaTypePersistenceMetadataDetails(jsonObject, getMemberDetails(jsonObject), metadataIdentificationString);
		PluralMetadata pluralMetadata = (PluralMetadata) metadataService.get(PluralMetadata.createIdentifier(jsonObject));
		if (persistenceAdditions.isEmpty() || javaTypePersistenceMetadataDetails == null || pluralMetadata == null) {
			return null;
		}
		
		// Temporary workaround until AspectJ allows multiple introductions of the same field to a target type.
		boolean servicesInjected = MemberFindingUtils.getAnnotationOfType(governorPhysicalTypeMetadata.getMemberHoldingTypeDetails().getAnnotations(), RooJavaType.ROO_WEB_SCAFFOLD) != null;
		
		// Maintain a list of entities that are being tested
		managedEntityTypes.put(jsonObject, metadataIdentificationString);
		
		return new WebJsonMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, annotationValues, persistenceAdditions, javaTypePersistenceMetadataDetails.getIdentifierField(), pluralMetadata.getPlural(), finderDetails, jsonMetadata, servicesInjected);
	}
	
	public String getItdUniquenessFilenameSuffix() {
		return "Controller_Json";
	}
	
	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = WebJsonMetadata.getJavaType(metadataIdentificationString);
		Path path = WebJsonMetadata.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}
	
	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return WebJsonMetadata.createIdentifier(javaType, path);
	}
	
	public String getProvidesType() {
		return WebJsonMetadata.getMetadataIdentiferType();
	}
}