package org.springframework.roo.addon.test;

import static org.springframework.roo.classpath.customdata.CustomDataKeys.COUNT_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_ENTRIES_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FLUSH_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.IDENTIFIER_ACCESSOR_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.LAYER_TYPE;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.MERGE_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.PERSIST_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.REMOVE_METHOD;
import static org.springframework.roo.model.JavaType.INT_PRIMITIVE;
import static org.springframework.roo.model.RooJavaType.ROO_DATA_ON_DEMAND;
import static org.springframework.roo.model.RooJavaType.ROO_INTEGRATION_TEST;
import static org.springframework.roo.model.RooJavaType.ROO_JPA_ACTIVE_RECORD;

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
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.layers.LayerService;
import org.springframework.roo.classpath.layers.LayerType;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.classpath.layers.MethodParameter;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.ContextualPath;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.Assert;

/**
 * Implementation of {@link IntegrationTestMetadataProvider}.
 *
 * @author Ben Alex
 * @since 1.0
 */
@Component(immediate = true)
@Service
public class IntegrationTestMetadataProviderImpl extends AbstractItdMetadataProvider implements IntegrationTestMetadataProvider {

	// Constants
	private static final int LAYER_POSITION = LayerType.HIGHEST.getPosition();
	private static final JavaSymbolName TRANSACTION_MANAGER_ATTRIBUTE = new JavaSymbolName("transactionManager");

	// Fields
	@Reference private ConfigurableMetadataProvider configurableMetadataProvider;
	@Reference private LayerService layerService;
	@Reference private ProjectOperations projectOperations;

	private final Map<JavaType, String> managedEntityTypes = new HashMap<JavaType, String>();
	private final Set<String> producedMids = new LinkedHashSet<String>();
	private Boolean wasGaeEnabled;

	protected void activate(final ComponentContext context) {
		metadataDependencyRegistry.addNotificationListener(this);
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		// Integration test classes are @Configurable because they may need DI of other DOD classes that provide M:1 relationships
		configurableMetadataProvider.addMetadataTrigger(ROO_INTEGRATION_TEST);
		addMetadataTrigger(ROO_INTEGRATION_TEST);
	}

	protected void deactivate(final ComponentContext context) {
		metadataDependencyRegistry.removeNotificationListener(this);
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		configurableMetadataProvider.removeMetadataTrigger(ROO_INTEGRATION_TEST);
		removeMetadataTrigger(ROO_INTEGRATION_TEST);
	}

	@Override
	protected void notifyForGenericListener(final String upstreamDependency) {
		if (PhysicalTypeIdentifier.isValid(upstreamDependency)) {
			handleGenericChangeToPhysicalType(PhysicalTypeIdentifier.getJavaType(upstreamDependency));
		}
		if (ProjectMetadata.isValid(upstreamDependency)) {
			handleGenericChangeToProject(ProjectMetadata.getModuleName(upstreamDependency));
		}
	}
	
	/**
	 * Handles a generic change (i.e. with no explicit downstream dependency)
	 * to the given physical type
	 * 
	 * @param physicalType the type that changed (required)
	 */
	private void handleGenericChangeToPhysicalType(final JavaType physicalType) {
		handleChangesToTestedEntities(physicalType);		
		handleChangesToLayeringForTestedEntities(physicalType);
	}

	private void handleChangesToTestedEntities(final JavaType physicalType) {
		final String localMid = managedEntityTypes.get(physicalType);
		if (localMid != null) {
			// One of the entities for which we produce metadata has changed; refresh that metadata
			metadataService.get(localMid);
		}
	}

	private void handleChangesToLayeringForTestedEntities(final JavaType physicalType) {
		final MemberHoldingTypeDetails memberHoldingTypeDetails = typeLocationService.getTypeDetails(physicalType);
		if (memberHoldingTypeDetails != null) {
			for (final JavaType type : memberHoldingTypeDetails.getLayerEntities()) {
				handleChangesToTestedEntities(type);
			}
		}
	}
	
	/**
	 * Handles a generic change (i.e. with no explicit downstream dependency)
	 * to the project metadata
	 */
	private void handleGenericChangeToProject(String moduleName) {
		final ProjectMetadata projectMetadata = projectOperations.getProjectMetadata(moduleName);
		if (projectMetadata != null && projectMetadata.isValid()) {
			final boolean isGaeEnabled = projectOperations.isGaeEnabled(moduleName);
			// We need to determine if the persistence state has changed, we do this by comparing the last known state to the current state
			final boolean hasGaeStateChanged = wasGaeEnabled == null || isGaeEnabled != wasGaeEnabled;
			if (hasGaeStateChanged) {
				wasGaeEnabled = isGaeEnabled;
				for (final String producedMid : producedMids) {
					metadataService.get(producedMid, true);
				}
			}
		}
	}

