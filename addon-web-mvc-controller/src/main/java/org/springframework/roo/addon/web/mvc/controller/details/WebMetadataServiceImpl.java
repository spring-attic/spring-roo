package org.springframework.roo.addon.web.mvc.controller.details;

import static org.springframework.roo.classpath.customdata.CustomDataKeys.COUNT_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_ENTRIES_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_ALL_SORTED_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_ENTRIES_SORTED_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.IDENTIFIER_ACCESSOR_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.IDENTIFIER_TYPE;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.MERGE_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.PERSIST_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.REMOVE_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.VERSION_ACCESSOR_METHOD;
import static org.springframework.roo.model.Jsr303JavaType.NOT_NULL;
import static org.springframework.roo.model.RooJavaType.ROO_WEB_SCAFFOLD;
import static org.springframework.roo.model.SpringJavaType.DATE_TIME_FORMAT;

import java.beans.Introspector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.finder.FinderMetadata;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.addon.web.mvc.controller.scaffold.WebScaffoldMetadata;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.customdata.tagkeys.MethodMetadataCustomDataKey;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.layers.LayerService;
import org.springframework.roo.classpath.layers.LayerType;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.classpath.layers.MethodParameter;
import org.springframework.roo.classpath.persistence.PersistenceMemberLocator;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JdkJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.CollectionUtils;

/**
 * Implementation of {@link WebMetadataService} to retrieve various metadata
 * information for use by Web scaffolding add-ons.
 * 
 * @author Stefan Schmidt
 * @since 1.1.2
 */
@Component
@Service
public class WebMetadataServiceImpl implements WebMetadataService {

    private static final MethodParameter FIRST_RESULT_PARAMETER = new MethodParameter(
            JavaType.INT_PRIMITIVE, "firstResult");
    private static final int LAYER_POSITION = LayerType.HIGHEST.getPosition();
    private static final Logger LOGGER = HandlerUtils
            .getLogger(WebMetadataServiceImpl.class);
    private static final MethodParameter MAX_RESULTS_PARAMETER = new MethodParameter(
            JavaType.INT_PRIMITIVE, "sizeNo");
    private static final MethodParameter SORT_FIELDNAME_PARAMETER = new MethodParameter(
            JavaType.STRING, "sortFieldName");
    private static final MethodParameter SORT_ORDER_PARAMETER = new MethodParameter(
            JavaType.STRING, "sortOrder");
    
    @Reference private LayerService layerService;
    @Reference private MemberDetailsScanner memberDetailsScanner;
    @Reference private MetadataDependencyRegistry metadataDependencyRegistry;
    @Reference private MetadataService metadataService;
    @Reference private PersistenceMemberLocator persistenceMemberLocator;
    @Reference private TypeLocationService typeLocationService;

    private final Map<String, String> pathMap = new HashMap<String, String>();

    private String getControllerPathForType(final JavaType type,
            final String metadataIdentificationString) {
        if (pathMap.containsKey(type.getFullyQualifiedTypeName())
                && !typeLocationService.hasTypeChanged(getClass().getName(),
                        type)) {
            return pathMap.get(type.getFullyQualifiedTypeName());
        }
        String webScaffoldMetadataKey = null;
        WebScaffoldMetadata webScaffoldMetadata = null;
        for (final ClassOrInterfaceTypeDetails cid : typeLocationService
                .findClassesOrInterfaceDetailsWithAnnotation(ROO_WEB_SCAFFOLD)) {
            for (final AnnotationMetadata annotation : cid.getAnnotations()) {
                if (annotation.getAnnotationType().equals(ROO_WEB_SCAFFOLD)) {
                    final AnnotationAttributeValue<?> formBackingObject = annotation
                            .getAttribute(new JavaSymbolName(
                                    "formBackingObject"));
                    if (formBackingObject instanceof ClassAttributeValue) {
                        final ClassAttributeValue formBackingObjectValue = (ClassAttributeValue) formBackingObject;
                        if (formBackingObjectValue.getValue().equals(type)) {
                            final AnnotationAttributeValue<String> path = annotation
                                    .getAttribute("path");
                            if (path != null) {
                                final String pathString = path.getValue();
                                pathMap.put(type.getFullyQualifiedTypeName(),
                                        pathString);
                                return pathString;
                            }
                            final LogicalPath cidPath = PhysicalTypeIdentifier
                                    .getPath(cid.getDeclaredByMetadataId());
                            webScaffoldMetadataKey = WebScaffoldMetadata
                                    .createIdentifier(cid.getName(), cidPath);
                            webScaffoldMetadata = (WebScaffoldMetadata) metadataService
                                    .get(webScaffoldMetadataKey);
                            break;
                        }
                    }
                }
            }
        }
        if (webScaffoldMetadata != null) {
            registerDependency(webScaffoldMetadataKey,
                    metadataIdentificationString);
            final String path = webScaffoldMetadata.getAnnotationValues()
                    .getPath();
            pathMap.put(type.getFullyQualifiedTypeName(), path);
            return path;
        }
        return getPlural(type, metadataIdentificationString).toLowerCase();
    }

