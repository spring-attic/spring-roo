package org.springframework.roo.addon.op4j;

import static org.springframework.roo.model.JavaType.OBJECT;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata to be triggered by {@link RooOp4j} annotation
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
public class Op4jMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

    // fully-qualified?
    private static final JavaType JAVA_RUN_TYPE_TYPES = new JavaType(
            "org.javaruntype.type.Types");
    private static final JavaType KEYS = new JavaType("Keys"); // TODO should be
    private static final JavaType OP4J_GET = new JavaType(
            "org.op4j.functions.Get");
    private static final String PROVIDES_TYPE_STRING = Op4jMetadata.class
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

    public Op4jMetadata(final String identifier, final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
        super(identifier, aspectName, governorPhysicalTypeMetadata);
        Validate.isTrue(isValid(identifier), "Metadata identification string '"
                + identifier + "' does not appear to be a valid");

        if (!isValid()) {
            return;
        }

        builder.addInnerType(getInnerType());

        // Create a representation of the desired output ITD
        itdTypeDetails = builder.build();
    }

    private ClassOrInterfaceTypeDetails getInnerType() {
        final List<FieldMetadataBuilder> fields = new ArrayList<FieldMetadataBuilder>();

        builder.getImportRegistrationResolver().addImports(OP4J_GET,
                JAVA_RUN_TYPE_TYPES);

        final String targetName = super.destination.getSimpleTypeName();
        final String initializer = "Get.attrOf(Types.forClass(" + targetName
                + ".class),\"" + targetName.toLowerCase() + "\")";
        final List<JavaType> parameters = Arrays.asList(OBJECT, destination);
        final JavaType function = new JavaType("org.op4j.functions.Function",
                0, DataType.TYPE, null, parameters);
        final int fieldModifier = Modifier.PUBLIC | Modifier.STATIC
                | Modifier.FINAL;
        final FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(
                getId(), fieldModifier, new JavaSymbolName(
                        targetName.toUpperCase()), function, initializer);
        fields.add(fieldBuilder);

        final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                getId(), Modifier.PUBLIC | Modifier.STATIC, KEYS,
                PhysicalTypeCategory.CLASS);
        cidBuilder.setDeclaredFields(fields);
        return cidBuilder.build();
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