	@Override
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(final String metadataIdentificationString, final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {
		// We need to parse the annotation, which we expect to be present
		final IntegrationTestAnnotationValues annotationValues = new IntegrationTestAnnotationValues(governorPhysicalTypeMetadata);
		final JavaType entity = annotationValues.getEntity();
		if (!annotationValues.isAnnotationFound() || entity == null) {
			return null;
		}

		final JavaType dataOnDemandType = getDataOnDemandType(entity);
		final String dataOnDemandMetadataKey = DataOnDemandMetadata.createIdentifier(dataOnDemandType, typeLocationService.getTypePath(dataOnDemandType));
		final DataOnDemandMetadata dataOnDemandMetadata = (DataOnDemandMetadata) metadataService.get(dataOnDemandMetadataKey);

		// We need to be informed if our dependent metadata changes
		metadataDependencyRegistry.registerDependency(dataOnDemandMetadataKey, metadataIdentificationString);

		if (dataOnDemandMetadata == null || !dataOnDemandMetadata.isValid()) {
			return null;
		}

		final MemberDetails memberDetails = getMemberDetails(entity);
		if (memberDetails == null) {
			return null;
		}

		final JavaType identifierType = persistenceMemberLocator.getIdentifierType(entity);
		if (identifierType == null) {
			return null;
		}

		final MethodParameter firstResultParameter = new MethodParameter(INT_PRIMITIVE, "firstResult");
		final MethodParameter maxResultsParameter = new MethodParameter(INT_PRIMITIVE, "maxResults");

		final MethodMetadata identifierAccessorMethod = memberDetails.getMostConcreteMethodWithTag(IDENTIFIER_ACCESSOR_METHOD);
		final MethodMetadata versionAccessorMethod = persistenceMemberLocator.getVersionAccessor(entity);
		final MemberTypeAdditions countMethodAdditions = layerService.getMemberTypeAdditions(metadataIdentificationString, COUNT_ALL_METHOD.name(), entity, identifierType, LAYER_POSITION);
		final MemberTypeAdditions findMethodAdditions = layerService.getMemberTypeAdditions(metadataIdentificationString, FIND_METHOD.name(), entity, identifierType, LAYER_POSITION, new MethodParameter(identifierType, "id"));
		final MemberTypeAdditions findAllMethodAdditions = layerService.getMemberTypeAdditions(metadataIdentificationString, FIND_ALL_METHOD.name(), entity, identifierType, LAYER_POSITION);
		final MemberTypeAdditions findEntriesMethod = layerService.getMemberTypeAdditions(metadataIdentificationString, FIND_ENTRIES_METHOD.name(), entity, identifierType, LAYER_POSITION, firstResultParameter, maxResultsParameter);
		final MethodParameter entityParameter = new MethodParameter(entity, "obj");
		final MemberTypeAdditions flushMethodAdditions = layerService.getMemberTypeAdditions(metadataIdentificationString, FLUSH_METHOD.name(), entity, identifierType, LAYER_POSITION, entityParameter);
		final MemberTypeAdditions mergeMethodAdditions = layerService.getMemberTypeAdditions(metadataIdentificationString, MERGE_METHOD.name(), entity, identifierType, LAYER_POSITION, entityParameter);
		final MemberTypeAdditions persistMethodAdditions = layerService.getMemberTypeAdditions(metadataIdentificationString, PERSIST_METHOD.name(), entity, identifierType, LAYER_POSITION, entityParameter);
		final MemberTypeAdditions removeMethodAdditions = layerService.getMemberTypeAdditions(metadataIdentificationString, REMOVE_METHOD.name(), entity, identifierType, LAYER_POSITION, entityParameter);
		if (persistMethodAdditions == null || findMethodAdditions == null || identifierAccessorMethod == null) {
			return null;
		}

		String transactionManager = null;
		final AnnotationMetadata rooEntityAnnotation = memberDetails.getAnnotation(ROO_JPA_ACTIVE_RECORD);
		if (rooEntityAnnotation != null) {
			final StringAttributeValue transactionManagerAttr = (StringAttributeValue) rooEntityAnnotation.getAttribute(TRANSACTION_MANAGER_ATTRIBUTE);
			if (transactionManagerAttr != null) {
				transactionManager = transactionManagerAttr.getValue();
			}
		}

		final boolean hasEmbeddedIdentifier = dataOnDemandMetadata.hasEmbeddedIdentifier();
		final boolean entityHasSuperclass  = getEntitySuperclass(entity) != null;

		for (final MemberHoldingTypeDetails memberHoldingTypeDetails : memberDetails.getDetails()) {
			if (memberHoldingTypeDetails instanceof ClassOrInterfaceTypeDetails) {
				metadataDependencyRegistry.registerDependency(memberHoldingTypeDetails.getDeclaredByMetadataId(), metadataIdentificationString);
				break;
			}
		}

		// In order to handle switching between GAE and JPA produced MIDs need to be remembered so they can be regenerated on JPA <-> GAE switch
		producedMids.add(metadataIdentificationString);

		// Maintain a list of entities that are being tested
		managedEntityTypes.put(entity, metadataIdentificationString);

		boolean isGaeEnabled = false;

		String moduleName = PhysicalTypeIdentifierNamingUtils.getPath(metadataIdentificationString).getModule();
		if (projectOperations.isProjectAvailable(moduleName)) {
			isGaeEnabled = projectOperations.isGaeEnabled(moduleName);
		}

		return new IntegrationTestMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, annotationValues, dataOnDemandMetadata, identifierAccessorMethod, versionAccessorMethod, countMethodAdditions, findMethodAdditions, findAllMethodAdditions, findEntriesMethod, flushMethodAdditions, mergeMethodAdditions, persistMethodAdditions, removeMethodAdditions, transactionManager, hasEmbeddedIdentifier, entityHasSuperclass, isGaeEnabled);
	}

