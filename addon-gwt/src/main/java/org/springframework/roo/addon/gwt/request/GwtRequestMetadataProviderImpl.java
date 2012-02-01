package org.springframework.roo.addon.gwt.request;

import static java.lang.reflect.Modifier.ABSTRACT;
import static java.lang.reflect.Modifier.STATIC;
import static org.springframework.roo.addon.gwt.GwtJavaType.INSTANCE_REQUEST;
import static org.springframework.roo.addon.gwt.GwtJavaType.OLD_REQUEST_CONTEXT;
import static org.springframework.roo.addon.gwt.GwtJavaType.REQUEST;
import static org.springframework.roo.addon.gwt.GwtJavaType.REQUEST_CONTEXT;
import static org.springframework.roo.addon.gwt.GwtJavaType.SERVICE_NAME;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.COUNT_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_ENTRIES_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.PERSIST_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.REMOVE_METHOD;
import static org.springframework.roo.model.JavaType.INT_PRIMITIVE;
import static org.springframework.roo.model.JavaType.LONG_PRIMITIVE;
import static org.springframework.roo.model.JavaType.VOID_PRIMITIVE;
import static org.springframework.roo.model.RooJavaType.ROO_GWT_REQUEST;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.gwt.GwtFileManager;
import org.springframework.roo.addon.gwt.GwtTypeService;
import org.springframework.roo.addon.gwt.GwtUtils;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.customdata.tagkeys.MethodMetadataCustomDataKey;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.layers.LayerService;
import org.springframework.roo.classpath.layers.LayerType;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.classpath.layers.MethodParameter;
import org.springframework.roo.classpath.persistence.PersistenceMemberLocator;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.AbstractHashCodeTrackingMetadataNotifier;
import org.springframework.roo.metadata.MetadataItem;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;

