package org.springframework.roo.addon.test;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.configurable.ConfigurableMetadataProvider;
import org.springframework.roo.addon.dod.DataOnDemandMetadata;
import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.layers.LayerCustomDataKeys;
import org.springframework.roo.project.layers.LayerService;
import org.springframework.roo.project.layers.LayerType;
import org.springframework.roo.project.layers.MemberTypeAdditions;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.Pair;
import org.springframework.roo.support.util.StringUtils;

/**
 * Implementation of {@link IntegrationTestMetadataProvider}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component(immediate = true)
@Service
public final class IntegrationTestMetadataProviderImpl extends AbstractItdMetadataProvider implements IntegrationTestMetadataProvider {
	@Reference private ConfigurableMetadataProvider configurableMetadataProvider;
	@Reference private ProjectOperations projectOperations;
	@Reference private LayerService layerService;
	private Set<String> producedMids = new LinkedHashSet<String>();
	private Map<JavaType, String> managedEntityTypes = new HashMap<JavaType, String>();
	private Boolean wasGaeEnabled = null;
	private static final int LAYER_POSITION = LayerType.HIGHEST.getPosition();

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.addNotificationListener(this);
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		// Integration test classes are @Configurable because they may need DI of other DOD classes that provide M:1 relationships
		configurableMetadataProvider.addMetadataTrigger(new JavaType(RooIntegrationTest.class.getName()));
		addMetadataTrigger(new JavaType(RooIntegrationTest.class.getName()));
	}
	
	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.removeNotificationListener(this);
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		configurableMetadataProvider.removeMetadataTrigger(new JavaType(RooIntegrationTest.class.getName()));
		removeMetadataTrigger(new JavaType(RooIntegrationTest.class.getName()));
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
		
		// If the upstream dependency isn't ProjectMetadata do not continue
		if (upstreamDependency.equals(ProjectMetadata.getProjectIdentifier())) {
			ProjectMetadata projectMetadata = projectOperations.getProjectMetadata();
			// If ProjectMetadata isn't valid do not continue
			if (projectMetadata == null || !projectMetadata.isValid()) {
				return;
			}
			boolean isGaeEnabled = projectMetadata.isGaeEnabled();
			// We need to determine if the persistence state has changed, we do this by comparing the last known state to the current state
			boolean hasGaeStateChanged = wasGaeEnabled == null || isGaeEnabled != wasGaeEnabled;
			if (hasGaeStateChanged) {
				wasGaeEnabled = isGaeEnabled;
				for (String producedMid : producedMids) {
					metadataService.get(producedMid, true);
				}
			}
		}
	}
	
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		// We know governor type details are non-null and can be safely cast
		
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		if (projectMetadata == null || !projectMetadata.isValid()) {
			return null;
		}
		
		// We need to parse the annotation, which we expect to be present
		IntegrationTestAnnotationValues annotationValues = new IntegrationTestAnnotationValues(governorPhysicalTypeMetadata);
		final JavaType entity = annotationValues.getEntity();
		if (!annotationValues.isAnnotationFound() || entity == null) {
			return null;
		}
		
		// Lookup the DOD's metadata. The DOD must conform to the DOD naming conventions
		JavaType dodJavaType = new JavaType(annotationValues.getEntity().getFullyQualifiedTypeName() + "DataOnDemand");
		String dataOnDemandMetadataKey = DataOnDemandMetadata.createIdentifier(dodJavaType, Path.SRC_TEST_JAVA);
		DataOnDemandMetadata dataOnDemandMetadata = (DataOnDemandMetadata) metadataService.get(dataOnDemandMetadataKey);

		// We need to be informed if our dependent metadata changes
		metadataDependencyRegistry.registerDependency(dataOnDemandMetadataKey, metadataIdentificationString);

		if (dataOnDemandMetadata == null || !dataOnDemandMetadata.isValid()) {
			return null;
		}
		
		// Lookup the entity's metadata
		MemberDetails memberDetails = getMemberDetails(entity);
		if (memberDetails == null) {
			return null;
		}
		
		MethodMetadata identifierAccessorMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, PersistenceCustomDataKeys.IDENTIFIER_ACCESSOR_METHOD);
		MethodMetadata versionAccessorMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, PersistenceCustomDataKeys.VERSION_ACCESSOR_METHOD);
		MethodMetadata countMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, PersistenceCustomDataKeys.COUNT_ALL_METHOD);
		MethodMetadata findMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, PersistenceCustomDataKeys.FIND_METHOD);
		MemberTypeAdditions findAllMethodAdditions = layerService.getMemberTypeAdditions(metadataIdentificationString, PersistenceCustomDataKeys.FIND_ALL_METHOD.name(), entity, LAYER_POSITION);
		MethodMetadata findEntriesMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, PersistenceCustomDataKeys.FIND_ENTRIES_METHOD);
		MethodMetadata flushMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, PersistenceCustomDataKeys.FLUSH_METHOD);
		MemberTypeAdditions mergeMethodAdditions = layerService.getMemberTypeAdditions(metadataIdentificationString, PersistenceCustomDataKeys.MERGE_METHOD.name(), entity, LAYER_POSITION, new Pair<JavaType, JavaSymbolName>(entity, new JavaSymbolName("obj")));
		MemberTypeAdditions persistMethodAdditions = layerService.getMemberTypeAdditions(metadataIdentificationString, PersistenceCustomDataKeys.PERSIST_METHOD.name(), entity, LAYER_POSITION, new Pair<JavaType, JavaSymbolName>(entity, new JavaSymbolName("obj")));
		MemberTypeAdditions removeMethodAdditions = layerService.getMemberTypeAdditions(metadataIdentificationString, PersistenceCustomDataKeys.REMOVE_METHOD.name(), entity, LAYER_POSITION, new Pair<JavaType, JavaSymbolName>(entity, new JavaSymbolName("obj")));
		if (persistMethodAdditions == null || flushMethod == null || findMethod == null || identifierAccessorMethod == null) {
			return null;
		}
	
		String transactionManager = null;
		AnnotationMetadata rooEntity = MemberFindingUtils.getDeclaredTypeAnnotation(memberDetails, new JavaType("org.springframework.roo.addon.entity.RooEntity"));
		if (rooEntity != null) {
			StringAttributeValue transactionManagerAttr = (StringAttributeValue) rooEntity.getAttribute(new JavaSymbolName("transactionManager"));
			if (transactionManagerAttr != null) {
				transactionManager = transactionManagerAttr.getValue();
			}
		}
		
		boolean hasEmbeddedIdentifier = dataOnDemandMetadata.hasEmbeddedIdentifier();
		boolean entityHasSuperclass  = getEntitySuperclass(annotationValues.getEntity()) != null;

		for (MemberHoldingTypeDetails memberHoldingTypeDetails : memberDetails.getDetails()) {
			if (memberHoldingTypeDetails instanceof ClassOrInterfaceTypeDetails) {
				metadataDependencyRegistry.registerDependency(memberHoldingTypeDetails.getDeclaredByMetadataId(), metadataIdentificationString);
				break;
			}
		}

		// In order to handle switching between GAE and JPA produced MIDs need to be remembered so they can be regenerated on JPA <-> GAE switch
		producedMids.add(metadataIdentificationString);
		
		// maintain a list of entities that are being tested
		managedEntityTypes.put(entity, metadataIdentificationString);

		return new IntegrationTestMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, projectMetadata, annotationValues, dataOnDemandMetadata, identifierAccessorMethod, versionAccessorMethod, countMethod, findMethod, findAllMethodAdditions, findEntriesMethod, flushMethod, mergeMethodAdditions, persistMethodAdditions, removeMethodAdditions, transactionManager, hasEmbeddedIdentifier, entityHasSuperclass);
	}
	
	private ClassOrInterfaceTypeDetails getEntitySuperclass(JavaType entity) {
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(entity, Path.SRC_MAIN_JAVA);
		PhysicalTypeMetadata ptm = (PhysicalTypeMetadata) metadataService.get(physicalTypeIdentifier);
		Assert.notNull(ptm, "Java source code unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(physicalTypeIdentifier));
		PhysicalTypeDetails ptd = ptm.getMemberHoldingTypeDetails();
		Assert.notNull(ptd, "Java source code details unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(physicalTypeIdentifier));
		Assert.isInstanceOf(ClassOrInterfaceTypeDetails.class, ptd, "Java source code is immutable for type " + PhysicalTypeIdentifier.getFriendlyName(physicalTypeIdentifier));
		ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails = (ClassOrInterfaceTypeDetails) ptd;
		return classOrInterfaceTypeDetails.getSuperclass();
	}
	
	public String getItdUniquenessFilenameSuffix() {
		return "IntegrationTest";
	}

	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = IntegrationTestMetadata.getJavaType(metadataIdentificationString);
		Path path = IntegrationTestMetadata.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}

	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return IntegrationTestMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return IntegrationTestMetadata.getMetadataIdentiferType();
	}
}