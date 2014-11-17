package org.springframework.roo.addon.dod;

import static org.springframework.roo.classpath.customdata.CustomDataKeys.EMBEDDED_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.EMBEDDED_ID_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_ENTRIES_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.IDENTIFIER_ACCESSOR_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.IDENTIFIER_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.MANY_TO_MANY_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.MANY_TO_ONE_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.ONE_TO_MANY_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.ONE_TO_ONE_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.PERSISTENT_TYPE;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.TRANSIENT_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.VERSION_FIELD;
import static org.springframework.roo.model.RooJavaType.ROO_DATA_ON_DEMAND;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.configurable.ConfigurableMetadataProvider;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.CustomDataKeys;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.layers.LayerService;
import org.springframework.roo.classpath.layers.LayerType;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.classpath.layers.MethodParameter;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.shell.NaturalOrderComparator;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Implementation of {@link DataOnDemandMetadataProvider}.
 * 
 * @author Ben Alex
 * @author Greg Turnquist
 * @author Andrew Swan
 * @since 1.0
 */
@Component
@Service
public class DataOnDemandMetadataProviderImpl extends
        AbstractMemberDiscoveringItdMetadataProvider implements
        DataOnDemandMetadataProvider {
	
	protected final static Logger LOGGER = HandlerUtils.getLogger(DataOnDemandMetadataProviderImpl.class);

    private static final String FLUSH_METHOD = CustomDataKeys.FLUSH_METHOD
            .name();
    private static final String PERSIST_METHOD = CustomDataKeys.PERSIST_METHOD
            .name();

    private ConfigurableMetadataProvider configurableMetadataProvider;
    private LayerService layerService;
    private final Map<String, JavaType> dodMidToEntityMap = new LinkedHashMap<String, JavaType>();
    private final Map<JavaType, String> entityToDodMidMap = new LinkedHashMap<JavaType, String>();

    protected void activate(final ComponentContext cContext) {
    	context = cContext.getBundleContext();
        getMetadataDependencyRegistry().addNotificationListener(this);
        getMetadataDependencyRegistry().registerDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        // DOD classes are @Configurable because they may need DI of other DOD
        // classes that provide M:1 relationships
        getConfigurableMetadataProvider().addMetadataTrigger(ROO_DATA_ON_DEMAND);
        addMetadataTrigger(ROO_DATA_ON_DEMAND);
    }

    @Override
    protected String createLocalIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return DataOnDemandMetadata.createIdentifier(javaType, path);
    }

    protected void deactivate(final ComponentContext context) {
        getMetadataDependencyRegistry().removeNotificationListener(this);
        getMetadataDependencyRegistry().deregisterDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        getConfigurableMetadataProvider().removeMetadataTrigger(ROO_DATA_ON_DEMAND);
        removeMetadataTrigger(ROO_DATA_ON_DEMAND);
    }

    private String getDataOnDemandMetadataId(final JavaType javaType,
            final Iterable<ClassOrInterfaceTypeDetails> dataOnDemandTypes) {
        for (final ClassOrInterfaceTypeDetails cid : dataOnDemandTypes) {
            final AnnotationMetadata dodAnnotation = cid
                    .getAnnotation(ROO_DATA_ON_DEMAND);
            final AnnotationAttributeValue<JavaType> entityAttribute = dodAnnotation
                    .getAttribute("entity");
            if (entityAttribute != null
                    && entityAttribute.getValue().equals(javaType)) {
                // Found the DoD type for the given field's type
                return DataOnDemandMetadata.createIdentifier(cid.getName(),
                        PhysicalTypeIdentifier.getPath(cid
                                .getDeclaredByMetadataId()));
            }
        }
        return null;
    }

    private List<EmbeddedHolder> getEmbeddedHolders(
            final MemberDetails memberDetails,
            final String metadataIdentificationString) {
        final List<EmbeddedHolder> embeddedHolders = new ArrayList<EmbeddedHolder>();

        final List<FieldMetadata> embeddedFields = MemberFindingUtils
                .getFieldsWithTag(memberDetails, EMBEDDED_FIELD);
        if (embeddedFields.isEmpty()) {
            return embeddedHolders;
        }

        for (final FieldMetadata embeddedField : embeddedFields) {
            final MemberDetails embeddedMemberDetails = getMemberDetails(embeddedField
                    .getFieldType());
            if (embeddedMemberDetails == null) {
                continue;
            }

            final List<FieldMetadata> fields = new ArrayList<FieldMetadata>();

            for (final FieldMetadata field : embeddedMemberDetails.getFields()) {
                if (!(Modifier.isStatic(field.getModifier())
                        || Modifier.isFinal(field.getModifier()) || Modifier
                            .isTransient(field.getModifier()))) {
                    getMetadataDependencyRegistry().registerDependency(
                            field.getDeclaredByMetadataId(),
                            metadataIdentificationString);
                    fields.add(field);
                }
            }
            embeddedHolders.add(new EmbeddedHolder(embeddedField, fields));
        }

        return embeddedHolders;
    }

    private EmbeddedIdHolder getEmbeddedIdHolder(
            final MemberDetails memberDetails,
            final String metadataIdentificationString) {
        final List<FieldMetadata> idFields = new ArrayList<FieldMetadata>();
        final List<FieldMetadata> fields = MemberFindingUtils.getFieldsWithTag(
                memberDetails, EMBEDDED_ID_FIELD);
        if (fields.isEmpty()) {
            return null;
        }
        final FieldMetadata embeddedIdField = fields.get(0);
        final MemberDetails identifierMemberDetails = getMemberDetails(embeddedIdField
                .getFieldType());
        if (identifierMemberDetails == null) {
            return null;
        }

        for (final FieldMetadata field : identifierMemberDetails.getFields()) {
            if (!(Modifier.isStatic(field.getModifier())
                    || Modifier.isFinal(field.getModifier()) || Modifier
                        .isTransient(field.getModifier()))) {
                getMetadataDependencyRegistry().registerDependency(
                        field.getDeclaredByMetadataId(),
                        metadataIdentificationString);
                idFields.add(field);
            }
        }

        return new EmbeddedIdHolder(embeddedIdField, idFields);
    }

    @Override
    protected String getGovernorPhysicalTypeIdentifier(
            final String metadataIdentificationString) {
        final JavaType javaType = DataOnDemandMetadata
                .getJavaType(metadataIdentificationString);
        final LogicalPath path = DataOnDemandMetadata
                .getPath(metadataIdentificationString);
        return PhysicalTypeIdentifier.createIdentifier(javaType, path);
    }

    public String getItdUniquenessFilenameSuffix() {
        return "DataOnDemand";
    }

    @Override
    protected String getLocalMidToRequest(final ItdTypeDetails itdTypeDetails) {
        // Determine the governor for this ITD, and whether any DOD metadata is
        // even hoping to hear about changes to that JavaType and its ITDs
        final JavaType governor = itdTypeDetails.getName();

        for (final JavaType type : itdTypeDetails.getGovernor()
                .getLayerEntities()) {
            final String localMidType = entityToDodMidMap.get(type);
            if (localMidType != null) {
                return localMidType;
            }
        }

        final String localMid = entityToDodMidMap.get(governor);
        if (localMid == null) {
            // No DOD is interested in this JavaType, so let's move on
            return null;
        }

        // We have some DOD metadata, so let's check if we care if any methods
        // match our requirements
        for (final MethodMetadata method : itdTypeDetails.getDeclaredMethods()) {
            if (BeanInfoUtils.isMutatorMethod(method)) {
                // A DOD cares about the JavaType, and an ITD offers a method
                // likely of interest, so let's formally trigger it to run.
                // Note that it will re-scan and discover this ITD, and register
                // a direct dependency on it for the future.
                return localMid;
            }
        }

        return null;
    }

    private Map<FieldMetadata, DataOnDemandMetadata> getLocatedFields(
            final MemberDetails memberDetails, final String dodMetadataId) {
        final Map<FieldMetadata, DataOnDemandMetadata> locatedFields = new LinkedHashMap<FieldMetadata, DataOnDemandMetadata>();
        final Iterable<ClassOrInterfaceTypeDetails> dataOnDemandTypes = typeLocationService
                .findClassesOrInterfaceDetailsWithAnnotation(ROO_DATA_ON_DEMAND);

        final List<MethodMetadata> mutatorMethods = memberDetails.getMethods();
        // To avoid unnecessary rewriting of the DoD ITD we sort the mutators by
        // method name to provide a consistent ordering
        Collections.sort(mutatorMethods,
                new NaturalOrderComparator<MethodMetadata>() {
                    @Override
                    protected String stringify(final MethodMetadata object) {
                        return object.getMethodName().getSymbolName();
                    }
                });

        for (final MethodMetadata method : mutatorMethods) {
            if (!BeanInfoUtils.isMutatorMethod(method)) {
                continue;
            }

            final FieldMetadata field = BeanInfoUtils
                    .getFieldForJavaBeanMethod(memberDetails, method);
            if (field == null) {
                continue;
            }

            // Track any changes to the mutator method (eg it goes away)
            getMetadataDependencyRegistry().registerDependency(
                    method.getDeclaredByMetadataId(), dodMetadataId);

            final Set<Object> fieldCustomDataKeys = field.getCustomData()
                    .keySet();

            // Never include id or version fields (they shouldn't normally have
            // a mutator anyway, but the user might have added one), or embedded
            // types
            if (fieldCustomDataKeys.contains(IDENTIFIER_FIELD)
                    || fieldCustomDataKeys.contains(EMBEDDED_ID_FIELD)
                    || fieldCustomDataKeys.contains(EMBEDDED_FIELD)
                    || fieldCustomDataKeys.contains(VERSION_FIELD)) {
                continue;
            }

            // Never include persistence transient fields
            if (fieldCustomDataKeys.contains(TRANSIENT_FIELD)) {
                continue;
            }

            // Never include any sort of collection; user has to make such
            // entities by hand
            if (field.getFieldType().isCommonCollectionType()
                    || fieldCustomDataKeys.contains(ONE_TO_MANY_FIELD)
                    || fieldCustomDataKeys.contains(MANY_TO_MANY_FIELD)) {
                continue;
            }

            // Look up collaborating metadata
            final DataOnDemandMetadata collaboratingMetadata = locateCollaboratingMetadata(
                    dodMetadataId, field, dataOnDemandTypes);
            locatedFields.put(field, collaboratingMetadata);
        }

        return locatedFields;
    }

    @Override
    protected ItdTypeDetailsProvidingMetadataItem getMetadata(
            final String dodMetadataId, final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final String itdFilename) {
    	
    	if(layerService == null){
    		layerService = getLayerService();
    	}
    	Validate.notNull(layerService, "LayerService is required");
    	
        // We need to parse the annotation, which we expect to be present
        final DataOnDemandAnnotationValues annotationValues = new DataOnDemandAnnotationValues(
                governorPhysicalTypeMetadata);
        final JavaType entity = annotationValues.getEntity();
        if (!annotationValues.isAnnotationFound() || entity == null) {
            return null;
        }

        // Remember that this entity JavaType matches up with this DOD's
        // metadata identification string
        // Start by clearing the previous association
        final JavaType oldEntity = dodMidToEntityMap.get(dodMetadataId);
        if (oldEntity != null) {
            entityToDodMidMap.remove(oldEntity);
        }
        entityToDodMidMap.put(annotationValues.getEntity(), dodMetadataId);
        dodMidToEntityMap.put(dodMetadataId, annotationValues.getEntity());

        final JavaType identifierType = getPersistenceMemberLocator()
                .getIdentifierType(entity);
        if (identifierType == null) {
            return null;
        }

        final MemberDetails memberDetails = getMemberDetails(entity);
        if (memberDetails == null) {
            return null;
        }

        final MemberHoldingTypeDetails persistenceMemberHoldingTypeDetails = MemberFindingUtils
                .getMostConcreteMemberHoldingTypeDetailsWithTag(memberDetails,
                        PERSISTENT_TYPE);
        if (persistenceMemberHoldingTypeDetails == null) {
            return null;
        }

        // We need to be informed if our dependent metadata changes
        getMetadataDependencyRegistry().registerDependency(
                persistenceMemberHoldingTypeDetails.getDeclaredByMetadataId(),
                dodMetadataId);

        // Get the additions to make for each required method
        final MethodParameter fromParameter = new MethodParameter(
                JavaType.INT_PRIMITIVE, "from");
        final MethodParameter toParameter = new MethodParameter(
                JavaType.INT_PRIMITIVE, "to");
        final MemberTypeAdditions findEntriesMethod = layerService
                .getMemberTypeAdditions(dodMetadataId,
                        FIND_ENTRIES_METHOD.name(), entity, identifierType,
                        LayerType.HIGHEST.getPosition(), fromParameter,
                        toParameter);
        final MemberTypeAdditions findMethodAdditions = layerService
                .getMemberTypeAdditions(dodMetadataId, FIND_METHOD.name(),
                        entity, identifierType,
                        LayerType.HIGHEST.getPosition(), new MethodParameter(
                                identifierType, "id"));
        final MethodParameter entityParameter = new MethodParameter(entity,
                "obj");
        final MemberTypeAdditions flushMethod = layerService
                .getMemberTypeAdditions(dodMetadataId, FLUSH_METHOD, entity,
                        identifierType, LayerType.HIGHEST.getPosition(),
                        entityParameter);
        final MethodMetadata identifierAccessor = memberDetails
                .getMostConcreteMethodWithTag(IDENTIFIER_ACCESSOR_METHOD);
        final MemberTypeAdditions persistMethodAdditions = layerService
                .getMemberTypeAdditions(dodMetadataId, PERSIST_METHOD, entity,
                        identifierType, LayerType.HIGHEST.getPosition(),
                        entityParameter);

        if (findEntriesMethod == null || findMethodAdditions == null
                || identifierAccessor == null || persistMethodAdditions == null) {
            return null;
        }

        // Identify all the fields we care about on the entity
        final Map<FieldMetadata, DataOnDemandMetadata> locatedFields = getLocatedFields(
                memberDetails, dodMetadataId);

        // Get the embedded identifier metadata holder - may be null if no
        // embedded identifier exists
        final EmbeddedIdHolder embeddedIdHolder = getEmbeddedIdHolder(
                memberDetails, dodMetadataId);

        // Get the list of embedded metadata holders - may be an empty list if
        // no embedded identifier exists
        final List<EmbeddedHolder> embeddedHolders = getEmbeddedHolders(
                memberDetails, dodMetadataId);

        return new DataOnDemandMetadata(dodMetadataId, aspectName,
                governorPhysicalTypeMetadata, annotationValues,
                identifierAccessor, findMethodAdditions, findEntriesMethod,
                persistMethodAdditions, flushMethod, locatedFields,
                identifierType, embeddedIdHolder, embeddedHolders);
    }

    public String getProvidesType() {
        return DataOnDemandMetadata.getMetadataIdentiferType();
    }

    /**
     * Returns the {@link DataOnDemandMetadata} for the entity that's the target
     * of the given reference field.
     * 
     * @param dodMetadataId
     * @param field
     * @param dataOnDemandTypes
     * @return <code>null</code> if it's not an n:1 or 1:1 field, or the DoD
     *         metadata is simply not available
     */
    private DataOnDemandMetadata locateCollaboratingMetadata(
            final String dodMetadataId, final FieldMetadata field,
            final Iterable<ClassOrInterfaceTypeDetails> dataOnDemandTypes) {
        if (!(field.getCustomData().keySet().contains(MANY_TO_ONE_FIELD) || field
                .getCustomData().keySet().contains(ONE_TO_ONE_FIELD))) {
            return null;
        }

        final String otherDodMetadataId = getDataOnDemandMetadataId(
                field.getFieldType(), dataOnDemandTypes);

        if (otherDodMetadataId == null
                || otherDodMetadataId.equals(dodMetadataId)) {
            // No DoD for this field's type, or it's a self-reference
            return null;
        }

        // Make this DoD depend on the related entity (not its Dod, otherwise
        // we get a circular MD dependency)
        registerDependencyUponType(dodMetadataId, field.getFieldType());

        return (DataOnDemandMetadata) getMetadataService().get(otherDodMetadataId);
    }

    private void registerDependencyUponType(final String dodMetadataId,
            final JavaType type) {
        final String fieldPhysicalTypeId = typeLocationService
                .getPhysicalTypeIdentifier(type);
        getMetadataDependencyRegistry().registerDependency(fieldPhysicalTypeId,
                dodMetadataId);
    }
    
    public ConfigurableMetadataProvider getConfigurableMetadataProvider(){
    	if(configurableMetadataProvider == null){
    		// Get all Services implement ConfigurableMetadataProvider interface
    		try {
    			ServiceReference<?>[] references = context.getAllServiceReferences(ConfigurableMetadataProvider.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				return (ConfigurableMetadataProvider) context.getService(ref);
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load ConfigurableMetadataProvider on DataOnDemandMetadataProviderImpl.");
    			return null;
    		}
    	}else{
    		return configurableMetadataProvider;
    	}
    	
    }
    
    public LayerService getLayerService(){
    	// Get all Services implement LayerService interface
		try {
			ServiceReference<?>[] references = context.getAllServiceReferences(LayerService.class.getName(), null);
			
			for(ServiceReference<?> ref : references){
				return (LayerService) context.getService(ref);
			}
			
			return null;
			
		} catch (InvalidSyntaxException e) {
			LOGGER.warning("Cannot load LayerService on DataOnDemandMetadataProviderImpl.");
			return null;
		}
    }
}