@Component(immediate = true)
@Service
public class GwtRequestMetadataProviderImpl extends
        AbstractHashCodeTrackingMetadataNotifier implements
        GwtRequestMetadataProvider {

    private static final int LAYER_POSITION = LayerType.HIGHEST.getPosition();

    @Reference GwtFileManager gwtFileManager;
    @Reference GwtTypeService gwtTypeService;
    @Reference LayerService layerService;
    @Reference MemberDetailsScanner memberDetailsScanner;
    @Reference PersistenceMemberLocator persistenceMemberLocator;
    @Reference ProjectOperations projectOperations;
    @Reference TypeLocationService typeLocationService;

    protected void activate(final ComponentContext context) {
        metadataDependencyRegistry.registerDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
    }

    protected void deactivate(final ComponentContext context) {
        metadataDependencyRegistry.deregisterDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
    }

    public MetadataItem get(final String requestMetadataId) {
        final ProjectMetadata projectMetadata = projectOperations
                .getProjectMetadata(PhysicalTypeIdentifierNamingUtils
                        .getModule(requestMetadataId));
        if (projectMetadata == null) {
            return null;
        }

        final ClassOrInterfaceTypeDetails requestInterface = getGovernor(requestMetadataId);
        if (requestInterface == null) {
            return null;
        }

        final AnnotationMetadata gwtRequestAnnotation = requestInterface
                .getAnnotation(ROO_GWT_REQUEST);
        if (gwtRequestAnnotation == null) {
            return null;
        }

        final JavaType entityType = new JavaType((String) gwtRequestAnnotation
                .getAttribute("value").getValue());

        // Get the methods to be invoked and the type(s) that provide them
        // (should only be one such type, or null)
        final Map<MethodMetadata, FieldMetadata> requestMethods = getRequestMethodsAndInvokedTypes(
                entityType, requestMetadataId);
        if (requestMethods == null) {
            return null;
        }

        final JavaType invokedType = getInvokedType(requestMethods.values());
        final String requestTypeContents = writeRequestInterface(
                requestInterface, invokedType, requestMethods.keySet(),
                entityType, requestMetadataId);
        final GwtRequestMetadata gwtRequestMetadata = new GwtRequestMetadata(
                requestMetadataId, requestTypeContents);
        notifyIfRequired(gwtRequestMetadata);
        return gwtRequestMetadata;
    }

    private ClassOrInterfaceTypeDetails getGovernor(
            final String metadataIdentificationString) {
        final JavaType governorTypeName = GwtRequestMetadata
                .getJavaType(metadataIdentificationString);
        final LogicalPath governorTypePath = GwtRequestMetadata
                .getPath(metadataIdentificationString);
        final String physicalTypeId = PhysicalTypeIdentifier.createIdentifier(
                governorTypeName, governorTypePath);
        return typeLocationService.getTypeDetails(physicalTypeId);
    }

    /**
     * Returns the type on which the given request methods will be invoked
     * 
     * @param invokedFields the autowired fields invoked by layer method calls
     *            (can include <code>null</code> elements for 'active record'
     *            calls)
     * @return <code>null</code> if active record is being used, otherwise a
     *         layer component type
     */
    private JavaType getInvokedType(
            final Collection<FieldMetadata> invokedFields) {
        final Collection<JavaType> distinctInvokedTypes = new HashSet<JavaType>();
        for (final FieldMetadata invokedField : invokedFields) {
            if (invokedField == null) {
                distinctInvokedTypes.add(null);
            }
            else {
                distinctInvokedTypes.add(invokedField.getFieldType());
            }
        }
        Validate.isTrue(distinctInvokedTypes.size() == 1,
                "Expected one invoked type but found: " + distinctInvokedTypes);
        return distinctInvokedTypes.iterator().next();
    }

    public String getProvidesType() {
        return GwtRequestMetadata.getMetadataIdentifierType();
    }

    private MethodMetadataBuilder getRequestMethod(
            final ClassOrInterfaceTypeDetails request,
            final MethodMetadata method, final JavaType returnType) {
        final ClassOrInterfaceTypeDetails entity = gwtTypeService
                .lookupEntityFromRequest(request);
        if (entity == null) {
            return null;
        }
        final List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
        for (final AnnotatedJavaType parameterType : method.getParameterTypes()) {
            parameterTypes.add(new AnnotatedJavaType(gwtTypeService
                    .getGwtSideLeafType(parameterType.getJavaType(),
                            entity.getType(), true, false)));
        }
        return new MethodMetadataBuilder(request.getDeclaredByMetadataId(),
                ABSTRACT, method.getMethodName(), returnType, parameterTypes,
                method.getParameterNames(), null);
    }

    private MethodMetadataBuilder getRequestMethod(
            final ClassOrInterfaceTypeDetails request,
            final MethodMetadata method, final JavaType entityType,
            final JavaType invokedType) {
        final ClassOrInterfaceTypeDetails proxy = gwtTypeService
                .lookupProxyFromRequest(request);
        if (proxy == null) {
            return null;
        }
        final JavaType methodReturnType = getRequestMethodReturnType(
                invokedType, method, proxy.getType());
        return getRequestMethod(request, method, methodReturnType);
    }

    private MethodMetadata getRequestMethod(final JavaType entity,
            final MethodMetadataCustomDataKey methodKey,
            final MemberTypeAdditions memberTypeAdditions,
            final String declaredByMetadataId) {
        final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
                declaredByMetadataId); // wrong MID, but doesn't matter here
        methodBuilder.setMethodName(new JavaSymbolName(memberTypeAdditions
                .getMethodName()));
        if (memberTypeAdditions.isStatic()) {
            // OK to overwrite any other modifiers
            methodBuilder.setModifier(STATIC);
        }
        /*
         * TODO make sure the active record instance methods have the correct
         * parameters
         * 
         * expected: abstract
         * InstanceRequest<com.example.gwtbug.client.proxy.ThingProxy,
         * java.lang.Void> persist(); actual: abstract Request<java.lang.Void>
         * persist(ThingProxy proxy);
         */
        for (final MethodParameter methodParameter : memberTypeAdditions
                .getMethodParameters()) {
            methodBuilder.addParameter(methodParameter.getValue()
                    .getSymbolName(), methodParameter.getKey());
        }
        final JavaType returnType = getReturnType(methodKey, entity);
        final JavaType gwtType = gwtTypeService.getGwtSideLeafType(returnType,
                entity, true, true);
        methodBuilder.setReturnType(gwtType);
        return methodBuilder.build();
    }

    private JavaType getRequestMethodReturnType(final JavaType invokedType,
            final MethodMetadata method, final JavaType proxyType) {
        if (invokedType == null && !method.isStatic()) {
            // Calling an active record method that's non-static (i.e. target is
            // an entity instance)
            final List<JavaType> methodReturnTypeArgs = Arrays.asList(
                    proxyType, method.getReturnType());
            return new JavaType(INSTANCE_REQUEST.getFullyQualifiedTypeName(),
                    0, DataType.TYPE, null, methodReturnTypeArgs);
        }
        final List<JavaType> methodReturnTypeArgs = Collections
                .singletonList(method.getReturnType());
        return new JavaType(REQUEST.getFullyQualifiedTypeName(), 0,
                DataType.TYPE, null, methodReturnTypeArgs);
    }

    private Map<MethodMetadata, FieldMetadata> getRequestMethodsAndInvokedTypes(
            final JavaType entity, final String requestMetadataId) {
        final JavaType idType = persistenceMemberLocator
                .getIdentifierType(entity);
        if (idType == null) {
            return null;
        }
        final Map<MethodMetadata, FieldMetadata> requestMethods = new LinkedHashMap<MethodMetadata, FieldMetadata>();
        for (final Entry<MethodMetadataCustomDataKey, Collection<MethodParameter>> methodSignature : getRequestMethodSignatures(
                entity, idType).entrySet()) {
            final String methodId = methodSignature.getKey().name();
            final MemberTypeAdditions memberTypeAdditions = layerService
                    .getMemberTypeAdditions(requestMetadataId, methodId,
                            entity, idType, LAYER_POSITION,
                            methodSignature.getValue());
            Validate.notNull(memberTypeAdditions, "No support for " + methodId
                    + " method for domain type " + entity);
            final MethodMetadata requestMethod = getRequestMethod(entity,
                    methodSignature.getKey(), memberTypeAdditions,
                    requestMetadataId);
            requestMethods.put(requestMethod,
                    memberTypeAdditions.getInvokedField());
        }
        return requestMethods;
    }

    private Map<MethodMetadataCustomDataKey, Collection<MethodParameter>> getRequestMethodSignatures(
            final JavaType domainType, final JavaType idType) {
        final Map<MethodMetadataCustomDataKey, Collection<MethodParameter>> signatures = new LinkedHashMap<MethodMetadataCustomDataKey, Collection<MethodParameter>>();
        final List<MethodParameter> noArgs = Arrays.asList();
        signatures.put(COUNT_ALL_METHOD, noArgs);
        signatures.put(FIND_ALL_METHOD, noArgs);
        signatures.put(FIND_ENTRIES_METHOD, Arrays.asList(new MethodParameter(
                INT_PRIMITIVE, "firstResult"), new MethodParameter(
                INT_PRIMITIVE, "maxResults")));
        signatures.put(FIND_METHOD,
                Arrays.asList(new MethodParameter(idType, "id")));
        final List<MethodParameter> proxyParameterAsList = Arrays
                .asList(new MethodParameter(domainType, "proxy"));
        signatures.put(PERSIST_METHOD, proxyParameterAsList);
        signatures.put(REMOVE_METHOD, proxyParameterAsList);
        return signatures;
    }

    private JavaType getReturnType(final MethodMetadataCustomDataKey methodKey,
            final JavaType entity) {
        if (COUNT_ALL_METHOD.equals(methodKey)) {
            return LONG_PRIMITIVE;
        }
        if (FIND_ALL_METHOD.equals(methodKey)
                || FIND_ENTRIES_METHOD.equals(methodKey)) {
            return JavaType.listOf(entity);
        }
        if (FIND_METHOD.equals(methodKey)) {
            return entity;
        }
        if (PERSIST_METHOD.equals(methodKey) || REMOVE_METHOD.equals(methodKey)) {
            return VOID_PRIMITIVE;
        }
        throw new IllegalStateException("Unexpected method key " + methodKey);
    }

    private AnnotationMetadata getServiceNameAnnotation(
            final ClassOrInterfaceTypeDetails request,
            final JavaType invokedType, final JavaType entityType,
            final String requestMetadataId) {
        final List<AnnotationAttributeValue<?>> serviceAttributeValues = new ArrayList<AnnotationAttributeValue<?>>();
        if (invokedType == null) {
            // Active record; specify the entity type as the invoked "service"
            final StringAttributeValue stringAttributeValue = new StringAttributeValue(
                    new JavaSymbolName("value"),
                    entityType.getFullyQualifiedTypeName());
            serviceAttributeValues.add(stringAttributeValue);
        }
        else {
            // Layer component, e.g. repository or service; specify its type as
            // the invoked "service"
            final StringAttributeValue stringAttributeValue = new StringAttributeValue(
                    new JavaSymbolName("value"),
                    invokedType.getFullyQualifiedTypeName());
            serviceAttributeValues.add(stringAttributeValue);

            // Specify the locator that GWT will use to find it
            final LogicalPath requestLogicalPath = PhysicalTypeIdentifier
                    .getPath(request.getDeclaredByMetadataId());
            final JavaType serviceLocator = gwtTypeService
                    .getServiceLocator(requestLogicalPath.getModule());
            final StringAttributeValue locatorAttributeValue = new StringAttributeValue(
                    new JavaSymbolName("locator"),
                    serviceLocator.getFullyQualifiedTypeName());
            serviceAttributeValues.add(locatorAttributeValue);
        }
        return new AnnotationMetadataBuilder(SERVICE_NAME,
                serviceAttributeValues).build();
    }

    /**
     * Creates or updates the entity-specific request interface with
     * 
     * @param request
     * @param requestMethods the methods to declare in the interface, mapped to
     *            the injected field type on which they are invoked (required)
     * @param entityType
     * @param requestMetadataId
     * @return the Java source code for the request interface
     */
    private String writeRequestInterface(
            final ClassOrInterfaceTypeDetails request,
            final JavaType invokedType,
            final Iterable<MethodMetadata> requestMethods,
            final JavaType entityType, final String requestMetadataId) {
        final ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                request);

        // Service name annotation (@RooGwtRequest was already applied by
        // GwtOperationsImpl#createRequestInterface)
        typeDetailsBuilder.removeAnnotation(SERVICE_NAME);
        typeDetailsBuilder.addAnnotation(getServiceNameAnnotation(request,
                invokedType, entityType, requestMetadataId));

        // Super-interface
        typeDetailsBuilder.removeExtendsTypes(OLD_REQUEST_CONTEXT);
        if (!typeDetailsBuilder.getExtendsTypes().contains(REQUEST_CONTEXT)) {
            typeDetailsBuilder.addExtendsTypes(REQUEST_CONTEXT);
        }

        typeDetailsBuilder.clearDeclaredMethods();
        for (final MethodMetadata method : requestMethods) {
            typeDetailsBuilder.addMethod(getRequestMethod(request, method,
                    entityType, invokedType));
        }

        return gwtFileManager.write(typeDetailsBuilder.build(),
                GwtUtils.PROXY_REQUEST_WARNING);
    }
}
