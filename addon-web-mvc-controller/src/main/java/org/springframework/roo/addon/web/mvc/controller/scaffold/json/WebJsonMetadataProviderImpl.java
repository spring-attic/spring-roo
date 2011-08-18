package org.springframework.roo.addon.web.mvc.controller.scaffold.json;

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
import org.springframework.roo.addon.web.mvc.controller.scaffold.WebScaffoldAnnotationValues;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.layers.LayerCustomDataKeys;
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
	
	// Constants
	private static final JavaType TRIGGER_ANNOTATION = RooJavaType.ROO_WEB_SCAFFOLD;
	
	// Fields
	@Reference private WebMetadataService webMetadataService;

	private final Map<JavaType, String> managedEntityTypes = new HashMap<JavaType, String>();

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.addNotificationListener(this);
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(TRIGGER_ANNOTATION);
	}

	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.removeNotificationListener(this);
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(TRIGGER_ANNOTATION);
	}
	
	// We need to notified when ProjectMetadata changes in order to handle JPA <-> GAE persistence changes
	@Override
	protected void notifyForGenericListener(String upstreamDependency) {
		// If the upstream dependency is null or invalid do not continue
		if (!StringUtils.hasText(upstreamDependency) || !MetadataIdentificationUtils.isValid(upstreamDependency)) {
			return;
		}
		
		//TODO: review need for member details scanning to pick up newly added tags (ideally these should be added automatically during MD processing;
		// We do need to be informed if a new layer is available to see if we should use that
		if (PhysicalTypeIdentifier.isValid(upstreamDependency)) {
			MemberDetails memberDetails = getMemberDetails(PhysicalTypeIdentifier.getJavaType(upstreamDependency));
			if (memberDetails != null) {
				MemberHoldingTypeDetails memberHoldingTypeDetails = MemberFindingUtils.getMostConcreteMemberHoldingTypeDetailsWithTag(memberDetails, LayerCustomDataKeys.LAYER_TYPE);
				if (memberHoldingTypeDetails != null) {
					@SuppressWarnings("unchecked")
					List<JavaType> domainTypes = (List<JavaType>) memberHoldingTypeDetails.getCustomData().get(LayerCustomDataKeys.LAYER_TYPE);
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
	}

	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		// We need to parse the annotation, which we expect to be present
		WebScaffoldAnnotationValues annotationValues = new WebScaffoldAnnotationValues(governorPhysicalTypeMetadata);
		if (!annotationValues.isAnnotationFound() || annotationValues.getFormBackingObject() == null || !annotationValues.isExposeJson() || governorPhysicalTypeMetadata.getMemberHoldingTypeDetails() == null) {
			return null;
		}
		
		// Lookup the form backing object's metadata
		JavaType domainEntity = annotationValues.getFormBackingObject();
		JsonMetadata jsonMetadata = (JsonMetadata) metadataService.get(JsonMetadata.createIdentifier(domainEntity, Path.SRC_MAIN_JAVA));
		if (jsonMetadata == null) {
			return null;
		}
		
		PhysicalTypeMetadata formBackingObjectPhysicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(domainEntity, Path.SRC_MAIN_JAVA));
		Assert.notNull(formBackingObjectPhysicalTypeMetadata, "Unable to obtain physical type metadata for type " + domainEntity.getFullyQualifiedTypeName());
		MemberDetails formBackingObjectMemberDetails = getMemberDetails(formBackingObjectPhysicalTypeMetadata);
		
		MemberHoldingTypeDetails formBackingMemberHoldingTypeDetails = MemberFindingUtils.getMostConcreteMemberHoldingTypeDetailsWithTag(formBackingObjectMemberDetails, PersistenceCustomDataKeys.PERSISTENT_TYPE);
		if (formBackingMemberHoldingTypeDetails == null) {
			return null;
		}
		
		// We need to be informed if our dependent metadata changes
		metadataDependencyRegistry.registerDependency(formBackingMemberHoldingTypeDetails.getDeclaredByMetadataId(), metadataIdentificationString);
		
		Set<FinderMetadataDetails> finderDetails = webMetadataService.getDynamicFinderMethodsAndFields(domainEntity, formBackingObjectMemberDetails, metadataIdentificationString);
		
		MemberDetails memberDetails = getMemberDetails(governorPhysicalTypeMetadata);
		final Map<String, MemberTypeAdditions> persistenceAdditions = webMetadataService.getCrudAdditions(domainEntity, metadataIdentificationString);
		// TODO: remove dependence on this type once we can access identifier and version fields through the layer service
		JavaTypePersistenceMetadataDetails javaTypePersistenceMetadataDetails = webMetadataService.getJavaTypePersistenceMetadataDetails(domainEntity, getMemberDetails(domainEntity), metadataIdentificationString);
		PluralMetadata plural = (PluralMetadata) metadataService.get(PluralMetadata.createIdentifier(domainEntity));
		if (persistenceAdditions.isEmpty() || javaTypePersistenceMetadataDetails == null || plural == null) {
			return null;
		}
		
		// maintain a list of entities that are being tested
		managedEntityTypes.put(domainEntity, metadataIdentificationString);
		
		return new WebJsonMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, annotationValues, memberDetails, persistenceAdditions, javaTypePersistenceMetadataDetails.getIdentifierField(), plural.getPlural(), finderDetails, jsonMetadata);
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