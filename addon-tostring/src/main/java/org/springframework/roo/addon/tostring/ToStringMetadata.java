package org.springframework.roo.addon.tostring;

import static org.springframework.roo.model.JavaType.STRING;

import java.lang.reflect.Modifier;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata for {@link RooToString}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class ToStringMetadata extends
        AbstractItdTypeDetailsProvidingMetadataItem {

    private static final String PROVIDES_TYPE_STRING = ToStringMetadata.class
            .getName();
    private static final String PROVIDES_TYPE = MetadataIdentificationUtils
            .create(PROVIDES_TYPE_STRING);
    private static final String STYLE = "SHORT_PREFIX_STYLE";
    private static final JavaType TO_STRING_BUILDER = new JavaType(
            "org.apache.commons.lang3.builder.ReflectionToStringBuilder");
    private static final JavaType TO_STRING_STYLE = new JavaType(
            "org.apache.commons.lang3.builder.ToStringStyle");

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

    private final ToStringAnnotationValues annotationValues;

    /**
     * Constructor
     * 
     * @param identifier
     * @param aspectName
     * @param governorPhysicalTypeMetadata
     * @param annotationValues
     */
    public ToStringMetadata(final String identifier, final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final ToStringAnnotationValues annotationValues) {
        super(identifier, aspectName, governorPhysicalTypeMetadata);
        Validate.isTrue(isValid(identifier), "Metadata identification string '"
                + identifier + "' does not appear to be a valid");
        Validate.notNull(annotationValues, "Annotation values required");

        this.annotationValues = annotationValues;

        // Generate the toString() method
        builder.addMethod(getToStringMethod());

        // Create a representation of the desired output ITD
        itdTypeDetails = builder.build();
    }

    /**
     * Obtains the "toString" method for this type, if available.
     * <p>
     * If the user provided a non-default name for "toString", that method will
     * be returned.
     * 
     * @return the "toString" method declared on this type or that will be
     *         introduced (or null if undeclared and not introduced)
     */
    private MethodMetadataBuilder getToStringMethod() {
        final String toStringMethod = annotationValues.getToStringMethod();
        if (StringUtils.isBlank(toStringMethod)) {
            return null;
        }

        // Compute the relevant toString method name
        final JavaSymbolName methodName = new JavaSymbolName(toStringMethod);

        // See if the type itself declared the method
        if (governorHasMethod(methodName)) {
            return null;
        }

        builder.getImportRegistrationResolver().addImports(TO_STRING_BUILDER,
                TO_STRING_STYLE);

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        final String[] excludeFields = annotationValues.getExcludeFields();
        String str;
        if (excludeFields != null && excludeFields.length > 0) {
            final StringBuilder builder = new StringBuilder();
            for (int i = 0; i < excludeFields.length; i++) {
                if (i > 0) {
                    builder.append(", ");
                }
                builder.append("\"").append(excludeFields[i]).append("\"");
            }
            str = "new ReflectionToStringBuilder(this, ToStringStyle." + STYLE
                    + ").setExcludeFieldNames(" + builder.toString()
                    + ").toString();";
        }
        else {
            str = "ReflectionToStringBuilder.toString(this, ToStringStyle."
                    + STYLE + ");";
        }
        bodyBuilder.appendFormalLine("return " + str);

        return new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
                STRING, bodyBuilder);
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
