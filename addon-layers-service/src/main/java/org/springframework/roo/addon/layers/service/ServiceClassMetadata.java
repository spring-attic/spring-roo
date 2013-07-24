package org.springframework.roo.addon.layers.service;

import static org.springframework.roo.model.SpringJavaType.PRE_AUTHORIZE;
import static org.springframework.roo.model.SpringJavaType.SERVICE;
import static org.springframework.roo.model.SpringJavaType.TRANSACTIONAL;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.CustomDataKeys;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * @author Stefan Schmidt
 * @since 1.2.0
 */
public class ServiceClassMetadata extends
        AbstractItdTypeDetailsProvidingMetadataItem {

    private static final String PROVIDES_TYPE_STRING = ServiceClassMetadata.class
            .getName();
    private static final String PROVIDES_TYPE = MetadataIdentificationUtils
            .create(PROVIDES_TYPE_STRING);

    public static String createIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return PhysicalTypeIdentifierNamingUtils.createIdentifier(
                PROVIDES_TYPE_STRING, javaType, path);
    }

    public static JavaType getJavaType(final String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.getJavaType(
                PROVIDES_TYPE_STRING, metadataIdentificationString);
    }

    public static String getMetadataIdentiferType() {
        return PROVIDES_TYPE;
    }

    public static LogicalPath getPath(final String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING,
                metadataIdentificationString);
    }

    public static boolean isValid(final String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING,
                metadataIdentificationString);
    }

    /**
     * Constructor
     * 
     * @param identifier the identifier for this item of metadata (required)
     * @param aspectName the Java type of the ITD (required)
     * @param governorPhysicalTypeMetadata the governor, which is expected to
     *            contain a {@link ClassOrInterfaceTypeDetails} (required)
     * @param governorDetails (required)
     * @param annotationValues (required)
     * @param domainTypeToIdTypeMap (required)
     * @param allCrudAdditions any additions to be made to the service class in
     *            order to invoke lower-layer methods (required)
     * @param domainTypePlurals the plurals of each domain type managed by the
     *            service
     */
    public ServiceClassMetadata(
            final String identifier,
            final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final MemberDetails governorDetails,
            final ServiceAnnotationValues annotationValues,
            final Map<JavaType, JavaType> domainTypeToIdTypeMap,
            final Map<JavaType, Map<ServiceLayerMethod, MemberTypeAdditions>> allCrudAdditions,
            final Map<JavaType, String> domainTypePlurals, String serviceName) {
        super(identifier, aspectName, governorPhysicalTypeMetadata);
        Validate.notNull(allCrudAdditions, "CRUD additions required");
        Validate.notNull(annotationValues, "Annotation values required");
        Validate.notNull(governorDetails, "Governor details required");
        Validate.notNull(domainTypePlurals, "Domain type plurals required");

        for (final Entry<JavaType, JavaType> entry : domainTypeToIdTypeMap
                .entrySet()) {
            final JavaType domainType = entry.getKey();
            final JavaType idType = entry.getValue();
            final Map<ServiceLayerMethod, MemberTypeAdditions> crudAdditions = allCrudAdditions
                    .get(domainType);
            for (final ServiceLayerMethod method : ServiceLayerMethod.values()) {
                final JavaSymbolName methodName = method.getSymbolName(
                        annotationValues, domainType,
                        domainTypePlurals.get(domainType));
                final String permissionName = method.getPermissionName(
                        annotationValues, domainType,
                        domainTypePlurals.get(domainType));
                if (methodName != null
                        && !governorDetails.isMethodDeclaredByAnother(
                                methodName,
                                method.getParameterTypes(domainType, idType),
                                getId())) {
                    // The method is desired and the service class' Java file
                    // doesn't contain it, so generate it
                    final MemberTypeAdditions lowerLayerCallAdditions = crudAdditions
                            .get(method);
                    if (lowerLayerCallAdditions != null) {
                        // A lower layer implements it
                        lowerLayerCallAdditions.copyAdditionsTo(builder,
                                governorTypeDetails);
                    }
                    final String body = method.getBody(lowerLayerCallAdditions);
                    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
                    bodyBuilder.appendFormalLine(body);
                    List<JavaSymbolName> parameterNames = method
                            .getParameterNames(domainType, idType);
                    MethodMetadataBuilder methodMetadataBuilder = new MethodMetadataBuilder(
                            getId(), Modifier.PUBLIC, methodName,
                            method.getReturnType(domainType),
                            AnnotatedJavaType.convertFromJavaTypes(method
                                    .getParameterTypes(domainType, idType)),
                            parameterNames, bodyBuilder);

                    StringBuilder preAuthorizeValue = new StringBuilder();

                    boolean isCreateOrUpdateMethod = false;
                    boolean isReadMethod = false;
                    boolean isDeleteMethod = false;
                    boolean usesDomainTypeMethod = false;
                    boolean requireAuthentication = annotationValues
                            .requireAuthentication();

                    // checks to see if the method is a "save" method
                    if (method.getKey().equals(
                            CustomDataKeys.PERSIST_METHOD.name())
                            || method.getKey().equals(
                                    CustomDataKeys.MERGE_METHOD.name())) {
                        isCreateOrUpdateMethod = true;
                    }

                    // Checks to see if the method is a "delete method
                    if (method.getKey().equals(
                            CustomDataKeys.REMOVE_METHOD.name())) {
                        isDeleteMethod = true;
                    }

                    // Checks to see if the method is a "read" method
                    if (method.getKey().equals(
                            CustomDataKeys.FIND_ALL_METHOD.name())
                            || method.getKey().equals(
                                    CustomDataKeys.FIND_ENTRIES_METHOD.name())
                            || method.getKey().equals(
                                    CustomDataKeys.FIND_METHOD.name())
                            || method.getKey().equals(
                                    CustomDataKeys.COUNT_ALL_METHOD)) {
                        isReadMethod = true;
                    }

                    if (method.getKey().equals(
                            CustomDataKeys.PERSIST_METHOD.name())
                            || method.getKey().equals(
                                    CustomDataKeys.MERGE_METHOD.name())
                            || method.getKey().equals(
                                    CustomDataKeys.REMOVE_METHOD.name())
                            || method.getKey().equals(
                                    CustomDataKeys.FIND_METHOD.name())) {
                        usesDomainTypeMethod = true;
                    }

                    // Adds required roles to @PreAuthorize annotation if the
                    // required roles for persist methods
                    if (annotationValues.getAuthorizedCreateOrUpdateRoles().length > 0
                            && isCreateOrUpdateMethod) {
                        requireAuthentication = true;
                        addRoles(preAuthorizeValue,
                                annotationValues
                                        .getAuthorizedCreateOrUpdateRoles());
                    }

                    // Adds required roles to @PreAuthorize annotation if the
                    // required roles exist for read methods
                    if (annotationValues.getAuthorizedReadRoles().length > 0
                            && isReadMethod) {
                        requireAuthentication = true;
                        addRoles(preAuthorizeValue,
                                annotationValues.getAuthorizedReadRoles());
                    }

                    // Adds required roles to @PreAuthorize annotation if the
                    // required roles exist for delete methods
                    if (annotationValues.getAuthorizedDeleteRoles().length > 0
                            && isDeleteMethod) {
                        requireAuthentication = true;
                        addRoles(preAuthorizeValue,
                                annotationValues.getAuthorizedDeleteRoles());
                    }

                    // Add permission evaluator to @PreAuthorize annotation if
                    // required
                    if (annotationValues.usePermissionEvaluator()
                            && usesDomainTypeMethod) {
                        requireAuthentication = true;
                        preAuthorizeValue
                                .append(" OR hasPermission("
                                        + (parameterNames.size() == 0 ? "#"
                                                : "#"
                                                        + parameterNames
                                                                .get(0)
                                                                .getSymbolName())
                                        + ", '" + serviceName + ":"
                                        + permissionName + "'" + ")");
                    }

                    // Adds "isAuthenticated" to @PreAuthorize annotation
                    if (requireAuthentication) {
                        preAuthorizeValue.insert(0, "isAuthenticated() AND ");
                    }

                    if (!preAuthorizeValue.toString().equals("")) {
                        final AnnotationMetadataBuilder annotationMetadataBuilder = new AnnotationMetadataBuilder(
                                PRE_AUTHORIZE);
                        annotationMetadataBuilder.addStringAttribute("value",
                                preAuthorizeValue.toString());
                        methodMetadataBuilder
                                .addAnnotation(annotationMetadataBuilder
                                        .build());
                    }

                    builder.addMethod(methodMetadataBuilder);
                }
            }
        }

        // If useXmlConfiguration is true, do not add @Service
        if (!annotationValues.useXmlConfiguration()) {
            // Introduce the @Service annotation via the ITD if it's not already
            // on
            // the service's Java class
            final AnnotationMetadata serviceAnnotation = new AnnotationMetadataBuilder(
                    SERVICE).build();
            if (!governorDetails.isRequestingAnnotatedWith(serviceAnnotation,
                    getId())) {
                builder.addAnnotation(serviceAnnotation);
            }
        }

        // Introduce the @Transactional annotation via the ITD if it's not
        // already on the service's Java class
        if (annotationValues.isTransactional()) {
            final AnnotationMetadata transactionalAnnotation = new AnnotationMetadataBuilder(
                    TRANSACTIONAL).build();
            if (!governorDetails.isRequestingAnnotatedWith(
                    transactionalAnnotation, getId())) {
                builder.addAnnotation(transactionalAnnotation);
            }
        }

        // Create a representation of the desired output ITD
        itdTypeDetails = builder.build();
    }

    private void addRoles(StringBuilder preAuthorizeValue, String[] roles) {
        preAuthorizeValue.append("(");
        int i = 0;
        for (String role : roles) {
            if (i > 0)
                preAuthorizeValue.append(" OR ");

            preAuthorizeValue.append("hasRole('" + role + "')");
            i++;
        }

        preAuthorizeValue.append(")");
    }

    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("identifier", getId());
        builder.append("valid", valid);
        builder.append("aspectName", aspectName);
        builder.append("destinationType", destination);
        builder.append("governor", governorPhysicalTypeMetadata.getId());
        builder.append("itdTypeDetails", itdTypeDetails);
        return builder.toString();
    }
}
