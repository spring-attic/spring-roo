package org.springframework.roo.addon.jsf.managedbean;

import static org.springframework.roo.addon.jsf.managedbean.JsfManagedBeanMetadata.APPLICATION_TYPE_FIELDS_KEY;
import static org.springframework.roo.addon.jsf.managedbean.JsfManagedBeanMetadata.APPLICATION_TYPE_KEY;
import static org.springframework.roo.addon.jsf.managedbean.JsfManagedBeanMetadata.CRUD_ADDITIONS_KEY;
import static org.springframework.roo.addon.jsf.managedbean.JsfManagedBeanMetadata.ENUMERATED_KEY;
import static org.springframework.roo.addon.jsf.managedbean.JsfManagedBeanMetadata.LIST_VIEW_FIELD_KEY;
import static org.springframework.roo.addon.jsf.managedbean.JsfManagedBeanMetadata.PARAMETER_TYPE_KEY;
import static org.springframework.roo.addon.jsf.managedbean.JsfManagedBeanMetadata.PARAMETER_TYPE_MANAGED_BEAN_NAME_KEY;
import static org.springframework.roo.addon.jsf.managedbean.JsfManagedBeanMetadata.PARAMETER_TYPE_PLURAL_KEY;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.COUNT_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.EMBEDDED_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_ENTRIES_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.MERGE_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.PERSIST_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.REMOVE_METHOD;
import static org.springframework.roo.model.JavaType.BOOLEAN_OBJECT;
import static org.springframework.roo.model.JavaType.BOOLEAN_PRIMITIVE;
import static org.springframework.roo.model.JavaType.BYTE_ARRAY_PRIMITIVE;
import static org.springframework.roo.model.JavaType.INT_PRIMITIVE;
import static org.springframework.roo.model.JdkJavaType.DATE;
import static org.springframework.roo.model.RooJavaType.ROO_JSF_MANAGED_BEAN;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.CustomDataKeys;
import org.springframework.roo.classpath.customdata.tagkeys.MethodMetadataCustomDataKey;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.layers.LayerService;
import org.springframework.roo.classpath.layers.LayerType;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.classpath.layers.MethodParameter;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.CustomDataBuilder;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Implementation of {@link JsfManagedBeanMetadataProvider}.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
@Component
@Service
public class JsfManagedBeanMetadataProviderImpl extends
        AbstractMemberDiscoveringItdMetadataProvider implements
        JsfManagedBeanMetadataProvider {
	
	protected final static Logger LOGGER = HandlerUtils.getLogger(JsfManagedBeanMetadataProviderImpl.class);
	
    private static final int LAYER_POSITION = LayerType.HIGHEST.getPosition();
    // -- The maximum number of fields to form a String to show in a drop down
    // field.
    private static final int MAX_DROP_DOWN_FIELDS = 4;
    // -- The maximum number of entity fields to show in a list view.
    private static final int MAX_LIST_VIEW_FIELDS = 5;

    private ConfigurableMetadataProvider configurableMetadataProvider;
    private LayerService layerService;
    private final Map<JavaType, String> entityToManagedBeanMidMap = new LinkedHashMap<JavaType, String>();
    private final Map<String, JavaType> managedBeanMidToEntityMap = new LinkedHashMap<String, JavaType>();

    protected void activate(final ComponentContext cContext) {
    	context = cContext.getBundleContext();
        getMetadataDependencyRegistry().addNotificationListener(this);
        getMetadataDependencyRegistry().registerDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        addMetadataTrigger(ROO_JSF_MANAGED_BEAN);
        getConfigurableMetadataProvider().addMetadataTrigger(ROO_JSF_MANAGED_BEAN);
    }

    @Override
    protected String createLocalIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return JsfManagedBeanMetadata.createIdentifier(javaType, path);
    }

    protected void deactivate(final ComponentContext context) {
        getMetadataDependencyRegistry().removeNotificationListener(this);
        getMetadataDependencyRegistry().deregisterDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        removeMetadataTrigger(ROO_JSF_MANAGED_BEAN);
        getConfigurableMetadataProvider()
                .removeMetadataTrigger(ROO_JSF_MANAGED_BEAN);
    }

    /**
     * Returns the additions to make to the generated ITD in order to invoke the
     * various CRUD methods of the given entity
     * 
     * @param entity the target entity type (required)
     * @param metadataIdentificationString the ID of the metadata that's being
     *            created (required)
     * @return a non-<code>null</code> map (may be empty if the CRUD methods are
     *         indeterminable)
     */
    private Map<MethodMetadataCustomDataKey, MemberTypeAdditions> getCrudAdditions(
            final JavaType entity, final String metadataIdentificationString) {
    	
    	if(layerService == null){
    		layerService = getLayerService();
    	}
    	Validate.notNull(layerService, "LayerService is required");
    	
        getMetadataDependencyRegistry().registerDependency(
                getTypeLocationService().getPhysicalTypeIdentifier(entity),
                metadataIdentificationString);
        final List<FieldMetadata> idFields = getPersistenceMemberLocator()
                .getIdentifierFields(entity);
        if (idFields.isEmpty()) {
            return Collections.emptyMap();
        }
        final FieldMetadata identifierField = idFields.get(0);
        final JavaType identifierType = getPersistenceMemberLocator()
                .getIdentifierType(entity);
        if (identifierType == null) {
            return Collections.emptyMap();
        }
        getMetadataDependencyRegistry().registerDependency(
                identifierField.getDeclaredByMetadataId(),
                metadataIdentificationString);

        final JavaSymbolName entityName = JavaSymbolName
                .getReservedWordSafeName(entity);
        final MethodParameter entityParameter = new MethodParameter(entity,
                entityName);
        final MethodParameter idParameter = new MethodParameter(identifierType,
                "id");
        final MethodParameter firstResultParameter = new MethodParameter(
                INT_PRIMITIVE, "firstResult");
        final MethodParameter maxResultsParameter = new MethodParameter(
                INT_PRIMITIVE, "sizeNo");

        final Map<MethodMetadataCustomDataKey, MemberTypeAdditions> additions = new HashMap<MethodMetadataCustomDataKey, MemberTypeAdditions>();
        additions.put(COUNT_ALL_METHOD, layerService.getMemberTypeAdditions(
                metadataIdentificationString, COUNT_ALL_METHOD.name(), entity,
                identifierType, LAYER_POSITION));
        additions.put(FIND_ALL_METHOD, layerService.getMemberTypeAdditions(
                metadataIdentificationString, FIND_ALL_METHOD.name(), entity,
                identifierType, LAYER_POSITION));
        additions.put(FIND_ENTRIES_METHOD, layerService.getMemberTypeAdditions(
                metadataIdentificationString, FIND_ENTRIES_METHOD.name(),
                entity, identifierType, LAYER_POSITION, firstResultParameter,
                maxResultsParameter));
        additions.put(FIND_METHOD, layerService.getMemberTypeAdditions(
                metadataIdentificationString, FIND_METHOD.name(), entity,
                identifierType, LAYER_POSITION, idParameter));
        additions.put(MERGE_METHOD, layerService.getMemberTypeAdditions(
                metadataIdentificationString, MERGE_METHOD.name(), entity,
                identifierType, LAYER_POSITION, entityParameter));
        additions.put(PERSIST_METHOD, layerService.getMemberTypeAdditions(
                metadataIdentificationString, PERSIST_METHOD.name(), entity,
                identifierType, LAYER_POSITION, entityParameter));
        additions.put(REMOVE_METHOD, layerService.getMemberTypeAdditions(
                metadataIdentificationString, REMOVE_METHOD.name(), entity,
                identifierType, LAYER_POSITION, entityParameter));
        return additions;
    }

    @Override
    protected String getGovernorPhysicalTypeIdentifier(
            final String metadataIdentificationString) {
        final JavaType javaType = JsfManagedBeanMetadata
                .getJavaType(metadataIdentificationString);
        final LogicalPath path = JsfManagedBeanMetadata
                .getPath(metadataIdentificationString);
        return PhysicalTypeIdentifier.createIdentifier(javaType, path);
    }

    public String getItdUniquenessFilenameSuffix() {
        return "ManagedBean";
    }

    @Override
    protected String getLocalMidToRequest(final ItdTypeDetails itdTypeDetails) {
        // Determine the governor for this ITD, and whether any metadata is even
        // hoping to hear about changes to that JavaType and its ITDs
        final JavaType governor = itdTypeDetails.getName();
        final String localMid = entityToManagedBeanMidMap.get(governor);
        if (localMid != null) {
            return localMid;
        }

        final MemberHoldingTypeDetails memberHoldingTypeDetails = getTypeLocationService()
                .getTypeDetails(governor);
        if (memberHoldingTypeDetails != null) {
            for (final JavaType type : memberHoldingTypeDetails
                    .getLayerEntities()) {
                final String localMidType = entityToManagedBeanMidMap.get(type);
                if (localMidType != null) {
                    return localMidType;
                }
            }
        }
        return null;
    }

    @Override
    protected ItdTypeDetailsProvidingMetadataItem getMetadata(
            final String metadataIdentificationString,
            final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final String itdFilename) {
        // We need to parse the annotation, which we expect to be present
        final JsfManagedBeanAnnotationValues annotationValues = new JsfManagedBeanAnnotationValues(
                governorPhysicalTypeMetadata);
        final JavaType entity = annotationValues.getEntity();
        if (!annotationValues.isAnnotationFound() || entity == null) {
            return null;
        }

        final MemberDetails memberDetails = getMemberDetails(entity);
        if (memberDetails == null) {
            return null;
        }

        final MethodMetadata identifierAccessor = getPersistenceMemberLocator()
                .getIdentifierAccessor(entity);
        final MethodMetadata versionAccessor = getPersistenceMemberLocator()
                .getVersionAccessor(entity);
        final Set<FieldMetadata> locatedFields = locateFields(entity,
                memberDetails, metadataIdentificationString,
                identifierAccessor, versionAccessor);

        // Remember that this entity JavaType matches up with this metadata
        // identification string
        // Start by clearing any previous association
        final JavaType oldEntity = managedBeanMidToEntityMap
                .get(metadataIdentificationString);
        if (oldEntity != null) {
            entityToManagedBeanMidMap.remove(oldEntity);
        }
        entityToManagedBeanMidMap.put(entity, metadataIdentificationString);
        managedBeanMidToEntityMap.put(metadataIdentificationString, entity);

        final String physicalTypeIdentifier = getTypeLocationService()
                .getPhysicalTypeIdentifier(entity);
        final LogicalPath path = PhysicalTypeIdentifier
                .getPath(physicalTypeIdentifier);
        final PluralMetadata pluralMetadata = (PluralMetadata) getMetadataService()
                .get(PluralMetadata.createIdentifier(entity, path));
        Validate.notNull(pluralMetadata, "Could not determine plural for '%s'",
                entity.getSimpleTypeName());
        final String plural = pluralMetadata.getPlural();

        final Map<MethodMetadataCustomDataKey, MemberTypeAdditions> crudAdditions = getCrudAdditions(
                entity, metadataIdentificationString);

        return new JsfManagedBeanMetadata(metadataIdentificationString,
                aspectName, governorPhysicalTypeMetadata, annotationValues,
                plural, crudAdditions, locatedFields, identifierAccessor);
    }

    public String getProvidesType() {
        return JsfManagedBeanMetadata.getMetadataIdentiferType();
    }

    private boolean isEnum(final ClassOrInterfaceTypeDetails cid) {
        return cid != null
                && cid.getPhysicalTypeCategory() == PhysicalTypeCategory.ENUMERATION;
    }

    private boolean isFieldOfInterest(final FieldMetadata field) {
        final JavaType fieldType = field.getFieldType();
        return !fieldType.isCommonCollectionType()
                && !fieldType.isArray() // Exclude collections and arrays
                && !fieldType.equals(BOOLEAN_PRIMITIVE)
                // Boolean values would not be meaningful in this presentation
                && !fieldType.equals(BOOLEAN_OBJECT)
                && !fieldType.equals(BYTE_ARRAY_PRIMITIVE)
                // Not interested in embedded types
                && !field.getCustomData().keySet().contains(EMBEDDED_FIELD);
    }

    /**
     * Returns an iterable collection of the given entity's fields excluding any
     * ID or version field; along the way, flags the first
     * {@value #MAX_LIST_VIEW_FIELDS} non ID/version fields as being displayable
     * in the list view for this entity type.
     * 
     * @param entity the entity for which to find the fields and accessors
     *            (required)
     * @param memberDetails the entity's members (required)
     * @param metadataIdentificationString the ID of the metadata being
     *            generated (required)
     * @param versionAccessor
     * @param identifierAccessor
     * @return a non-<code>null</code> iterable collection
     */
    private Set<FieldMetadata> locateFields(final JavaType entity,
            final MemberDetails memberDetails,
            final String metadataIdentificationString,
            final MethodMetadata identifierAccessor,
            final MethodMetadata versionAccessor) {
        final Set<FieldMetadata> locatedFields = new LinkedHashSet<FieldMetadata>();
        final Set<ClassOrInterfaceTypeDetails> managedBeanTypes = getTypeLocationService()
                .findClassesOrInterfaceDetailsWithAnnotation(ROO_JSF_MANAGED_BEAN);

        int listViewFields = 0;
        for (final MethodMetadata method : memberDetails.getMethods()) {
            if (!BeanInfoUtils.isAccessorMethod(method)) {
                continue;
            }
            if (method.hasSameName(identifierAccessor, versionAccessor)) {
                continue;
            }
            final FieldMetadata field = BeanInfoUtils.getFieldForPropertyName(
                    memberDetails,
                    BeanInfoUtils.getPropertyNameForJavaBeanMethod(method));
            if (field == null) {
                continue;
            }
            getMetadataDependencyRegistry().registerDependency(
                    field.getDeclaredByMetadataId(),
                    metadataIdentificationString);

            final CustomDataBuilder customDataBuilder = new CustomDataBuilder(
                    field.getCustomData());
            final JavaType fieldType = field.getFieldType();
            if (fieldType.equals(DATE)
                    && field.getFieldName().getSymbolName().equals("created")) {
                continue;
            }

            final ClassOrInterfaceTypeDetails fieldTypeCid = getTypeLocationService()
                    .getTypeDetails(fieldType);

            // Check field is to be displayed in the entity's list view
            if (listViewFields < MAX_LIST_VIEW_FIELDS
                    && isFieldOfInterest(field) && fieldTypeCid == null) {
                listViewFields++;
                customDataBuilder.put(LIST_VIEW_FIELD_KEY, field);
            }

            final boolean enumerated = field.getCustomData().keySet()
                    .contains(CustomDataKeys.ENUMERATED_FIELD)
                    || isEnum(fieldTypeCid);
            if (enumerated) {
                customDataBuilder.put(ENUMERATED_KEY, null);
            }
            else {
                if (fieldType.isCommonCollectionType()) {
                    parameterTypeLoop: for (final JavaType parameter : fieldType
                            .getParameters()) {
                        final ClassOrInterfaceTypeDetails parameterTypeCid = getTypeLocationService()
                                .getTypeDetails(parameter);
                        if (parameterTypeCid == null) {
                            continue;
                        }

                        for (final ClassOrInterfaceTypeDetails managedBeanType : managedBeanTypes) {
                            final AnnotationMetadata managedBeanAnnotation = managedBeanType
                                    .getAnnotation(ROO_JSF_MANAGED_BEAN);
                            if (((JavaType) managedBeanAnnotation.getAttribute(
                                    "entity").getValue()).equals(parameter)) {
                                customDataBuilder.put(PARAMETER_TYPE_KEY,
                                        parameter);
                                customDataBuilder.put(
                                        PARAMETER_TYPE_MANAGED_BEAN_NAME_KEY,
                                        managedBeanAnnotation.getAttribute(
                                                "beanName").getValue());

                                final LogicalPath logicalPath = PhysicalTypeIdentifier
                                        .getPath(parameterTypeCid
                                                .getDeclaredByMetadataId());
                                final PluralMetadata pluralMetadata = (PluralMetadata) getMetadataService()
                                        .get(PluralMetadata.createIdentifier(
                                                parameter, logicalPath));
                                if (pluralMetadata != null) {
                                    customDataBuilder.put(
                                            PARAMETER_TYPE_PLURAL_KEY,
                                            pluralMetadata.getPlural());
                                }
                                // Only support one generic type parameter
                                break parameterTypeLoop;
                            }
                            // Parameter type is not an entity - test for an
                            // enum
                            if (isEnum(parameterTypeCid)) {
                                customDataBuilder.put(PARAMETER_TYPE_KEY,
                                        parameter);
                            }
                        }
                    }
                }
                else {
                    if (fieldTypeCid != null
                            && !customDataBuilder.keySet().contains(
                                    CustomDataKeys.EMBEDDED_FIELD)) {
                        customDataBuilder.put(APPLICATION_TYPE_KEY, null);
                        final MethodMetadata applicationTypeIdentifierAccessor = getPersistenceMemberLocator()
                                .getIdentifierAccessor(entity);
                        final MethodMetadata applicationTypeVersionAccessor = getPersistenceMemberLocator()
                                .getVersionAccessor(entity);
                        final List<FieldMetadata> applicationTypeFields = new ArrayList<FieldMetadata>();

                        int dropDownFields = 0;
                        final MemberDetails applicationTypeMemberDetails = getMemberDetails(fieldType);
                        for (final MethodMetadata applicationTypeMethod : applicationTypeMemberDetails
                                .getMethods()) {
                            if (!BeanInfoUtils
                                    .isAccessorMethod(applicationTypeMethod)) {
                                continue;
                            }
                            if (applicationTypeMethod.hasSameName(
                                    applicationTypeIdentifierAccessor,
                                    applicationTypeVersionAccessor)) {
                                continue;
                            }
                            final FieldMetadata applicationTypeField = BeanInfoUtils
                                    .getFieldForJavaBeanMethod(
                                            applicationTypeMemberDetails,
                                            applicationTypeMethod);
                            if (applicationTypeField == null) {
                                continue;
                            }
                            if (dropDownFields < MAX_DROP_DOWN_FIELDS
                                    && isFieldOfInterest(applicationTypeField)
                                    && !getTypeLocationService()
                                            .isInProject(applicationTypeField
                                                    .getFieldType())) {
                                dropDownFields++;
                                applicationTypeFields.add(applicationTypeField);
                            }
                        }
                        if (applicationTypeFields.isEmpty()) {
                            applicationTypeFields.add(BeanInfoUtils
                                    .getFieldForJavaBeanMethod(
                                            applicationTypeMemberDetails,
                                            applicationTypeIdentifierAccessor));
                        }
                        customDataBuilder.put(APPLICATION_TYPE_FIELDS_KEY,
                                applicationTypeFields);
                        customDataBuilder.put(
                                CRUD_ADDITIONS_KEY,
                                getCrudAdditions(fieldType,
                                        metadataIdentificationString));
                    }
                }
            }

            final FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(
                    field);
            fieldBuilder.setCustomData(customDataBuilder);
            locatedFields.add(fieldBuilder.build());
        }

        return locatedFields;
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
    			LOGGER.warning("Cannot load ConfigurableMetadataProvider on JsfManagedBeanMetadataProviderImpl.");
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
			LOGGER.warning("Cannot load LayerService on JsfManagedBeanMetadataProviderImpl.");
			return null;
		}
    }
}