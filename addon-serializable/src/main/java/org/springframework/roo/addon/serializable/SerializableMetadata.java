package org.springframework.roo.addon.serializable;

import static java.lang.reflect.Modifier.FINAL;
import static java.lang.reflect.Modifier.PRIVATE;
import static java.lang.reflect.Modifier.STATIC;
import static org.springframework.roo.model.JavaType.LONG_PRIMITIVE;
import static org.springframework.roo.model.JdkJavaType.SERIALIZABLE;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.CustomDataKeys;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.ItdTypeDetailsBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata for {@link RooSerializable}.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class SerializableMetadata extends
        AbstractItdTypeDetailsProvidingMetadataItem {

    private static final String DEFAULT_SERIAL_VERSION = "1L";
    private static final String PROVIDES_TYPE_STRING = SerializableMetadata.class
            .getName();
    private static final String PROVIDES_TYPE = MetadataIdentificationUtils
            .create(PROVIDES_TYPE_STRING);
    static final JavaSymbolName SERIAL_VERSION_FIELD = new JavaSymbolName(
            "serialVersionUID");

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
     * @param identifier
     * @param aspectName
     * @param governorPhysicalTypeMetadata
     */
    public SerializableMetadata(final String identifier,
            final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
        super(identifier, aspectName, governorPhysicalTypeMetadata);
        Validate.isTrue(isValid(identifier), "Metadata id '%s' is invalid",
                identifier);

        if (isValid()) {
            ensureGovernorImplements(SERIALIZABLE);
            addSerialVersionUIDFieldIfRequired();
            buildItd();
        }
    }

    /**
     * Adds a "serialVersionUID" field to the {@link ItdTypeDetailsBuilder} if
     * the governor doesn't already contain it.
     */
    private void addSerialVersionUIDFieldIfRequired() {
        if (!governorTypeDetails.declaresField(SERIAL_VERSION_FIELD)) {
            builder.addField(createSerialVersionField());
        }
    }

    /**
     * Generates a field to store the serialization ID
     * 
     * @return a non-<code>null</code> field
     */
    private FieldMetadataBuilder createSerialVersionField() {
        final FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(
                getId(), PRIVATE | STATIC | FINAL, SERIAL_VERSION_FIELD,
                LONG_PRIMITIVE, DEFAULT_SERIAL_VERSION);
        fieldBuilder.getCustomData().put(
                CustomDataKeys.SERIAL_VERSION_UUID_FIELD, true);
        return fieldBuilder;
    }

    /**
     * For unit testing
     * 
     * @return
     */
    ItdTypeDetails getItdTypeDetails() {
        return itdTypeDetails;
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