    public Map<MethodMetadataCustomDataKey, MemberTypeAdditions> getCrudAdditions(
            final JavaType domainType, final String metadataId) {
        final String domainTypeMid = typeLocationService
                .getPhysicalTypeIdentifier(domainType);
        if (domainTypeMid != null) {
            metadataDependencyRegistry.registerDependency(domainTypeMid,
                    metadataId);
        }

        final JavaTypePersistenceMetadataDetails persistenceDetails = getJavaTypePersistenceMetadataDetails(
                domainType, getMemberDetails(domainType), metadataId);
        if (persistenceDetails == null) {
            return Collections.emptyMap();
        }
        final Map<MethodMetadataCustomDataKey, MemberTypeAdditions> additions = new HashMap<MethodMetadataCustomDataKey, MemberTypeAdditions>();
        additions.put(COUNT_ALL_METHOD, persistenceDetails.getCountMethod());
        additions.put(REMOVE_METHOD, persistenceDetails.getRemoveMethod());
        additions.put(FIND_METHOD, persistenceDetails.getFindMethod());
        additions.put(FIND_ALL_METHOD, persistenceDetails.getFindAllMethod());
        additions.put(FIND_ENTRIES_METHOD,
                persistenceDetails.getFindEntriesMethod());
        additions.put(FIND_ALL_SORTED_METHOD, persistenceDetails.getFindAllSortedMethod());
        additions.put(FIND_ENTRIES_SORTED_METHOD,
                persistenceDetails.getFindEntriesSortedMethod());
        additions.put(MERGE_METHOD, persistenceDetails.getMergeMethod());
        additions.put(PERSIST_METHOD, persistenceDetails.getPersistMethod());
        return additions;
    }

    public Map<JavaSymbolName, DateTimeFormatDetails> getDatePatterns(
            final JavaType javaType, final MemberDetails memberDetails,
            final String metadataIdentificationString) {
        Validate.notNull(javaType, "Java type required");
        Validate.notNull(memberDetails, "Member details required");

        final MethodMetadata identifierAccessor = persistenceMemberLocator
                .getIdentifierAccessor(javaType);
        final MethodMetadata versionAccessor = persistenceMemberLocator
                .getVersionAccessor(javaType);

        final Map<JavaSymbolName, DateTimeFormatDetails> dates = new LinkedHashMap<JavaSymbolName, DateTimeFormatDetails>();
        final JavaTypePersistenceMetadataDetails javaTypePersistenceMetadataDetails = getJavaTypePersistenceMetadataDetails(
                javaType, memberDetails, metadataIdentificationString);

        for (final MethodMetadata method : memberDetails.getMethods()) {
            // Only interested in accessors
            if (!BeanInfoUtils.isAccessorMethod(method)) {
                continue;
            }
            // Not interested in fields that are not exposed via a mutator and
            // accessor and in identifiers and version fields
            if (method.hasSameName(identifierAccessor, versionAccessor)) {
                continue;
            }
            final FieldMetadata field = BeanInfoUtils
                    .getFieldForJavaBeanMethod(memberDetails, method);
            if (field == null
                    || !BeanInfoUtils.hasAccessorAndMutator(field,
                            memberDetails)) {
                continue;
            }
            final JavaType returnType = method.getReturnType();
            if (!JdkJavaType.isDateField(returnType)) {
                continue;
            }
            final AnnotationMetadata annotation = MemberFindingUtils
                    .getAnnotationOfType(field.getAnnotations(),
                            DATE_TIME_FORMAT);
            final JavaSymbolName patternSymbol = new JavaSymbolName("pattern");
            final JavaSymbolName styleSymbol = new JavaSymbolName("style");
            DateTimeFormatDetails dateTimeFormat = null;
            if (annotation != null) {
                if (annotation.getAttributeNames().contains(styleSymbol)) {
                    dateTimeFormat = DateTimeFormatDetails.withStyle(annotation
                            .getAttribute(styleSymbol).getValue().toString());
                }
                else if (annotation.getAttributeNames().contains(patternSymbol)) {
                    dateTimeFormat = DateTimeFormatDetails
                            .withPattern(annotation.getAttribute(patternSymbol)
                                    .getValue().toString());
                }
            }
            if (dateTimeFormat != null) {
                registerDependency(field.getDeclaredByMetadataId(),
                        metadataIdentificationString);
                dates.put(field.getFieldName(), dateTimeFormat);
                if (javaTypePersistenceMetadataDetails != null) {
                    for (final String finder : javaTypePersistenceMetadataDetails
                            .getFinderNames()) {
                        if (finder.contains(StringUtils.capitalize(field
                                .getFieldName().getSymbolName()) + "Between")) {
                            dates.put(
                                    new JavaSymbolName("min"
                                            + StringUtils.capitalize(field
                                                    .getFieldName()
                                                    .getSymbolName())),
                                    dateTimeFormat);
                            dates.put(
                                    new JavaSymbolName("max"
                                            + StringUtils.capitalize(field
                                                    .getFieldName()
                                                    .getSymbolName())),
                                    dateTimeFormat);
                        }
                    }
                }
            }
            else {
                LOGGER.warning("It is recommended to use @DateTimeFormat(style=\"M-\") on "
                        + field.getFieldType().getFullyQualifiedTypeName()
                        + "."
                        + field.getFieldName()
                        + " to use automatic date conversion in Spring MVC");
            }
        }
        return Collections.unmodifiableMap(dates);
    }

