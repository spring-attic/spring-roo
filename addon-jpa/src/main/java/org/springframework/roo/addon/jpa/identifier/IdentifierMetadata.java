package org.springframework.roo.addon.jpa.identifier;

import static org.springframework.roo.classpath.customdata.CustomDataKeys.IDENTIFIER_TYPE;
import static org.springframework.roo.model.JavaType.LONG_OBJECT;
import static org.springframework.roo.model.JdkJavaType.BIG_DECIMAL;
import static org.springframework.roo.model.JdkJavaType.DATE;
import static org.springframework.roo.model.JpaJavaType.COLUMN;
import static org.springframework.roo.model.JpaJavaType.EMBEDDABLE;
import static org.springframework.roo.model.JpaJavaType.TEMPORAL;
import static org.springframework.roo.model.JpaJavaType.TEMPORAL_TYPE;
import static org.springframework.roo.model.JpaJavaType.TRANSIENT;
import static org.springframework.roo.model.SpringJavaType.DATE_TIME_FORMAT;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.ConstructorMetadataBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata for {@link RooIdentifier}.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class IdentifierMetadata extends
        AbstractItdTypeDetailsProvidingMetadataItem {

    private static final String PROVIDES_TYPE_STRING = IdentifierMetadata.class
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

    public static String getMetadataIdentifierType() {
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

    // See {@link IdentifierService} for further information (populated via
    // {@link IdentifierMetadataProviderImpl}); may be null
    private List<Identifier> identifierServiceResult;

    private boolean publicNoArgConstructor;

    public IdentifierMetadata(final String identifier,
            final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final IdentifierAnnotationValues annotationValues,
            final List<Identifier> identifierServiceResult) {
        super(identifier, aspectName, governorPhysicalTypeMetadata);
        Validate.isTrue(isValid(identifier), "Metadata identification string '"
                + identifier + "' does not appear to be a valid");
        Validate.notNull(annotationValues, "Annotation values required");

        if (!isValid()) {
            return;
        }

        this.identifierServiceResult = identifierServiceResult;

        // Add @Embeddable annotation
        builder.addAnnotation(getEmbeddableAnnotation());

        // Add declared fields and accessors and mutators
        final List<FieldMetadataBuilder> fields = getFieldBuilders();
        for (final FieldMetadataBuilder field : fields) {
            builder.addField(field);
        }

        // Obtain a parameterised constructor
        builder.addConstructor(getParameterizedConstructor(fields));

        // Obtain a no-arg constructor, if one is appropriate to provide
        if (annotationValues.isNoArgConstructor()) {
            builder.addConstructor(getNoArgConstructor());
        }

        if (annotationValues.isGettersByDefault()) {
            for (final MethodMetadataBuilder accessor : getAccessors(fields)) {
                builder.addMethod(accessor);
            }
        }
        if (annotationValues.isSettersByDefault()) {
            for (final MethodMetadataBuilder mutator : getMutators(fields)) {
                builder.addMethod(mutator);
            }
        }

        // Add custom data tag for Roo Identifier type
        builder.putCustomData(IDENTIFIER_TYPE, null);

        // Create a representation of the desired output ITD
        buildItd();
    }

    /**
     * Locates the accessor methods.
     * <p>
     * If {@link #getFieldBuilders()} returns fields created by this ITD, public
     * accessors will automatically be produced in the declaring class.
     * 
     * @param fields
     * @return the accessors (never returns null)
     */
    private List<MethodMetadataBuilder> getAccessors(
            final List<FieldMetadataBuilder> fields) {
        final List<MethodMetadataBuilder> accessors = new ArrayList<MethodMetadataBuilder>();

        // Compute the names of the accessors that will be produced
        for (final FieldMetadataBuilder field : fields) {
            final JavaSymbolName requiredAccessorName = BeanInfoUtils
                    .getAccessorMethodName(field.getFieldName(),
                            field.getFieldType());
            final MethodMetadata accessor = getGovernorMethod(requiredAccessorName);
            if (accessor == null) {
                accessors.add(getAccessorMethod(field.getFieldName(),
                        field.getFieldType()));
            }
            else {
                Validate.isTrue(
                        Modifier.isPublic(accessor.getModifier()),
                        "User provided field but failed to provide a public '%s()' method in '%s'",
                        requiredAccessorName.getSymbolName(),
                        destination.getFullyQualifiedTypeName());
                accessors.add(new MethodMetadataBuilder(accessor));
            }
        }
        return accessors;
    }

    private AnnotationMetadataBuilder getColumnBuilder(
            final Identifier identifier) {
        final AnnotationMetadataBuilder columnBuilder = new AnnotationMetadataBuilder(
                COLUMN);
        columnBuilder.addStringAttribute("name", identifier.getColumnName());
        if (StringUtils.isNotBlank(identifier.getColumnDefinition())) {
            columnBuilder.addStringAttribute("columnDefinition",
                    identifier.getColumnDefinition());
        }
        columnBuilder.addBooleanAttribute("nullable", false);

        // Add length attribute for Strings
        if (identifier.getColumnSize() < 4000
                && identifier.getFieldType().equals(JavaType.STRING)) {
            columnBuilder.addIntegerAttribute("length",
                    identifier.getColumnSize());
        }

        // Add precision and scale attributes for numeric fields
        if (identifier.getScale() > 0
                && (identifier.getFieldType().equals(JavaType.DOUBLE_OBJECT)
                        || identifier.getFieldType().equals(
                                JavaType.DOUBLE_PRIMITIVE) || identifier
                        .getFieldType().equals(BIG_DECIMAL))) {
            columnBuilder.addIntegerAttribute("precision",
                    identifier.getColumnSize());
            columnBuilder.addIntegerAttribute("scale", identifier.getScale());
        }

        return columnBuilder;
    }

    private AnnotationMetadata getEmbeddableAnnotation() {
        if (governorTypeDetails.getAnnotation(EMBEDDABLE) != null) {
            return null;
        }
        final AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(
                EMBEDDABLE);
        return annotationBuilder.build();
    }

    /**
     * Locates declared fields.
     * <p>
     * If no parent is defined, one will be located or created. All declared
     * fields will be returned.
     * 
     * @return fields (never returns null)
     */
    private List<FieldMetadataBuilder> getFieldBuilders() {
        // Locate all declared fields
        final List<? extends FieldMetadata> declaredFields = governorTypeDetails
                .getDeclaredFields();

        // Add fields to ITD from annotation
        final List<FieldMetadata> fields = new ArrayList<FieldMetadata>();
        if (identifierServiceResult != null) {
            for (final Identifier identifier : identifierServiceResult) {
                final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
                annotations.add(getColumnBuilder(identifier));
                if (identifier.getFieldType().equals(DATE)) {
                    setDateAnnotations(identifier.getColumnDefinition(),
                            annotations);
                }

                final FieldMetadata idField = new FieldMetadataBuilder(getId(),
                        Modifier.PRIVATE, annotations,
                        identifier.getFieldName(), identifier.getFieldType())
                        .build();

                // Only add field to ITD if not declared on governor
                if (!hasField(declaredFields, idField)) {
                    fields.add(idField);
                }
            }
        }

        fields.addAll(declaredFields);

        // Remove fields with static and transient modifiers
        for (final Iterator<FieldMetadata> iter = fields.iterator(); iter
                .hasNext();) {
            final FieldMetadata field = iter.next();
            if (Modifier.isStatic(field.getModifier())
                    || Modifier.isTransient(field.getModifier())) {
                iter.remove();
            }
        }

        // Remove fields with the @Transient annotation
        final List<FieldMetadata> transientAnnotatedFields = governorTypeDetails
                .getFieldsWithAnnotation(TRANSIENT);
        if (fields.containsAll(transientAnnotatedFields)) {
            fields.removeAll(transientAnnotatedFields);
        }

        final List<FieldMetadataBuilder> fieldBuilders = new ArrayList<FieldMetadataBuilder>();
        if (!fields.isEmpty()) {
            for (final FieldMetadata field : fields) {
                fieldBuilders.add(new FieldMetadataBuilder(field));
            }
            return fieldBuilders;
        }

        // We need to create a default identifier field
        final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

        // Compute the column name, as required
        final AnnotationMetadataBuilder columnBuilder = new AnnotationMetadataBuilder(
                COLUMN);
        columnBuilder.addStringAttribute("name", "id");
        columnBuilder.addBooleanAttribute("nullable", false);
        annotations.add(columnBuilder);

        fieldBuilders.add(new FieldMetadataBuilder(getId(), Modifier.PRIVATE,
                annotations, new JavaSymbolName("id"), LONG_OBJECT));

        return fieldBuilders;
    }

    /**
     * Locates the mutator methods.
     * <p>
     * If {@link #getFieldBuilders()} returns fields created by this ITD, public
     * mutators will automatically be produced in the declaring class.
     * 
     * @param fields
     * @return the mutators (never returns null)
     */
    private List<MethodMetadataBuilder> getMutators(
            final List<FieldMetadataBuilder> fields) {
        final List<MethodMetadataBuilder> mutators = new ArrayList<MethodMetadataBuilder>();

        // Compute the names of the mutators that will be produced
        for (final FieldMetadataBuilder field : fields) {
            final JavaSymbolName requiredMutatorName = BeanInfoUtils
                    .getMutatorMethodName(field.getFieldName());
            final JavaType parameterType = field.getFieldType();
            final MethodMetadata mutator = getGovernorMethod(
                    requiredMutatorName, parameterType);
            if (mutator == null) {
                mutators.add(getMutatorMethod(field.getFieldName(),
                        field.getFieldType()));
            }
            else {
                Validate.isTrue(
                        Modifier.isPublic(mutator.getModifier()),
                        "User provided field but failed to provide a public '%s(%s)' method in '%s'",
                        requiredMutatorName.getSymbolName(), field
                                .getFieldName().getSymbolName(), destination
                                .getFullyQualifiedTypeName());
                mutators.add(new MethodMetadataBuilder(mutator));
            }
        }
        return mutators;
    }

    /**
     * Locates the no-arg constructor for this class, if available.
     * <p>
     * If a class defines a no-arg constructor, it is returned (irrespective of
     * access modifiers).
     * <p>
     * If a class does not define a no-arg constructor, one might be created. It
     * will only be created if the {@link RooIdentifier#noArgConstructor} is
     * true AND there is at least one other constructor declared in the source
     * file. If a constructor is created, it will have a private access
     * modifier.
     * 
     * @return the constructor (may return null if no constructor is to be
     *         produced)
     */
    private ConstructorMetadataBuilder getNoArgConstructor() {
        // Search for an existing constructor
        final List<JavaType> parameterTypes = new ArrayList<JavaType>();
        final ConstructorMetadata result = governorTypeDetails
                .getDeclaredConstructor(parameterTypes);
        if (result != null) {
            // Found an existing no-arg constructor on this class
            return null;
        }

        // Create the constructor
        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine("super();");

        final ConstructorMetadataBuilder constructorBuilder = new ConstructorMetadataBuilder(
                getId());
        constructorBuilder.setModifier(publicNoArgConstructor ? Modifier.PUBLIC
                : Modifier.PRIVATE);
        constructorBuilder.setParameterTypes(AnnotatedJavaType
                .convertFromJavaTypes(parameterTypes));
        constructorBuilder.setBodyBuilder(bodyBuilder);
        return constructorBuilder;
    }

    /**
     * Locates the parameterised constructor consisting of the id fields for
     * this class.
     * 
     * @param fields
     * @return the constructor, never null.
     */
    private ConstructorMetadataBuilder getParameterizedConstructor(
            final List<FieldMetadataBuilder> fields) {
        // Search for an existing constructor
        final List<JavaType> parameterTypes = new ArrayList<JavaType>();
        for (final FieldMetadataBuilder field : fields) {
            parameterTypes.add(field.getFieldType());
        }

        final ConstructorMetadata result = governorTypeDetails
                .getDeclaredConstructor(parameterTypes);
        if (result != null) {
            // Found an existing parameterised constructor on this class
            publicNoArgConstructor = true;
            return null;
        }

        final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine("super();");
        for (final FieldMetadataBuilder field : fields) {
            final String fieldName = field.getFieldName().getSymbolName();
            bodyBuilder.appendFormalLine("this." + fieldName + " = "
                    + fieldName + ";");
            parameterNames.add(field.getFieldName());
        }

        // Create the constructor
        final ConstructorMetadataBuilder constructorBuilder = new ConstructorMetadataBuilder(
                getId());
        constructorBuilder.setModifier(Modifier.PUBLIC);
        constructorBuilder.setParameterTypes(AnnotatedJavaType
                .convertFromJavaTypes(parameterTypes));
        constructorBuilder.setParameterNames(parameterNames);
        constructorBuilder.setBodyBuilder(bodyBuilder);
        return constructorBuilder;
    }

    private boolean hasField(
            final List<? extends FieldMetadata> declaredFields,
            final FieldMetadata idField) {
        for (final FieldMetadata declaredField : declaredFields) {
            if (declaredField.getFieldName().equals(idField.getFieldName())) {
                return true;
            }
        }
        return false;
    }

    private void setDateAnnotations(final String columnDefinition,
            final List<AnnotationMetadataBuilder> annotations) {
        // Add JSR 220 @Temporal annotation to date fields
        String temporalType = StringUtils.defaultIfEmpty(
                StringUtils.upperCase(columnDefinition), "DATE");
        if ("DATETIME".equals(temporalType)) {
            temporalType = "TIMESTAMP"; // ROO-2606
        }
        final AnnotationMetadataBuilder temporalBuilder = new AnnotationMetadataBuilder(
                TEMPORAL);
        temporalBuilder.addEnumAttribute("value", new EnumDetails(
                TEMPORAL_TYPE, new JavaSymbolName(temporalType)));
        annotations.add(temporalBuilder);

        final AnnotationMetadataBuilder dateTimeFormatBuilder = new AnnotationMetadataBuilder(
                DATE_TIME_FORMAT);
        dateTimeFormatBuilder.addStringAttribute("style", "M-");
        annotations.add(dateTimeFormatBuilder);
    }
}
