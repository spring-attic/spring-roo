package org.springframework.roo.addon.layers.service;

import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * The metadata about a service interface within a user project
 * 
 * @author Stefan Schmidt
 * @author Andrew Swan
 * @since 1.2.0
 */
public class ServiceInterfaceMetadata extends
        AbstractItdTypeDetailsProvidingMetadataItem {

    private static final InvocableMemberBodyBuilder BODY = new InvocableMemberBodyBuilder();
    private static final String PROVIDES_TYPE_STRING = ServiceInterfaceMetadata.class
            .getName();
    private static final String PROVIDES_TYPE = MetadataIdentificationUtils
            .create(PROVIDES_TYPE_STRING);
    private static final int PUBLIC_ABSTRACT = Modifier.PUBLIC
            | Modifier.ABSTRACT;

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

    private final ServiceAnnotationValues annotationValues;

    private final MemberDetails governorDetails;

    /**
     * Constructor
     * 
     * @param identifier (required)
     * @param aspectName (required)
     * @param governorPhysicalTypeMetadata (required)
     * @param governorDetails (required)
     * @param domainTypeToIdTypeMap (required)
     * @param annotationValues (required)
     * @param domainTypePlurals
     */
    public ServiceInterfaceMetadata(final String identifier,
            final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final MemberDetails governorDetails,
            final Map<JavaType, JavaType> domainTypeToIdTypeMap,
            final ServiceAnnotationValues annotationValues,
            final Map<JavaType, String> domainTypePlurals) {
        super(identifier, aspectName, governorPhysicalTypeMetadata);
        Validate.notNull(annotationValues, "Annotation values required");
        Validate.notNull(governorDetails, "Governor member details required");
        Validate.notNull(domainTypeToIdTypeMap,
                "Domain type to ID type map required required");
        Validate.notNull(domainTypePlurals,
                "Domain type plural values required");

        this.annotationValues = annotationValues;
        this.governorDetails = governorDetails;

        for (final Entry<JavaType, JavaType> entry : domainTypeToIdTypeMap
                .entrySet()) {
            final JavaType domainType = entry.getKey();
            final String plural = domainTypePlurals.get(domainType);
            for (final ServiceLayerMethod method : ServiceLayerMethod.values()) {
                builder.addMethod(getMethod(method, domainType,
                        entry.getValue(), plural));
            }
        }

        // Create a representation of the desired output ITD
        itdTypeDetails = builder.build();
    }

    /**
     * Returns the metadata for declaring the given method in the service
     * interface
     * 
     * @param method the method to declare
     * @param domainType the domain type being managed
     * @param idType
     * @param plural the domain type's plural
     * @return <code>null</code> if the method isn't required or is already
     *         declared in the governor
     */
    private MethodMetadataBuilder getMethod(final ServiceLayerMethod method,
            final JavaType domainType, final JavaType idType,
            final String plural) {
        final JavaSymbolName methodName = method.getSymbolName(
                annotationValues, domainType, plural);
        if (methodName != null
                && governorDetails.isMethodDeclaredByAnother(methodName,
                        method.getParameterTypes(domainType, idType), getId())) {
            // We don't want this method, or the governor already declares it
            return null;
        }

        return new MethodMetadataBuilder(getId(), PUBLIC_ABSTRACT, methodName,
                method.getReturnType(domainType),
                AnnotatedJavaType.convertFromJavaTypes(method
                        .getParameterTypes(domainType, idType)),
                method.getParameterNames(domainType, idType), BODY);
    }

    public ServiceAnnotationValues getServiceAnnotationValues() {
        return annotationValues;
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