    public List<JavaTypeMetadataDetails> getDependentApplicationTypeMetadata(
            final JavaType javaType, final MemberDetails memberDetails,
            final String metadataIdentificationString) {
        Validate.notNull(javaType, "Java type required");
        Validate.notNull(memberDetails, "Member details required");

        final List<JavaTypeMetadataDetails> dependentTypes = new ArrayList<JavaTypeMetadataDetails>();
        for (final MethodMetadata method : memberDetails.getMethods()) {
            final JavaType type = method.getReturnType();
            if (BeanInfoUtils.isAccessorMethod(method)
                    && isApplicationType(type)) {
                final FieldMetadata field = BeanInfoUtils
                        .getFieldForJavaBeanMethod(memberDetails, method);
                if (field != null
                        && MemberFindingUtils.getAnnotationOfType(
                                field.getAnnotations(), NOT_NULL) != null) {
                    final MemberDetails typeMemberDetails = getMemberDetails(type);
                    if (getJavaTypePersistenceMetadataDetails(type,
                            typeMemberDetails, metadataIdentificationString) != null) {
                        dependentTypes
                                .add(getJavaTypeMetadataDetails(type,
                                        typeMemberDetails,
                                        metadataIdentificationString));
                    }
                }
            }
        }
        return Collections.unmodifiableList(dependentTypes);
    }

