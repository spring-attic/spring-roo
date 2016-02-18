package org.springframework.roo.addon.layers.repository.jpa.addon;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.layers.repository.jpa.annotations.RooJpaRepositoryCustomImpl;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.ConstructorMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.ImportRegistrationResolver;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata for {@link RooJpaRepositoryCustomImpl}.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public class RepositoryJpaCustomImplMetadata extends
        AbstractItdTypeDetailsProvidingMetadataItem {

    private static final String PROVIDES_TYPE_STRING = RepositoryJpaCustomImplMetadata.class
            .getName();
    private static final String PROVIDES_TYPE = MetadataIdentificationUtils
            .create(PROVIDES_TYPE_STRING);
    
    private static final JavaType QUERY_DSL_REPOSITORY_SUPPORT = new JavaType(
            "org.springframework.data.jpa.repository.support.QueryDslRepositorySupport");
    
    private ImportRegistrationResolver importResolver;

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
     * @param annotationValues (required)
     * @param identifierType the type of the entity's identifier field
     *            (required)
     * @param domainType entity referenced on interface
     */
    public RepositoryJpaCustomImplMetadata(final String identifier,
            final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final RepositoryJpaCustomImplAnnotationValues annotationValues,
            final JavaType domainType) {
        super(identifier, aspectName, governorPhysicalTypeMetadata);
        Validate.notNull(annotationValues, "Annotation values required");
        
        this.importResolver = builder.getImportRegistrationResolver();
        
        // RepositoryCustom implementation always extends QueryDslRepositorySupport
        ensureGovernorExtends(QUERY_DSL_REPOSITORY_SUPPORT);
        
        // Get repository that needs to be implemented
        ensureGovernorImplements(annotationValues.getRepository());
        
        // Add constructor
        builder.addConstructor(getRepositoryCustomImplConstructor(domainType));

        // Build the ITD
        itdTypeDetails = builder.build();
    }
    
    /**
     * Returns constructor for RepositoryCustom implementation
     * 
     * @param domainType referenced entity
     * @return ConstructorMetadata that contains necessary body
     */
    private ConstructorMetadata getRepositoryCustomImplConstructor(JavaType domainType) {
        // Generating constructor builder
        ConstructorMetadataBuilder constructorBuilder = new ConstructorMetadataBuilder(getId());

        // Generating body
        InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine(String.format("super(%s.class);",
                domainType.getNameIncludingTypeParameters(false,
                        importResolver)));
        constructorBuilder.setBodyBuilder(bodyBuilder);

        return constructorBuilder.build();
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