	/**
	 * Returns the {@link JavaType} for the given entity's "data on demand" class.
	 *
	 * @param entity the entity for which to get the DoD type
	 * @return a non-<code>null</code> type (which may or may not exist yet)
	 */
	private JavaType getDataOnDemandType(final JavaType entity) {
		// First check for an existing type with the standard DoD naming convention
		final JavaType defaultDodType = new JavaType(entity.getFullyQualifiedTypeName() + "DataOnDemand");
		if (typeLocationService.getTypeDetails(defaultDodType) != null) {
			return defaultDodType;
		}

		// Otherwise we look through all DoD-annotated classes for this entity's one
		for (final ClassOrInterfaceTypeDetails dodType : typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(ROO_DATA_ON_DEMAND)) {
			final AnnotationMetadata dodAnnotation = MemberFindingUtils.getFirstAnnotation(dodType, ROO_DATA_ON_DEMAND);
			if (dodAnnotation != null && dodAnnotation.getAttribute("entity").getValue().equals(entity)) {
				return dodType.getName();
			}
		}

		// No existing DoD class was found for this entity, so use the default name
		return defaultDodType;
	}

	private ClassOrInterfaceTypeDetails getEntitySuperclass(final JavaType entity) {
		final String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(entity, typeLocationService.getTypePath(entity));
		final PhysicalTypeMetadata ptm = (PhysicalTypeMetadata) metadataService.get(physicalTypeIdentifier);
		Assert.notNull(ptm, "Java source code unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(physicalTypeIdentifier));
		final PhysicalTypeDetails ptd = ptm.getMemberHoldingTypeDetails();
		Assert.notNull(ptd, "Java source code details unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(physicalTypeIdentifier));
		Assert.isInstanceOf(ClassOrInterfaceTypeDetails.class, ptd, "Java source code is immutable for type " + PhysicalTypeIdentifier.getFriendlyName(physicalTypeIdentifier));
		final ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails = (ClassOrInterfaceTypeDetails) ptd;
		return classOrInterfaceTypeDetails.getSuperclass();
	}

	public String getItdUniquenessFilenameSuffix() {
		return "IntegrationTest";
	}

	@Override
	protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
		final JavaType javaType = IntegrationTestMetadata.getJavaType(metadataIdentificationString);
		final ContextualPath path = IntegrationTestMetadata.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}

	@Override
	protected String createLocalIdentifier(final JavaType javaType, final ContextualPath path) {
		return IntegrationTestMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return IntegrationTestMetadata.getMetadataIdentiferType();
	}
}