    public Set<FinderMetadataDetails> getDynamicFinderMethodsAndFields(
            final JavaType formBackingType,
            final MemberDetails formBackingTypeDetails,
            final String metadataIdentificationString) {
        Validate.notNull(formBackingType, "Java type required");
        Validate.notNull(formBackingTypeDetails, "Member details required");

        final ClassOrInterfaceTypeDetails javaTypeDetails = typeLocationService
                .getTypeDetails(formBackingType);
        Validate.notNull(
                formBackingType,
                "Class or interface type details isn't available for type '%s'",
                formBackingType);
        final LogicalPath logicalPath = PhysicalTypeIdentifier
                .getPath(javaTypeDetails.getDeclaredByMetadataId());
        final String finderMetadataKey = FinderMetadata.createIdentifier(
                formBackingType, logicalPath);
        registerDependency(finderMetadataKey, metadataIdentificationString);
        final FinderMetadata finderMetadata = (FinderMetadata) metadataService
                .get(finderMetadataKey);
        if (finderMetadata == null) {
            return null;
        }
        final SortedSet<FinderMetadataDetails> finderMetadataDetails = new TreeSet<FinderMetadataDetails>();
        for (final MethodMetadata method : finderMetadata
                .getAllDynamicFinders()) {
            final List<JavaSymbolName> parameterNames = method
                    .getParameterNames();
            final List<JavaType> parameterTypes = AnnotatedJavaType
                    .convertFromAnnotatedJavaTypes(method.getParameterTypes());
            final List<FieldMetadata> fields = new ArrayList<FieldMetadata>();
            for (int i = 0; i < parameterTypes.size(); i++) {
                JavaSymbolName fieldName = null;
                if (parameterNames.get(i).getSymbolName().startsWith("max")
                        || parameterNames.get(i).getSymbolName()
                                .startsWith("min")) {
                    fieldName = new JavaSymbolName(
                            Introspector.decapitalize(StringUtils
                                    .capitalize(parameterNames.get(i)
                                            .getSymbolName().substring(3))));
                }
                else {
                    fieldName = parameterNames.get(i);
                }
                final FieldMetadata field = BeanInfoUtils
                        .getFieldForPropertyName(formBackingTypeDetails,
                                fieldName);
                if (field != null) {
                    final FieldMetadataBuilder fieldMd = new FieldMetadataBuilder(
                            field);
                    fieldMd.setFieldName(parameterNames.get(i));
                    fields.add(fieldMd.build());
                }
            }
            final FinderMetadataDetails details = new FinderMetadataDetails(
                    method.getMethodName().getSymbolName(), method, fields);
            finderMetadataDetails.add(details);
        }
        
        SortedSet<FinderMetadataDetails> finderMetadataDetailsWoCountMethods = new TreeSet<FinderMetadataDetails>();
        for(FinderMetadataDetails dynamicFinderMethod : finderMetadataDetails) {
        	if(!dynamicFinderMethod.getFinderName().startsWith("count")) {
        		finderMetadataDetailsWoCountMethods.add(dynamicFinderMethod);
        	}
        }
        
        return Collections.unmodifiableSortedSet(finderMetadataDetailsWoCountMethods);
    }

    public FieldMetadata getIdentifierField(final JavaType javaType) {
        return CollectionUtils.firstElementOf(persistenceMemberLocator
                .getIdentifierFields(javaType));
    }

    public JavaTypeMetadataDetails getJavaTypeMetadataDetails(
            final JavaType javaType, final MemberDetails memberDetails,
            final String metadataIdentificationString) {
        Validate.notNull(javaType, "Java type required");
        registerDependency(
                memberDetails.getDetails()
                        .get(memberDetails.getDetails().size() - 1)
                        .getDeclaredByMetadataId(),
                metadataIdentificationString);
        return new JavaTypeMetadataDetails(
                javaType,
                getPlural(javaType, metadataIdentificationString),
                isEnumType(javaType),
                isApplicationType(javaType),
                getJavaTypePersistenceMetadataDetails(javaType, memberDetails,
                        metadataIdentificationString),
                getControllerPathForType(javaType, metadataIdentificationString));
    }

    public JavaTypePersistenceMetadataDetails getJavaTypePersistenceMetadataDetails(
            final JavaType javaType, final MemberDetails memberDetails,
            final String metadataIdentificationString) {
        Validate.notNull(javaType, "Java type required");
        Validate.notNull(memberDetails, "Member details required");
        Validate.notBlank(metadataIdentificationString, "Metadata id required");

        final MethodMetadata idAccessor = memberDetails
                .getMostConcreteMethodWithTag(IDENTIFIER_ACCESSOR_METHOD);
        if (idAccessor == null) {
            return null;
        }

        final FieldMetadata idField = CollectionUtils
                .firstElementOf(persistenceMemberLocator
                        .getIdentifierFields(javaType));
        if (idField == null) {
            return null;
        }

        final JavaType idType = persistenceMemberLocator
                .getIdentifierType(javaType);
        if (idType == null) {
            return null;
        }

        registerDependency(idAccessor.getDeclaredByMetadataId(),
                metadataIdentificationString);
        registerDependency(idField.getDeclaredByMetadataId(),
                metadataIdentificationString);

        final MethodParameter entityParameter = new MethodParameter(javaType,
                JavaSymbolName.getReservedWordSafeName(javaType));
        final MethodParameter idParameter = new MethodParameter(idType, idField
                .getFieldName().getSymbolName());
        final MethodMetadata versionAccessor = memberDetails
                .getMostConcreteMethodWithTag(VERSION_ACCESSOR_METHOD);
        final MemberTypeAdditions persistMethod = layerService
                .getMemberTypeAdditions(metadataIdentificationString,
                        PERSIST_METHOD.name(), javaType, idType,
                        LAYER_POSITION, entityParameter);
        final MemberTypeAdditions removeMethod = layerService
                .getMemberTypeAdditions(metadataIdentificationString,
                        REMOVE_METHOD.name(), javaType, idType, LAYER_POSITION,
                        entityParameter);
        final MemberTypeAdditions mergeMethod = layerService
                .getMemberTypeAdditions(metadataIdentificationString,
                        MERGE_METHOD.name(), javaType, idType, LAYER_POSITION,
                        entityParameter);
        final MemberTypeAdditions findAllMethod = layerService
                .getMemberTypeAdditions(metadataIdentificationString,
                        FIND_ALL_METHOD.name(), javaType, idType,
                        LAYER_POSITION);
        final MemberTypeAdditions findAllSortedMethod = layerService
                .getMemberTypeAdditions(metadataIdentificationString,
                        FIND_ALL_SORTED_METHOD.name(), javaType, idType,
                        LAYER_POSITION, SORT_FIELDNAME_PARAMETER,
                        SORT_ORDER_PARAMETER);
        final MemberTypeAdditions findMethod = layerService
                .getMemberTypeAdditions(metadataIdentificationString,
                        FIND_METHOD.name(), javaType, idType, LAYER_POSITION,
                        idParameter);
        final MemberTypeAdditions countMethod = layerService
                .getMemberTypeAdditions(metadataIdentificationString,
                        COUNT_ALL_METHOD.name(), javaType, idType,
                        LAYER_POSITION);
        final MemberTypeAdditions findEntriesMethod = layerService
                .getMemberTypeAdditions(metadataIdentificationString,
                        FIND_ENTRIES_METHOD.name(), javaType, idType,
                        LAYER_POSITION, FIRST_RESULT_PARAMETER,
                        MAX_RESULTS_PARAMETER);
        final MemberTypeAdditions findEntriesSortedMethod = layerService
                .getMemberTypeAdditions(metadataIdentificationString,
                        FIND_ENTRIES_SORTED_METHOD.name(), javaType, idType,
                        LAYER_POSITION, FIRST_RESULT_PARAMETER,
                        MAX_RESULTS_PARAMETER, SORT_FIELDNAME_PARAMETER,
                        SORT_ORDER_PARAMETER);
        
        final List<String> dynamicFinderNames = memberDetails
                .getDynamicFinderNames();

        return new JavaTypePersistenceMetadataDetails(idType, idField,
                idAccessor, versionAccessor, persistMethod, mergeMethod,
                removeMethod, findAllMethod, findAllSortedMethod, findMethod, countMethod,
                findEntriesMethod, findEntriesSortedMethod, dynamicFinderNames, isRooIdentifier(
                        javaType, memberDetails),
                persistenceMemberLocator.getEmbeddedIdentifierFields(javaType));
    }

    public MemberDetails getMemberDetails(final JavaType javaType) {
        final ClassOrInterfaceTypeDetails cid = typeLocationService
                .getTypeDetails(javaType);
        Validate.notNull(cid,
                "Unable to obtain physical type metadata for type %s",
                javaType.getFullyQualifiedTypeName());
        return memberDetailsScanner.getMemberDetails(
                WebMetadataServiceImpl.class.getName(), cid);
    }

    private String getPlural(final JavaType javaType,
            final String metadataIdentificationString) {
        Validate.notNull(javaType, "Java type required");
        Validate.notNull(metadataService, "Metadata service required");

        final ClassOrInterfaceTypeDetails javaTypeDetails = typeLocationService
                .getTypeDetails(javaType);
        Validate.notNull(
                javaTypeDetails,
                "Class or interface type details isn't available for type '%s'",
                javaType);
        final LogicalPath logicalPath = PhysicalTypeIdentifier
                .getPath(javaTypeDetails.getDeclaredByMetadataId());
        final String pluralMetadataKey = PluralMetadata.createIdentifier(
                javaType, logicalPath);
        final PluralMetadata pluralMetadata = (PluralMetadata) metadataService
                .get(pluralMetadataKey);
        if (pluralMetadata != null) {
            registerDependency(pluralMetadata.getId(),
                    metadataIdentificationString);
            final String plural = pluralMetadata.getPlural();
            if (plural.equalsIgnoreCase(javaType.getSimpleTypeName())) {
                return plural + "Items";
            }
            else {
                return plural;
            }
        }
        return javaType.getSimpleTypeName() + "s";
    }

    public SortedMap<JavaType, JavaTypeMetadataDetails> getRelatedApplicationTypeMetadata(
            final JavaType baseType, final MemberDetails baseTypeDetails,
            final String metadataIdentificationString) {
        Validate.notNull(baseType, "Java type required");
        Validate.notNull(baseTypeDetails, "Member details required");
        Validate.isTrue(isApplicationType(baseType),
                "The type %s does not belong to this application", baseType);

        final SortedMap<JavaType, JavaTypeMetadataDetails> specialTypes = new TreeMap<JavaType, JavaTypeMetadataDetails>();
        specialTypes.put(
                baseType,
                getJavaTypeMetadataDetails(baseType, baseTypeDetails,
                        metadataIdentificationString));

        for (final JavaType fieldType : baseTypeDetails
                .getPersistentFieldTypes(baseType, persistenceMemberLocator)) {
            if (isApplicationType(fieldType)) {
                final MemberDetails fieldTypeDetails = getMemberDetails(fieldType);
                specialTypes.put(
                        fieldType,
                        getJavaTypeMetadataDetails(fieldType, fieldTypeDetails,
                                metadataIdentificationString));
            }
        }

        return specialTypes;
    }

    public List<FieldMetadata> getScaffoldEligibleFieldMetadata(
            final JavaType javaType, final MemberDetails memberDetails,
            final String metadataIdentificationString) {
        Validate.notNull(javaType, "Java type required");
        Validate.notNull(memberDetails, "Member details required");

        final MethodMetadata identifierAccessor = persistenceMemberLocator
                .getIdentifierAccessor(javaType);
        final MethodMetadata versionAccessor = persistenceMemberLocator
                .getVersionAccessor(javaType);

        final Map<JavaSymbolName, FieldMetadata> fields = new LinkedHashMap<JavaSymbolName, FieldMetadata>();
        final List<MethodMetadata> methods = memberDetails.getMethods();

        for (final MethodMetadata method : methods) {
            // Only interested in accessors
            if (!BeanInfoUtils.isAccessorMethod(method)
                    || method.hasSameName(identifierAccessor, versionAccessor)) {
                continue;
            }

            final FieldMetadata field = BeanInfoUtils
                    .getFieldForJavaBeanMethod(memberDetails, method);
            if (field == null
                    || !BeanInfoUtils.hasAccessorAndMutator(field,
                            memberDetails)) {
                continue;
            }
            final JavaSymbolName fieldName = field.getFieldName();
            registerDependency(method.getDeclaredByMetadataId(),
                    metadataIdentificationString);
            if (!fields.containsKey(fieldName)) {
                fields.put(fieldName, field);
            }
        }
        return Collections.unmodifiableList(new ArrayList<FieldMetadata>(fields
                .values()));
    }

    public boolean isApplicationType(final JavaType javaType) {
        return typeLocationService.isInProject(javaType);
    }

    private boolean isEnumType(final JavaType javaType) {
        Validate.notNull(javaType, "Java type required");
        Validate.notNull(metadataService, "Metadata service required");
        final ClassOrInterfaceTypeDetails javaTypeDetails = typeLocationService
                .getTypeDetails(javaType);
        if (javaTypeDetails != null) {
            if (javaTypeDetails.getPhysicalTypeCategory().equals(
                    PhysicalTypeCategory.ENUMERATION)) {
                return true;
            }
        }
        return false;
    }

    public boolean isRooIdentifier(final JavaType javaType,
            final MemberDetails memberDetails) {
        Validate.notNull(javaType, "Java type required");
        Validate.notNull(memberDetails, "Member details required");
        return MemberFindingUtils.getMemberHoldingTypeDetailsWithTag(
                memberDetails, IDENTIFIER_TYPE).size() > 0;
    }

    private void registerDependency(final String upstreamDependency,
            final String downStreamDependency) {
        if (metadataDependencyRegistry != null
                && StringUtils.isNotBlank(upstreamDependency)
                && StringUtils.isNotBlank(downStreamDependency)
                && !upstreamDependency.equals(downStreamDependency)
                && !MetadataIdentificationUtils.getMetadataClass(
                        downStreamDependency).equals(
                        MetadataIdentificationUtils
                                .getMetadataClass(upstreamDependency))) {
            metadataDependencyRegistry.registerDependency(upstreamDependency,
                    downStreamDependency);
        }
    }
}
