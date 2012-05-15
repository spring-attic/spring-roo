package org.springframework.roo.addon.dod;

import static org.springframework.roo.model.HibernateJavaType.VALIDATOR_CONSTRAINTS_EMAIL;
import static org.springframework.roo.model.JavaType.STRING;
import static org.springframework.roo.model.JdkJavaType.ARRAY_LIST;
import static org.springframework.roo.model.JdkJavaType.BIG_DECIMAL;
import static org.springframework.roo.model.JdkJavaType.BIG_INTEGER;
import static org.springframework.roo.model.JdkJavaType.CALENDAR;
import static org.springframework.roo.model.JdkJavaType.DATE;
import static org.springframework.roo.model.JdkJavaType.GREGORIAN_CALENDAR;
import static org.springframework.roo.model.JdkJavaType.ITERATOR;
import static org.springframework.roo.model.JdkJavaType.LIST;
import static org.springframework.roo.model.JdkJavaType.RANDOM;
import static org.springframework.roo.model.JdkJavaType.SECURE_RANDOM;
import static org.springframework.roo.model.Jsr303JavaType.CONSTRAINT_VIOLATION;
import static org.springframework.roo.model.Jsr303JavaType.CONSTRAINT_VIOLATION_EXCEPTION;
import static org.springframework.roo.model.Jsr303JavaType.DECIMAL_MAX;
import static org.springframework.roo.model.Jsr303JavaType.DECIMAL_MIN;
import static org.springframework.roo.model.Jsr303JavaType.DIGITS;
import static org.springframework.roo.model.Jsr303JavaType.FUTURE;
import static org.springframework.roo.model.Jsr303JavaType.MAX;
import static org.springframework.roo.model.Jsr303JavaType.MIN;
import static org.springframework.roo.model.Jsr303JavaType.NOT_NULL;
import static org.springframework.roo.model.Jsr303JavaType.PAST;
import static org.springframework.roo.model.Jsr303JavaType.SIZE;
import static org.springframework.roo.model.SpringJavaType.AUTOWIRED;
import static org.springframework.roo.model.SpringJavaType.COMPONENT;

import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.CustomDataKeys;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JdkJavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata for {@link RooDataOnDemand}.
 * 
 * @author Ben Alex
 * @author Stefan Schmidt
 * @author Alan Stewart
 * @author Greg Turnquist
 * @author Andrew Swan
 * @since 1.0
 */
public class DataOnDemandMetadata extends
        AbstractItdTypeDetailsProvidingMetadataItem {

    private static final String INDEX_VAR = "index";
    private static final JavaSymbolName INDEX_SYMBOL = new JavaSymbolName(
            INDEX_VAR);
    private static final JavaSymbolName MAX_SYMBOL = new JavaSymbolName("max");
    private static final JavaSymbolName MIN_SYMBOL = new JavaSymbolName("min");
    private static final String OBJ_VAR = "obj";
    private static final JavaSymbolName OBJ_SYMBOL = new JavaSymbolName(OBJ_VAR);
    private static final String PROVIDES_TYPE_STRING = DataOnDemandMetadata.class
            .getName();
    private static final String PROVIDES_TYPE = MetadataIdentificationUtils
            .create(PROVIDES_TYPE_STRING);
    private static final JavaSymbolName VALUE = new JavaSymbolName("value");

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

    private JavaSymbolName dataFieldName;
    private final Map<FieldMetadata, Map<FieldMetadata, String>> embeddedFieldInitializers = new LinkedHashMap<FieldMetadata, Map<FieldMetadata, String>>();
    private List<EmbeddedHolder> embeddedHolders;
    private EmbeddedIdHolder embeddedIdHolder;
    private JavaType entity;
    private final Map<FieldMetadata, String> fieldInitializers = new LinkedHashMap<FieldMetadata, String>();
    private MemberTypeAdditions findMethod;
    private MethodMetadata identifierAccessor;
    private JavaType identifierType;
    private MethodMetadata modifyMethod;
    private MethodMetadata newTransientEntityMethod;
    private MethodMetadata randomPersistentEntityMethod;
    private final List<JavaType> requiredDataOnDemandCollaborators = new ArrayList<JavaType>();
    private JavaSymbolName rndFieldName;
    private MethodMetadata specificPersistentEntityMethod;

    /**
     * Constructor
     * 
     * @param identifier
     * @param aspectName
     * @param governorPhysicalTypeMetadata
     * @param annotationValues
     * @param identifierAccessor
     * @param findMethodAdditions
     * @param findEntriesMethod
     * @param persistMethodAdditions
     * @param flushMethod
     * @param locatedFields
     * @param entity
     * @param embeddedIdHolder
     * @param embeddedHolders
     */
    public DataOnDemandMetadata(final String identifier,
            final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final DataOnDemandAnnotationValues annotationValues,
            final MethodMetadata identifierAccessor,
            final MemberTypeAdditions findMethodAdditions,
            final MemberTypeAdditions findEntriesMethodAdditions,
            final MemberTypeAdditions persistMethodAdditions,
            final MemberTypeAdditions flushMethod,
            final Map<FieldMetadata, DataOnDemandMetadata> locatedFields,
            final JavaType identifierType,
            final EmbeddedIdHolder embeddedIdHolder,
            final List<EmbeddedHolder> embeddedHolders) {
        super(identifier, aspectName, governorPhysicalTypeMetadata);
        Validate.isTrue(isValid(identifier), "Metadata identification string '"
                + identifier + "' does not appear to be a valid");
        Validate.notNull(annotationValues, "Annotation values required");
        Validate.notNull(identifierAccessor,
                "Identifier accessor method required");
        Validate.notNull(locatedFields, "Located fields map required");
        Validate.notNull(embeddedHolders, "Embedded holders list required");

        if (!isValid()) {
            return;
        }

        if (findEntriesMethodAdditions == null
                || persistMethodAdditions == null
                || findMethodAdditions == null) {
            valid = false;
            return;
        }

        this.embeddedIdHolder = embeddedIdHolder;
        this.embeddedHolders = embeddedHolders;
        this.identifierAccessor = identifierAccessor;
        findMethod = findMethodAdditions;
        this.identifierType = identifierType;
        entity = annotationValues.getEntity();

        // Calculate and store field initializers
        for (final Map.Entry<FieldMetadata, DataOnDemandMetadata> entry : locatedFields
                .entrySet()) {
            final FieldMetadata field = entry.getKey();
            final String initializer = getFieldInitializer(field,
                    entry.getValue());
            if (!StringUtils.isBlank(initializer)) {
                fieldInitializers.put(field, initializer);
            }
        }

        for (final EmbeddedHolder embeddedHolder : embeddedHolders) {
            final Map<FieldMetadata, String> initializers = new LinkedHashMap<FieldMetadata, String>();
            for (final FieldMetadata field : embeddedHolder.getFields()) {
                initializers.put(field, getFieldInitializer(field, null));
            }
            embeddedFieldInitializers.put(embeddedHolder.getEmbeddedField(),
                    initializers);
        }

        builder.addAnnotation(getComponentAnnotation());
        builder.addField(getRndField());
        builder.addField(getDataField());

        addCollaboratingDoDFieldsToBuilder();
        setNewTransientEntityMethod();

        builder.addMethod(getEmbeddedIdMutatorMethod());

        for (final EmbeddedHolder embeddedHolder : embeddedHolders) {
            builder.addMethod(getEmbeddedClassMutatorMethod(embeddedHolder));
            addEmbeddedClassFieldMutatorMethodsToBuilder(embeddedHolder);
        }

        for (final MethodMetadataBuilder fieldInitializerMethod : getFieldMutatorMethods()) {
            builder.addMethod(fieldInitializerMethod);
        }

        setSpecificPersistentEntityMethod();
        setRandomPersistentEntityMethod();
        setModifyMethod();
        builder.addMethod(getInitMethod(annotationValues.getQuantity(),
                findEntriesMethodAdditions, persistMethodAdditions, flushMethod));

        itdTypeDetails = builder.build();
    }

    private void addCollaboratingDoDFieldsToBuilder() {
        final Set<JavaSymbolName> fields = new LinkedHashSet<JavaSymbolName>();
        for (final JavaType entityNeedingCollaborator : requiredDataOnDemandCollaborators) {
            final JavaType collaboratorType = getCollaboratingType(entityNeedingCollaborator);
            final String collaboratingFieldName = getCollaboratingFieldName(
                    entityNeedingCollaborator).getSymbolName();

            final JavaSymbolName fieldSymbolName = new JavaSymbolName(
                    collaboratingFieldName);
            final FieldMetadata candidate = governorTypeDetails
                    .getField(fieldSymbolName);
            if (candidate != null) {
                // We really expect the field to be correct if we're going to
                // rely on it
                Validate.isTrue(
                        candidate.getFieldType().equals(collaboratorType),
                        "Field '" + collaboratingFieldName + "' on '"
                                + destination.getFullyQualifiedTypeName()
                                + "' must be of type '"
                                + collaboratorType.getFullyQualifiedTypeName()
                                + "'");
                Validate.isTrue(Modifier.isPrivate(candidate.getModifier()),
                        "Field '" + collaboratingFieldName + "' on '"
                                + destination.getFullyQualifiedTypeName()
                                + "' must be private");
                Validate.notNull(
                        MemberFindingUtils.getAnnotationOfType(
                                candidate.getAnnotations(), AUTOWIRED),
                        "Field '" + collaboratingFieldName + "' on '"
                                + destination.getFullyQualifiedTypeName()
                                + "' must be @Autowired");
                // It's ok, so we can move onto the new field
                continue;
            }

            // Create field and add it to the ITD, if it hasn't already been
            if (!fields.contains(fieldSymbolName)) {
                // Must make the field
                final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
                annotations.add(new AnnotationMetadataBuilder(AUTOWIRED));
                builder.addField(new FieldMetadataBuilder(getId(),
                        Modifier.PRIVATE, annotations, fieldSymbolName,
                        collaboratorType));
                fields.add(fieldSymbolName);
            }
        }
    }

    private void addEmbeddedClassFieldMutatorMethodsToBuilder(
            final EmbeddedHolder embeddedHolder) {
        final JavaType embeddedFieldType = embeddedHolder.getEmbeddedField()
                .getFieldType();
        final JavaType[] parameterTypes = { embeddedFieldType,
                JavaType.INT_PRIMITIVE };
        final List<JavaSymbolName> parameterNames = Arrays.asList(OBJ_SYMBOL,
                INDEX_SYMBOL);

        for (final FieldMetadata field : embeddedHolder.getFields()) {
            final String initializer = getFieldInitializer(field, null);
            final JavaSymbolName fieldMutatorMethodName = BeanInfoUtils
                    .getMutatorMethodName(field.getFieldName());

            final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
            bodyBuilder.append(getFieldValidationBody(field, initializer,
                    fieldMutatorMethodName, false));

            final JavaSymbolName embeddedClassMethodName = getEmbeddedFieldMutatorMethodName(
                    embeddedHolder.getEmbeddedField().getFieldName(),
                    field.getFieldName());
            if (governorHasMethod(embeddedClassMethodName, parameterTypes)) {
                // Method found in governor so do not create method in ITD
                continue;
            }

            builder.addMethod(new MethodMetadataBuilder(getId(),
                    Modifier.PUBLIC, embeddedClassMethodName,
                    JavaType.VOID_PRIMITIVE, AnnotatedJavaType
                            .convertFromJavaTypes(parameterTypes),
                    parameterNames, bodyBuilder));
        }
    }

    private JavaSymbolName getCollaboratingFieldName(final JavaType entity) {
        return new JavaSymbolName(
                StringUtils.uncapitalize(getCollaboratingType(entity)
                        .getSimpleTypeName()));
    }

    private JavaType getCollaboratingType(final JavaType entity) {
        return new JavaType(entity.getFullyQualifiedTypeName() + "DataOnDemand");
    }

    private String getColumnPrecisionAndScaleBody(final FieldMetadata field,
            final Map<String, Object> values, final String suffix) {
        if (values == null || !values.containsKey("precision")) {
            return InvocableMemberBodyBuilder.getInstance().getOutput();
        }

        final String fieldName = field.getFieldName().getSymbolName();
        final JavaType fieldType = field.getFieldType();

        final Integer precision = (Integer) values.get("precision");
        Integer scale = (Integer) values.get("scale");
        scale = scale == null ? 0 : scale;
        final BigDecimal maxValue = new BigDecimal(StringUtils.rightPad("9",
                precision - scale, '9')
                + "."
                + StringUtils.rightPad("9", scale, '9'));

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        if (fieldType.equals(BIG_DECIMAL)) {
            bodyBuilder.appendFormalLine("if (" + fieldName + ".compareTo(new "
                    + BIG_DECIMAL.getSimpleTypeName() + "(\"" + maxValue
                    + "\")) == 1) {");
            bodyBuilder.indent();
            bodyBuilder.appendFormalLine(fieldName + " = new "
                    + BIG_DECIMAL.getSimpleTypeName() + "(\"" + maxValue
                    + "\");");
        }
        else {
            bodyBuilder.appendFormalLine("if (" + fieldName + " > "
                    + maxValue.doubleValue() + suffix + ") {");
            bodyBuilder.indent();
            bodyBuilder.appendFormalLine(fieldName + " = "
                    + maxValue.doubleValue() + suffix + ";");
        }

        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("}");

        return bodyBuilder.getOutput();
    }

    /**
     * Adds the @org.springframework.stereotype.Component annotation to the
     * type, unless it already exists.
     * 
     * @return the annotation is already exists or will be created, or null if
     *         it will not be created (required)
     */
    public AnnotationMetadata getComponentAnnotation() {
        if (governorTypeDetails.getAnnotation(COMPONENT) != null) {
            return null;
        }
        final AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(
                COMPONENT);
        return annotationBuilder.build();
    }

    /**
     * @return the "data" field to use, which is either provided by the user or
     *         produced on demand (never returns null)
     */
    private FieldMetadataBuilder getDataField() {
        final List<JavaType> parameterTypes = Arrays.asList(entity);
        final JavaType listType = new JavaType(
                LIST.getFullyQualifiedTypeName(), 0, DataType.TYPE, null,
                parameterTypes);

        int index = -1;
        while (true) {
            // Compute the required field name
            index++;

            // The type parameters to be used by the field type
            final JavaSymbolName fieldName = new JavaSymbolName("data"
                    + StringUtils.repeat("_", index));
            dataFieldName = fieldName;
            final FieldMetadata candidate = governorTypeDetails
                    .getField(fieldName);
            if (candidate != null) {
                // Verify if candidate is suitable
                if (!Modifier.isPrivate(candidate.getModifier())) {
                    // Candidate is not private, so we might run into naming
                    // clashes if someone subclasses this (therefore go onto the
                    // next possible name)
                    continue;
                }

                if (!candidate.getFieldType().equals(listType)) {
                    // Candidate isn't a java.util.List<theEntity>, so it isn't
                    // suitable
                    // The equals method also verifies type params are present
                    continue;
                }

                // If we got this far, we found a valid candidate
                // We don't check if there is a corresponding initializer, but
                // we assume the user knows what they're doing and have made one
                return new FieldMetadataBuilder(candidate);
            }

            // Candidate not found, so let's create one
            return new FieldMetadataBuilder(getId(), Modifier.PRIVATE,
                    new ArrayList<AnnotationMetadataBuilder>(), fieldName,
                    listType);
        }
    }

    private JavaSymbolName getDataFieldName() {
        return dataFieldName;
    }

    private String getDecimalMinAndDecimalMaxBody(final FieldMetadata field,
            final AnnotationMetadata decimalMinAnnotation,
            final AnnotationMetadata decimalMaxAnnotation, final String suffix) {
        final String fieldName = field.getFieldName().getSymbolName();
        final JavaType fieldType = field.getFieldType();

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

        if (decimalMinAnnotation != null && decimalMaxAnnotation == null) {
            final String minValue = (String) decimalMinAnnotation.getAttribute(
                    VALUE).getValue();

            if (fieldType.equals(BIG_DECIMAL)) {
                bodyBuilder.appendFormalLine("if (" + fieldName
                        + ".compareTo(new " + BIG_DECIMAL.getSimpleTypeName()
                        + "(\"" + minValue + "\")) == -1) {");
                bodyBuilder.indent();
                bodyBuilder.appendFormalLine(fieldName + " = new "
                        + BIG_DECIMAL.getSimpleTypeName() + "(\"" + minValue
                        + "\");");
            }
            else {
                bodyBuilder.appendFormalLine("if (" + fieldName + " < "
                        + minValue + suffix + ") {");
                bodyBuilder.indent();
                bodyBuilder.appendFormalLine(fieldName + " = " + minValue
                        + suffix + ";");
            }

            bodyBuilder.indentRemove();
            bodyBuilder.appendFormalLine("}");
        }
        else if (decimalMinAnnotation == null && decimalMaxAnnotation != null) {
            final String maxValue = (String) decimalMaxAnnotation.getAttribute(
                    VALUE).getValue();

            if (fieldType.equals(BIG_DECIMAL)) {
                bodyBuilder.appendFormalLine("if (" + fieldName
                        + ".compareTo(new " + BIG_DECIMAL.getSimpleTypeName()
                        + "(\"" + maxValue + "\")) == 1) {");
                bodyBuilder.indent();
                bodyBuilder.appendFormalLine(fieldName + " = new "
                        + BIG_DECIMAL.getSimpleTypeName() + "(\"" + maxValue
                        + "\");");
            }
            else {
                bodyBuilder.appendFormalLine("if (" + fieldName + " > "
                        + maxValue + suffix + ") {");
                bodyBuilder.indent();
                bodyBuilder.appendFormalLine(fieldName + " = " + maxValue
                        + suffix + ";");
            }

            bodyBuilder.indentRemove();
            bodyBuilder.appendFormalLine("}");
        }
        else if (decimalMinAnnotation != null && decimalMaxAnnotation != null) {
            final String minValue = (String) decimalMinAnnotation.getAttribute(
                    VALUE).getValue();
            final String maxValue = (String) decimalMaxAnnotation.getAttribute(
                    VALUE).getValue();
            Validate.isTrue(
                    Double.parseDouble(maxValue) >= Double
                            .parseDouble(minValue),
                    "The value of @DecimalMax must be greater or equal to the value of @DecimalMin for field "
                            + fieldName);

            if (fieldType.equals(BIG_DECIMAL)) {
                bodyBuilder.appendFormalLine("if (" + fieldName
                        + ".compareTo(new " + BIG_DECIMAL.getSimpleTypeName()
                        + "(\"" + minValue + "\")) == -1 || " + fieldName
                        + ".compareTo(new " + BIG_DECIMAL.getSimpleTypeName()
                        + "(\"" + maxValue + "\")) == 1) {");
                bodyBuilder.indent();
                bodyBuilder.appendFormalLine(fieldName + " = new "
                        + BIG_DECIMAL.getSimpleTypeName() + "(\"" + maxValue
                        + "\");");
            }
            else {
                bodyBuilder.appendFormalLine("if (" + fieldName + " < "
                        + minValue + suffix + " || " + fieldName + " > "
                        + maxValue + suffix + ") {");
                bodyBuilder.indent();
                bodyBuilder.appendFormalLine(fieldName + " = " + maxValue
                        + suffix + ";");
            }

            bodyBuilder.indentRemove();
            bodyBuilder.appendFormalLine("}");
        }

        return bodyBuilder.getOutput();
    }

    private String getDigitsBody(final FieldMetadata field,
            final AnnotationMetadata digitsAnnotation, final String suffix) {
        final String fieldName = field.getFieldName().getSymbolName();
        final JavaType fieldType = field.getFieldType();

        final Integer integerValue = (Integer) digitsAnnotation.getAttribute(
                new JavaSymbolName("integer")).getValue();
        final Integer fractionValue = (Integer) digitsAnnotation.getAttribute(
                new JavaSymbolName("fraction")).getValue();
        final BigDecimal maxValue = new BigDecimal(StringUtils.rightPad("9",
                integerValue, '9')
                + "."
                + StringUtils.rightPad("9", fractionValue, '9'));

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        if (fieldType.equals(BIG_DECIMAL)) {
            bodyBuilder.appendFormalLine("if (" + fieldName + ".compareTo(new "
                    + BIG_DECIMAL.getSimpleTypeName() + "(\"" + maxValue
                    + "\")) == 1) {");
            bodyBuilder.indent();
            bodyBuilder.appendFormalLine(fieldName + " = new "
                    + BIG_DECIMAL.getSimpleTypeName() + "(\"" + maxValue
                    + "\");");
        }
        else {
            bodyBuilder.appendFormalLine("if (" + fieldName + " > "
                    + maxValue.doubleValue() + suffix + ") {");
            bodyBuilder.indent();
            bodyBuilder.appendFormalLine(fieldName + " = "
                    + maxValue.doubleValue() + suffix + ";");
        }

        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("}");

        return bodyBuilder.getOutput();
    }

    private MethodMetadataBuilder getEmbeddedClassMutatorMethod(
            final EmbeddedHolder embeddedHolder) {
        final JavaSymbolName methodName = getEmbeddedFieldMutatorMethodName(embeddedHolder
                .getEmbeddedField().getFieldName());
        final JavaType[] parameterTypes = { entity, JavaType.INT_PRIMITIVE };

        // Locate user-defined method
        if (governorHasMethod(methodName, parameterTypes)) {
            // Method found in governor so do not create method in ITD
            return null;
        }

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

        // Create constructor for embedded class
        final JavaType embeddedFieldType = embeddedHolder.getEmbeddedField()
                .getFieldType();
        builder.getImportRegistrationResolver().addImport(embeddedFieldType);
        bodyBuilder.appendFormalLine(embeddedFieldType.getSimpleTypeName()
                + " embeddedClass = new "
                + embeddedFieldType.getSimpleTypeName() + "();");
        for (final FieldMetadata field : embeddedHolder.getFields()) {
            bodyBuilder.appendFormalLine(getEmbeddedFieldMutatorMethodName(
                    embeddedHolder.getEmbeddedField().getFieldName(),
                    field.getFieldName()).getSymbolName()
                    + "(embeddedClass, " + INDEX_VAR + ");");
        }
        bodyBuilder.appendFormalLine(OBJ_VAR + "."
                + embeddedHolder.getEmbeddedMutatorMethodName()
                + "(embeddedClass);");

        final List<JavaSymbolName> parameterNames = Arrays.asList(OBJ_SYMBOL,
                INDEX_SYMBOL);

        return new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
                JavaType.VOID_PRIMITIVE,
                AnnotatedJavaType.convertFromJavaTypes(parameterTypes),
                parameterNames, bodyBuilder);
    }

    private JavaSymbolName getEmbeddedFieldMutatorMethodName(
            final JavaSymbolName fieldName) {
        return BeanInfoUtils.getMutatorMethodName(fieldName);
    }

    private JavaSymbolName getEmbeddedFieldMutatorMethodName(
            final JavaSymbolName embeddedFieldName,
            final JavaSymbolName fieldName) {
        return getEmbeddedFieldMutatorMethodName(new JavaSymbolName(
                embeddedFieldName.getSymbolName()
                        + StringUtils.capitalize(fieldName.getSymbolName())));
    }

    private MethodMetadataBuilder getEmbeddedIdMutatorMethod() {
        if (!hasEmbeddedIdentifier()) {
            return null;
        }

        final JavaSymbolName embeddedIdMutator = embeddedIdHolder
                .getEmbeddedIdMutator();
        final JavaSymbolName methodName = getEmbeddedIdMutatorMethodName();
        final JavaType[] parameterTypes = { entity, JavaType.INT_PRIMITIVE };

        // Locate user-defined method
        if (governorHasMethod(methodName, parameterTypes)) {
            // Method found in governor so do not create method in ITD
            return null;
        }

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

        // Create constructor for embedded id class
        final JavaType embeddedIdFieldType = embeddedIdHolder
                .getEmbeddedIdField().getFieldType();
        builder.getImportRegistrationResolver().addImport(embeddedIdFieldType);

        final StringBuilder sb = new StringBuilder();
        final List<FieldMetadata> identifierFields = embeddedIdHolder
                .getIdFields();
        for (int i = 0, n = identifierFields.size(); i < n; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            final FieldMetadata field = identifierFields.get(i);
            final String fieldName = field.getFieldName().getSymbolName();
            final JavaType fieldType = field.getFieldType();
            builder.getImportRegistrationResolver().addImport(fieldType);
            final String initializer = getFieldInitializer(field, null);
            bodyBuilder.append(getFieldValidationBody(field, initializer, null,
                    true));
            sb.append(fieldName);
        }
        bodyBuilder.appendFormalLine("");
        bodyBuilder.appendFormalLine(embeddedIdFieldType.getSimpleTypeName()
                + " embeddedIdClass = new "
                + embeddedIdFieldType.getSimpleTypeName() + "(" + sb.toString()
                + ");");
        bodyBuilder.appendFormalLine(OBJ_VAR + "." + embeddedIdMutator
                + "(embeddedIdClass);");

        final List<JavaSymbolName> parameterNames = Arrays.asList(OBJ_SYMBOL,
                INDEX_SYMBOL);

        return new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
                JavaType.VOID_PRIMITIVE,
                AnnotatedJavaType.convertFromJavaTypes(parameterTypes),
                parameterNames, bodyBuilder);
    }

    private JavaSymbolName getEmbeddedIdMutatorMethodName() {
        final List<JavaSymbolName> fieldNames = new ArrayList<JavaSymbolName>();
        for (final FieldMetadata field : fieldInitializers.keySet()) {
            fieldNames.add(field.getFieldName());
        }

        int index = -1;
        JavaSymbolName embeddedIdField;
        while (true) {
            // Compute the required field name
            index++;
            embeddedIdField = new JavaSymbolName("embeddedIdClass"
                    + StringUtils.repeat("_", index));
            if (!fieldNames.contains(embeddedIdField)) {
                // Found a usable name
                break;
            }
        }
        return BeanInfoUtils.getMutatorMethodName(embeddedIdField);
    }

    public JavaType getEntityType() {
        return entity;
    }

    private String getFieldInitializer(final FieldMetadata field,
            final DataOnDemandMetadata collaboratingMetadata) {
        final JavaType fieldType = field.getFieldType();
        final String fieldName = field.getFieldName().getSymbolName();
        String initializer = "null";
        final String fieldInitializer = field.getFieldInitializer();
        final Set<Object> fieldCustomDataKeys = field.getCustomData().keySet();

        // Date fields included for DataNucleus (
        if (fieldType.equals(DATE)) {
            if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(),
                    PAST) != null) {
                builder.getImportRegistrationResolver().addImport(DATE);
                initializer = "new Date(new Date().getTime() - 10000000L)";
            }
            else if (MemberFindingUtils.getAnnotationOfType(
                    field.getAnnotations(), FUTURE) != null) {
                builder.getImportRegistrationResolver().addImport(DATE);
                initializer = "new Date(new Date().getTime() + 10000000L)";
            }
            else {
                builder.getImportRegistrationResolver().addImports(CALENDAR,
                        GREGORIAN_CALENDAR);
                initializer = "new GregorianCalendar(Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH), Calendar.getInstance().get(Calendar.DAY_OF_MONTH), Calendar.getInstance().get(Calendar.HOUR_OF_DAY), Calendar.getInstance().get(Calendar.MINUTE), Calendar.getInstance().get(Calendar.SECOND) + new Double(Math.random() * 1000).intValue()).getTime()";
            }
        }
        else if (fieldType.equals(CALENDAR)) {
            builder.getImportRegistrationResolver().addImports(CALENDAR,
                    GREGORIAN_CALENDAR);

            final String calendarString = "new GregorianCalendar(Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH), Calendar.getInstance().get(Calendar.DAY_OF_MONTH)";
            if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(),
                    PAST) != null) {
                initializer = calendarString + " - 1)";
            }
            else if (MemberFindingUtils.getAnnotationOfType(
                    field.getAnnotations(), FUTURE) != null) {
                initializer = calendarString + " + 1)";
            }
            else {
                initializer = "Calendar.getInstance()";
            }
        }
        else if (fieldType.equals(STRING)) {
            if (fieldInitializer != null && fieldInitializer.contains("\"")) {
                final int offset = fieldInitializer.indexOf("\"");
                initializer = fieldInitializer.substring(offset + 1,
                        fieldInitializer.lastIndexOf("\""));
            }
            else {
                initializer = fieldName;
            }

            if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(),
                    VALIDATOR_CONSTRAINTS_EMAIL) != null
                    || fieldName.toLowerCase().contains("email")) {
                initializer = "\"foo\" + " + INDEX_VAR + " + \"@bar.com\"";
            }
            else {
                int maxLength = Integer.MAX_VALUE;

                // Check for @Size
                final AnnotationMetadata sizeAnnotation = MemberFindingUtils
                        .getAnnotationOfType(field.getAnnotations(), SIZE);
                if (sizeAnnotation != null) {
                    final AnnotationAttributeValue<?> maxValue = sizeAnnotation
                            .getAttribute(MAX_SYMBOL);
                    if (maxValue != null) {
                        validateNumericAnnotationAttribute(fieldName, "@Size",
                                "max", maxValue.getValue());
                        maxLength = ((Integer) maxValue.getValue()).intValue();
                    }
                    final AnnotationAttributeValue<?> minValue = sizeAnnotation
                            .getAttribute(MIN_SYMBOL);
                    if (minValue != null) {
                        validateNumericAnnotationAttribute(fieldName, "@Size",
                                "min", minValue.getValue());
                        final int minLength = ((Integer) minValue.getValue())
                                .intValue();
                        Validate.isTrue(
                                maxLength >= minLength,
                                "@Size attribute 'max' must be greater than 'min' for field '"
                                        + fieldName + "' in "
                                        + entity.getFullyQualifiedTypeName());
                        if (initializer.length() + 2 < minLength) {
                            initializer = String
                                    .format("%1$-" + (minLength - 2) + "s",
                                            initializer).replace(' ', 'x');
                        }
                    }
                }
                else {
                    if (field.getCustomData().keySet()
                            .contains(CustomDataKeys.COLUMN_FIELD)) {
                        @SuppressWarnings("unchecked")
                        final Map<String, Object> columnValues = (Map<String, Object>) field
                                .getCustomData().get(
                                        CustomDataKeys.COLUMN_FIELD);
                        if (columnValues.keySet().contains("length")) {
                            final Object lengthValue = columnValues
                                    .get("length");
                            validateNumericAnnotationAttribute(fieldName,
                                    "@Column", "length", lengthValue);
                            maxLength = ((Integer) lengthValue).intValue();
                        }
                    }
                }

                switch (maxLength) {
                case 0:
                    initializer = "\"\"";
                    break;
                case 1:
                    initializer = "String.valueOf(" + INDEX_VAR + ")";
                    break;
                case 2:
                    initializer = "\"" + initializer.charAt(0) + "\" + "
                            + INDEX_VAR;
                    break;
                default:
                    if (initializer.length() + 2 > maxLength) {
                        initializer = "\""
                                + initializer.substring(0, maxLength - 2)
                                + "_\" + " + INDEX_VAR;
                    }
                    else {
                        initializer = "\"" + initializer + "_\" + " + INDEX_VAR;
                    }
                }
            }
        }
        else if (fieldType.equals(new JavaType(STRING
                .getFullyQualifiedTypeName(), 1, DataType.TYPE, null, null))) {
            initializer = StringUtils.defaultIfEmpty(fieldInitializer,
                    "{ \"Y\", \"N\" }");
        }
        else if (fieldType.equals(JavaType.BOOLEAN_OBJECT)) {
            initializer = StringUtils.defaultIfEmpty(fieldInitializer,
                    "Boolean.TRUE");
        }
        else if (fieldType.equals(JavaType.BOOLEAN_PRIMITIVE)) {
            initializer = StringUtils.defaultIfEmpty(fieldInitializer, "true");
        }
        else if (fieldType.equals(JavaType.INT_OBJECT)) {
            initializer = StringUtils.defaultIfEmpty(fieldInitializer,
                    "new Integer(" + INDEX_VAR + ")");
        }
        else if (fieldType.equals(JavaType.INT_PRIMITIVE)) {
            initializer = StringUtils.defaultIfEmpty(fieldInitializer,
                    INDEX_VAR);
        }
        else if (fieldType
                .equals(new JavaType(JavaType.INT_OBJECT
                        .getFullyQualifiedTypeName(), 1, DataType.PRIMITIVE,
                        null, null))) {
            initializer = StringUtils.defaultIfEmpty(fieldInitializer, "{ "
                    + INDEX_VAR + ", " + INDEX_VAR + " }");
        }
        else if (fieldType.equals(JavaType.DOUBLE_OBJECT)) {
            initializer = StringUtils.defaultIfEmpty(fieldInitializer,
                    "new Integer(" + INDEX_VAR + ").doubleValue()"); // Auto-boxed
        }
        else if (fieldType.equals(JavaType.DOUBLE_PRIMITIVE)) {
            initializer = StringUtils.defaultIfEmpty(fieldInitializer,
                    "new Integer(" + INDEX_VAR + ").doubleValue()");
        }
        else if (fieldType
                .equals(new JavaType(JavaType.DOUBLE_OBJECT
                        .getFullyQualifiedTypeName(), 1, DataType.PRIMITIVE,
                        null, null))) {
            initializer = StringUtils.defaultIfEmpty(fieldInitializer,
                    "{ new Integer(" + INDEX_VAR
                            + ").doubleValue(), new Integer(" + INDEX_VAR
                            + ").doubleValue() }");
        }
        else if (fieldType.equals(JavaType.FLOAT_OBJECT)) {
            initializer = StringUtils.defaultIfEmpty(fieldInitializer,
                    "new Integer(" + INDEX_VAR + ").floatValue()"); // Auto-boxed
        }
        else if (fieldType.equals(JavaType.FLOAT_PRIMITIVE)) {
            initializer = StringUtils.defaultIfEmpty(fieldInitializer,
                    "new Integer(" + INDEX_VAR + ").floatValue()");
        }
        else if (fieldType
                .equals(new JavaType(JavaType.FLOAT_OBJECT
                        .getFullyQualifiedTypeName(), 1, DataType.PRIMITIVE,
                        null, null))) {
            initializer = StringUtils.defaultIfEmpty(fieldInitializer,
                    "{ new Integer(" + INDEX_VAR
                            + ").floatValue(), new Integer(" + INDEX_VAR
                            + ").floatValue() }");
        }
        else if (fieldType.equals(JavaType.LONG_OBJECT)) {
            initializer = StringUtils.defaultIfEmpty(fieldInitializer,
                    "new Integer(" + INDEX_VAR + ").longValue()"); // Auto-boxed
        }
        else if (fieldType.equals(JavaType.LONG_PRIMITIVE)) {
            initializer = StringUtils.defaultIfEmpty(fieldInitializer,
                    "new Integer(" + INDEX_VAR + ").longValue()");
        }
        else if (fieldType
                .equals(new JavaType(JavaType.LONG_OBJECT
                        .getFullyQualifiedTypeName(), 1, DataType.PRIMITIVE,
                        null, null))) {
            initializer = StringUtils.defaultIfEmpty(fieldInitializer,
                    "{ new Integer(" + INDEX_VAR
                            + ").longValue(), new Integer(" + INDEX_VAR
                            + ").longValue() }");
        }
        else if (fieldType.equals(JavaType.SHORT_OBJECT)) {
            initializer = StringUtils.defaultIfEmpty(fieldInitializer,
                    "new Integer(" + INDEX_VAR + ").shortValue()"); // Auto-boxed
        }
        else if (fieldType.equals(JavaType.SHORT_PRIMITIVE)) {
            initializer = StringUtils.defaultIfEmpty(fieldInitializer,
                    "new Integer(" + INDEX_VAR + ").shortValue()");
        }
        else if (fieldType
                .equals(new JavaType(JavaType.SHORT_OBJECT
                        .getFullyQualifiedTypeName(), 1, DataType.PRIMITIVE,
                        null, null))) {
            initializer = StringUtils.defaultIfEmpty(fieldInitializer,
                    "{ new Integer(" + INDEX_VAR
                            + ").shortValue(), new Integer(" + INDEX_VAR
                            + ").shortValue() }");
        }
        else if (fieldType.equals(JavaType.CHAR_OBJECT)) {
            initializer = StringUtils.defaultIfEmpty(fieldInitializer,
                    "new Character('N')");
        }
        else if (fieldType.equals(JavaType.CHAR_PRIMITIVE)) {
            initializer = StringUtils.defaultIfEmpty(fieldInitializer, "'N'");
        }
        else if (fieldType
                .equals(new JavaType(JavaType.CHAR_OBJECT
                        .getFullyQualifiedTypeName(), 1, DataType.PRIMITIVE,
                        null, null))) {
            initializer = StringUtils.defaultIfEmpty(fieldInitializer,
                    "{ 'Y', 'N' }");
        }
        else if (fieldType.equals(BIG_DECIMAL)) {
            builder.getImportRegistrationResolver().addImport(BIG_DECIMAL);
            initializer = BIG_DECIMAL.getSimpleTypeName() + ".valueOf("
                    + INDEX_VAR + ")";
        }
        else if (fieldType.equals(BIG_INTEGER)) {
            builder.getImportRegistrationResolver().addImport(BIG_INTEGER);
            initializer = BIG_INTEGER.getSimpleTypeName() + ".valueOf("
                    + INDEX_VAR + ")";
        }
        else if (fieldType.equals(JavaType.BYTE_OBJECT)) {
            initializer = "new Byte("
                    + StringUtils.defaultIfEmpty(fieldInitializer, "\"1\"")
                    + ")";
        }
        else if (fieldType.equals(JavaType.BYTE_PRIMITIVE)) {
            initializer = "new Byte("
                    + StringUtils.defaultIfEmpty(fieldInitializer, "\"1\"")
                    + ").byteValue()";
        }
        else if (fieldType.equals(JavaType.BYTE_ARRAY_PRIMITIVE)) {
            initializer = StringUtils.defaultIfEmpty(fieldInitializer,
                    "String.valueOf(" + INDEX_VAR + ").getBytes()");
        }
        else if (fieldType.equals(entity)) {
            // Avoid circular references (ROO-562)
            initializer = OBJ_VAR;
        }
        else if (fieldCustomDataKeys.contains(CustomDataKeys.ENUMERATED_FIELD)) {
            builder.getImportRegistrationResolver().addImport(fieldType);
            initializer = fieldType.getSimpleTypeName()
                    + ".class.getEnumConstants()[0]";
        }
        else if (collaboratingMetadata != null
                && collaboratingMetadata.getEntityType() != null) {
            requiredDataOnDemandCollaborators.add(fieldType);
            initializer = getFieldInitializerForRelatedEntity(field,
                    collaboratingMetadata, fieldCustomDataKeys);
        }

        return initializer;
    }

    private String getFieldInitializerForRelatedEntity(
            final FieldMetadata field,
            final DataOnDemandMetadata collaboratingMetadata,
            final Set<?> fieldCustomDataKeys) {
        // To avoid circular references, we don't try to set nullable fields
        final boolean nullableField = field.getAnnotation(NOT_NULL) == null;
        if (nullableField) {
            return null;
        }
        final String collaboratingFieldName = getCollaboratingFieldName(
                field.getFieldType()).getSymbolName();
        if (fieldCustomDataKeys.contains(CustomDataKeys.ONE_TO_ONE_FIELD)) {
            // We try to keep the same ID (ROO-568)
            return collaboratingFieldName
                    + "."
                    + collaboratingMetadata.getSpecificPersistentEntityMethod()
                            .getMethodName().getSymbolName() + "(" + INDEX_VAR
                    + ")";
        }
        return collaboratingFieldName
                + "."
                + collaboratingMetadata.getRandomPersistentEntityMethod()
                        .getMethodName().getSymbolName() + "()";
    }

    private List<MethodMetadataBuilder> getFieldMutatorMethods() {
        final List<MethodMetadataBuilder> fieldMutatorMethods = new ArrayList<MethodMetadataBuilder>();
        final List<JavaSymbolName> parameterNames = Arrays.asList(OBJ_SYMBOL,
                INDEX_SYMBOL);
        final JavaType[] parameterTypes = { entity, JavaType.INT_PRIMITIVE };

        for (final Map.Entry<FieldMetadata, String> entry : fieldInitializers
                .entrySet()) {
            final FieldMetadata field = entry.getKey();
            final JavaSymbolName mutatorName = BeanInfoUtils
                    .getMutatorMethodName(field.getFieldName());

            // Locate user-defined method
            if (governorHasMethod(mutatorName, parameterTypes)) {
                // Method found in governor so do not create method in ITD
                continue;
            }

            // Method not on governor so need to create it
            final String initializer = entry.getValue();
            if (!StringUtils.isBlank(initializer)) {
                final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
                bodyBuilder.append(getFieldValidationBody(field, initializer,
                        mutatorName, false));

                fieldMutatorMethods.add(new MethodMetadataBuilder(getId(),
                        Modifier.PUBLIC, mutatorName, JavaType.VOID_PRIMITIVE,
                        AnnotatedJavaType.convertFromJavaTypes(parameterTypes),
                        parameterNames, bodyBuilder));
            }
        }

        return fieldMutatorMethods;
    }

    private String getFieldValidationBody(final FieldMetadata field,
            final String initializer, final JavaSymbolName mutatorName,
            final boolean isFieldOfEmbeddableType) {
        final String fieldName = field.getFieldName().getSymbolName();
        final JavaType fieldType = field.getFieldType();

        String suffix = "";
        if (fieldType.equals(JavaType.LONG_OBJECT)
                || fieldType.equals(JavaType.LONG_PRIMITIVE)) {
            suffix = "L";
        }
        else if (fieldType.equals(JavaType.FLOAT_OBJECT)
                || fieldType.equals(JavaType.FLOAT_PRIMITIVE)) {
            suffix = "F";
        }
        else if (fieldType.equals(JavaType.DOUBLE_OBJECT)
                || fieldType.equals(JavaType.DOUBLE_PRIMITIVE)) {
            suffix = "D";
        }

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine(getTypeStr(fieldType) + " " + fieldName
                + " = " + initializer + ";");

        if (fieldType.equals(JavaType.STRING)) {
            boolean isUnique = isFieldOfEmbeddableType;
            @SuppressWarnings("unchecked")
            final Map<String, Object> values = (Map<String, Object>) field
                    .getCustomData().get(CustomDataKeys.COLUMN_FIELD);
            if (!isUnique && values != null && values.containsKey("unique")) {
                isUnique = (Boolean) values.get("unique");
            }

            // Check for @Size or @Column with length attribute
            final AnnotationMetadata sizeAnnotation = MemberFindingUtils
                    .getAnnotationOfType(field.getAnnotations(), SIZE);
            if (sizeAnnotation != null
                    && sizeAnnotation.getAttribute(MAX_SYMBOL) != null) {
                final Integer maxValue = (Integer) sizeAnnotation.getAttribute(
                        MAX_SYMBOL).getValue();
                bodyBuilder.appendFormalLine("if (" + fieldName
                        + ".length() > " + maxValue + ") {");
                bodyBuilder.indent();
                if (isUnique) {
                    bodyBuilder.appendFormalLine(fieldName
                            + " = new Random().nextInt(10) + " + fieldName
                            + ".substring(1, " + maxValue + ");");
                }
                else {
                    bodyBuilder.appendFormalLine(fieldName + " = " + fieldName
                            + ".substring(0, " + maxValue + ");");
                }
                bodyBuilder.indentRemove();
                bodyBuilder.appendFormalLine("}");
            }
            else if (sizeAnnotation == null && values != null) {
                if (values.containsKey("length")) {
                    final Integer lengthValue = (Integer) values.get("length");
                    bodyBuilder.appendFormalLine("if (" + fieldName
                            + ".length() > " + lengthValue + ") {");
                    bodyBuilder.indent();
                    if (isUnique) {
                        bodyBuilder.appendFormalLine(fieldName
                                + " = new Random().nextInt(10) + " + fieldName
                                + ".substring(1, " + lengthValue + ");");
                    }
                    else {
                        bodyBuilder.appendFormalLine(fieldName + " = "
                                + fieldName + ".substring(0, " + lengthValue
                                + ");");
                    }
                    bodyBuilder.indentRemove();
                    bodyBuilder.appendFormalLine("}");
                }
            }
        }
        else if (JdkJavaType.isDecimalType(fieldType)) {
            // Check for @Digits, @DecimalMax, @DecimalMin
            final AnnotationMetadata digitsAnnotation = MemberFindingUtils
                    .getAnnotationOfType(field.getAnnotations(), DIGITS);
            final AnnotationMetadata decimalMinAnnotation = MemberFindingUtils
                    .getAnnotationOfType(field.getAnnotations(), DECIMAL_MIN);
            final AnnotationMetadata decimalMaxAnnotation = MemberFindingUtils
                    .getAnnotationOfType(field.getAnnotations(), DECIMAL_MAX);

            if (digitsAnnotation != null) {
                bodyBuilder.append(getDigitsBody(field, digitsAnnotation,
                        suffix));
            }
            else if (decimalMinAnnotation != null
                    || decimalMaxAnnotation != null) {
                bodyBuilder.append(getDecimalMinAndDecimalMaxBody(field,
                        decimalMinAnnotation, decimalMaxAnnotation, suffix));
            }
            else if (field.getCustomData().keySet()
                    .contains(CustomDataKeys.COLUMN_FIELD)) {
                @SuppressWarnings("unchecked")
                final Map<String, Object> values = (Map<String, Object>) field
                        .getCustomData().get(CustomDataKeys.COLUMN_FIELD);
                bodyBuilder.append(getColumnPrecisionAndScaleBody(field,
                        values, suffix));
            }
        }
        else if (JdkJavaType.isIntegerType(fieldType)) {
            // Check for @Min and @Max
            bodyBuilder.append(getMinAndMaxBody(field, suffix));
        }

        if (mutatorName != null) {
            bodyBuilder.appendFormalLine(OBJ_VAR + "."
                    + mutatorName.getSymbolName() + "(" + fieldName + ");");
        }

        return bodyBuilder.getOutput();
    }

    /**
     * Returns the DoD type's "void init()" method (existing or generated)
     * 
     * @param findEntriesMethodAdditions (required)
     * @param persistMethodAdditions (required)
     * @param flushAdditions (required)
     * @return never <code>null</code>
     */
    private MethodMetadataBuilder getInitMethod(final int quantity,
            final MemberTypeAdditions findEntriesMethodAdditions,
            final MemberTypeAdditions persistMethodAdditions,
            final MemberTypeAdditions flushAdditions) {
        // Method definition to find or build
        final JavaSymbolName methodName = new JavaSymbolName("init");
        final JavaType[] parameterTypes = {};
        final List<JavaSymbolName> parameterNames = Collections
                .<JavaSymbolName> emptyList();
        final JavaType returnType = JavaType.VOID_PRIMITIVE;

        // Locate user-defined method
        final MethodMetadata userMethod = getGovernorMethod(methodName,
                parameterTypes);
        if (userMethod != null) {
            Validate.isTrue(
                    userMethod.getReturnType().equals(returnType),
                    "Method '" + methodName + "' on '" + destination
                            + "' must return '"
                            + returnType.getNameIncludingTypeParameters() + "'");
            return new MethodMetadataBuilder(userMethod);
        }

        // Create the method body
        builder.getImportRegistrationResolver().addImports(ARRAY_LIST,
                ITERATOR, CONSTRAINT_VIOLATION_EXCEPTION, CONSTRAINT_VIOLATION);

        findEntriesMethodAdditions
                .copyAdditionsTo(builder, governorTypeDetails);
        persistMethodAdditions.copyAdditionsTo(builder, governorTypeDetails);

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        final String dataField = getDataFieldName().getSymbolName();
        bodyBuilder.appendFormalLine("int from = 0;");
        bodyBuilder.appendFormalLine("int to = 10;");
        bodyBuilder.appendFormalLine(dataField + " = "
                + findEntriesMethodAdditions.getMethodCall() + ";");
        bodyBuilder.appendFormalLine("if (" + dataField + " == null) {");
        bodyBuilder.indent();
        bodyBuilder
                .appendFormalLine("throw new IllegalStateException(\"Find entries implementation for '"
                        + entity.getSimpleTypeName()
                        + "' illegally returned null\");");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("}");
        bodyBuilder.appendFormalLine("if (!" + dataField + ".isEmpty()) {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine("return;");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("}");
        bodyBuilder.appendFormalLine("");
        bodyBuilder.appendFormalLine(dataField + " = new ArrayList<"
                + entity.getSimpleTypeName() + ">();");
        bodyBuilder.appendFormalLine("for (int i = 0; i < " + quantity
                + "; i++) {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine(entity.getSimpleTypeName() + " " + OBJ_VAR
                + " = "
                + newTransientEntityMethod.getMethodName().getSymbolName()
                + "(i);");
        bodyBuilder.appendFormalLine("try {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine(persistMethodAdditions.getMethodCall()
                + ";");
        bodyBuilder.indentRemove();
        bodyBuilder
                .appendFormalLine("} catch (ConstraintViolationException e) {");
        bodyBuilder.indent();
        bodyBuilder
                .appendFormalLine("StringBuilder msg = new StringBuilder();");
        bodyBuilder
                .appendFormalLine("for (Iterator<ConstraintViolation<?>> iter = e.getConstraintViolations().iterator(); iter.hasNext();) {");
        bodyBuilder.indent();
        bodyBuilder
                .appendFormalLine("ConstraintViolation<?> cv = iter.next();");
        bodyBuilder
                .appendFormalLine("msg.append(\"[\").append(cv.getConstraintDescriptor()).append(\":\").append(cv.getMessage()).append(\"=\").append(cv.getInvalidValue()).append(\"]\");");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("}");
        bodyBuilder
                .appendFormalLine("throw new RuntimeException(msg.toString(), e);");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("}");
        if (flushAdditions != null) {
            bodyBuilder.appendFormalLine(flushAdditions.getMethodCall() + ";");
            flushAdditions.copyAdditionsTo(builder, governorTypeDetails);
        }
        bodyBuilder.appendFormalLine(dataField + ".add(" + OBJ_VAR + ");");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("}");

        // Create the method
        return new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
                returnType,
                AnnotatedJavaType.convertFromJavaTypes(parameterTypes),
                parameterNames, bodyBuilder);
    }

    private String getMinAndMaxBody(final FieldMetadata field,
            final String suffix) {
        final String fieldName = field.getFieldName().getSymbolName();
        final JavaType fieldType = field.getFieldType();

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

        final AnnotationMetadata minAnnotation = MemberFindingUtils
                .getAnnotationOfType(field.getAnnotations(), MIN);
        final AnnotationMetadata maxAnnotation = MemberFindingUtils
                .getAnnotationOfType(field.getAnnotations(), MAX);
        if (minAnnotation != null && maxAnnotation == null) {
            final Number minValue = (Number) minAnnotation.getAttribute(VALUE)
                    .getValue();

            if (fieldType.equals(BIG_INTEGER)) {
                bodyBuilder.appendFormalLine("if (" + fieldName
                        + ".compareTo(new " + BIG_INTEGER.getSimpleTypeName()
                        + "(\"" + minValue + "\")) == -1) {");
                bodyBuilder.indent();
                bodyBuilder.appendFormalLine(fieldName + " = new "
                        + BIG_INTEGER.getSimpleTypeName() + "(\"" + minValue
                        + "\");");
            }
            else {
                bodyBuilder.appendFormalLine("if (" + fieldName + " < "
                        + minValue + suffix + ") {");
                bodyBuilder.indent();
                bodyBuilder.appendFormalLine(fieldName + " = " + minValue
                        + suffix + ";");
            }

            bodyBuilder.indentRemove();
            bodyBuilder.appendFormalLine("}");
        }
        else if (minAnnotation == null && maxAnnotation != null) {
            final Number maxValue = (Number) maxAnnotation.getAttribute(VALUE)
                    .getValue();

            if (fieldType.equals(BIG_INTEGER)) {
                bodyBuilder.appendFormalLine("if (" + fieldName
                        + ".compareTo(new " + BIG_INTEGER.getSimpleTypeName()
                        + "(\"" + maxValue + "\")) == 1) {");
                bodyBuilder.indent();
                bodyBuilder.appendFormalLine(fieldName + " = new "
                        + BIG_INTEGER.getSimpleTypeName() + "(\"" + maxValue
                        + "\");");
            }
            else {
                bodyBuilder.appendFormalLine("if (" + fieldName + " > "
                        + maxValue + suffix + ") {");
                bodyBuilder.indent();
                bodyBuilder.appendFormalLine(fieldName + " = " + maxValue
                        + suffix + ";");
            }

            bodyBuilder.indentRemove();
            bodyBuilder.appendFormalLine("}");
        }
        else if (minAnnotation != null && maxAnnotation != null) {
            final Number minValue = (Number) minAnnotation.getAttribute(VALUE)
                    .getValue();
            final Number maxValue = (Number) maxAnnotation.getAttribute(VALUE)
                    .getValue();
            Validate.isTrue(maxValue.longValue() >= minValue.longValue(),
                    "The value of @Max must be greater or equal to the value of @Min for field "
                            + fieldName);

            if (fieldType.equals(BIG_INTEGER)) {
                bodyBuilder.appendFormalLine("if (" + fieldName
                        + ".compareTo(new " + BIG_INTEGER.getSimpleTypeName()
                        + "(\"" + minValue + "\")) == -1 || " + fieldName
                        + ".compareTo(new " + BIG_INTEGER.getSimpleTypeName()
                        + "(\"" + maxValue + "\")) == 1) {");
                bodyBuilder.indent();
                bodyBuilder.appendFormalLine(fieldName + " = new "
                        + BIG_INTEGER.getSimpleTypeName() + "(\"" + maxValue
                        + "\");");
            }
            else {
                bodyBuilder.appendFormalLine("if (" + fieldName + " < "
                        + minValue + suffix + " || " + fieldName + " > "
                        + maxValue + suffix + ") {");
                bodyBuilder.indent();
                bodyBuilder.appendFormalLine(fieldName + " = " + maxValue
                        + suffix + ";");
            }

            bodyBuilder.indentRemove();
            bodyBuilder.appendFormalLine("}");
        }

        return bodyBuilder.getOutput();
    }

    /**
     * @return the "modifyEntity(Entity):boolean" method (never returns null)
     */
    public MethodMetadata getModifyMethod() {
        return modifyMethod;
    }

    /**
     * @return the "getNewTransientEntity(int index):Entity" method (never
     *         returns null)
     */
    public MethodMetadata getNewTransientEntityMethod() {
        return newTransientEntityMethod;
    }

    /**
     * @return the "getRandomEntity():Entity" method (never returns null)
     */
    public MethodMetadata getRandomPersistentEntityMethod() {
        return randomPersistentEntityMethod;
    }

    private FieldMetadataBuilder getRndField() {
        int index = -1;
        while (true) {
            // Compute the required field name
            index++;
            final JavaSymbolName fieldName = new JavaSymbolName("rnd"
                    + StringUtils.repeat("_", index));
            rndFieldName = fieldName;
            final FieldMetadata candidate = governorTypeDetails
                    .getField(fieldName);
            if (candidate != null) {
                // Verify if candidate is suitable
                if (!Modifier.isPrivate(candidate.getModifier())) {
                    // Candidate is not private, so we might run into naming
                    // clashes if someone subclasses this (therefore go onto the
                    // next possible name)
                    continue;
                }
                if (!candidate.getFieldType().equals(RANDOM)) {
                    // Candidate isn't a java.util.Random, so it isn't suitable
                    continue;
                }
                // If we got this far, we found a valid candidate
                // We don't check if there is a corresponding initializer, but
                // we assume the user knows what they're doing and have made one
                return new FieldMetadataBuilder(candidate);
            }

            // Candidate not found, so let's create one
            builder.getImportRegistrationResolver().addImports(RANDOM,
                    SECURE_RANDOM);

            final FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(
                    getId());
            fieldBuilder.setModifier(Modifier.PRIVATE);
            fieldBuilder.setFieldName(fieldName);
            fieldBuilder.setFieldType(RANDOM);
            fieldBuilder.setFieldInitializer("new SecureRandom()");
            return fieldBuilder;
        }
    }

    /**
     * @return the "rnd" field to use, which is either provided by the user or
     *         produced on demand (never returns null)
     */
    private JavaSymbolName getRndFieldName() {
        return rndFieldName;
    }

    /**
     * @return the "getSpecificEntity(int):Entity" method (never returns null)
     */
    private MethodMetadata getSpecificPersistentEntityMethod() {
        return specificPersistentEntityMethod;
    }

    private String getTypeStr(final JavaType fieldType) {
        builder.getImportRegistrationResolver().addImport(fieldType);

        final String arrayStr = fieldType.isArray() ? "[]" : "";
        String typeStr = fieldType.getSimpleTypeName();

        if (fieldType.getFullyQualifiedTypeName().equals(
                JavaType.FLOAT_PRIMITIVE.getFullyQualifiedTypeName())
                && fieldType.isPrimitive()) {
            typeStr = "float" + arrayStr;
        }
        else if (fieldType.getFullyQualifiedTypeName().equals(
                JavaType.DOUBLE_PRIMITIVE.getFullyQualifiedTypeName())
                && fieldType.isPrimitive()) {
            typeStr = "double" + arrayStr;
        }
        else if (fieldType.getFullyQualifiedTypeName().equals(
                JavaType.INT_PRIMITIVE.getFullyQualifiedTypeName())
                && fieldType.isPrimitive()) {
            typeStr = "int" + arrayStr;
        }
        else if (fieldType.getFullyQualifiedTypeName().equals(
                JavaType.SHORT_PRIMITIVE.getFullyQualifiedTypeName())
                && fieldType.isPrimitive()) {
            typeStr = "short" + arrayStr;
        }
        else if (fieldType.getFullyQualifiedTypeName().equals(
                JavaType.BYTE_PRIMITIVE.getFullyQualifiedTypeName())
                && fieldType.isPrimitive()) {
            typeStr = "byte" + arrayStr;
        }
        else if (fieldType.getFullyQualifiedTypeName().equals(
                JavaType.CHAR_PRIMITIVE.getFullyQualifiedTypeName())
                && fieldType.isPrimitive()) {
            typeStr = "char" + arrayStr;
        }
        else if (fieldType.equals(new JavaType(STRING
                .getFullyQualifiedTypeName(), 1, DataType.TYPE, null, null))) {
            typeStr = "String[]";
        }
        return typeStr;
    }

    public boolean hasEmbeddedIdentifier() {
        return embeddedIdHolder != null;
    }

    private void setModifyMethod() {
        // Method definition to find or build
        final JavaSymbolName methodName = new JavaSymbolName("modify"
                + entity.getSimpleTypeName());
        final JavaType parameterType = entity;
        final List<JavaSymbolName> parameterNames = Arrays.asList(OBJ_SYMBOL);
        final JavaType returnType = JavaType.BOOLEAN_PRIMITIVE;

        // Locate user-defined method
        final MethodMetadata userMethod = getGovernorMethod(methodName,
                parameterType);
        if (userMethod != null) {
            Validate.isTrue(
                    userMethod.getReturnType().equals(returnType),
                    "Method '" + methodName + "' on '" + destination
                            + "' must return '"
                            + returnType.getNameIncludingTypeParameters() + "'");
            modifyMethod = userMethod;
            return;
        }

        // Create method
        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine("return false;");

        final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
                getId(), Modifier.PUBLIC, methodName, returnType,
                AnnotatedJavaType.convertFromJavaTypes(parameterType),
                parameterNames, bodyBuilder);
        builder.addMethod(methodBuilder);
        modifyMethod = methodBuilder.build();
    }

    private void setNewTransientEntityMethod() {
        // Method definition to find or build
        final JavaSymbolName methodName = new JavaSymbolName("getNewTransient"
                + entity.getSimpleTypeName());

        final JavaType parameterType = JavaType.INT_PRIMITIVE;
        final List<JavaSymbolName> parameterNames = Arrays.asList(INDEX_SYMBOL);

        // Locate user-defined method
        final MethodMetadata userMethod = getGovernorMethod(methodName,
                parameterType);
        if (userMethod != null) {
            Validate.isTrue(
                    userMethod.getReturnType().equals(entity),
                    "Method '" + methodName + "' on '" + destination
                            + "' must return '"
                            + entity.getNameIncludingTypeParameters() + "'");
            newTransientEntityMethod = userMethod;
            return;
        }

        // Create method
        builder.getImportRegistrationResolver().addImport(entity);

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine(entity.getSimpleTypeName() + " " + OBJ_VAR
                + " = new " + entity.getSimpleTypeName() + "();");

        // Create the composite key embedded id method call if required
        if (hasEmbeddedIdentifier()) {
            bodyBuilder.appendFormalLine(getEmbeddedIdMutatorMethodName() + "("
                    + OBJ_VAR + ", " + INDEX_VAR + ");");
        }

        // Create a mutator method call for each embedded class
        for (final EmbeddedHolder embeddedHolder : embeddedHolders) {
            bodyBuilder
                    .appendFormalLine(getEmbeddedFieldMutatorMethodName(embeddedHolder
                            .getEmbeddedField().getFieldName())
                            + "("
                            + OBJ_VAR
                            + ", " + INDEX_VAR + ");");
        }

        // Create mutator method calls for each entity field
        for (final FieldMetadata field : fieldInitializers.keySet()) {
            final JavaSymbolName mutatorName = BeanInfoUtils
                    .getMutatorMethodName(field);
            bodyBuilder.appendFormalLine(mutatorName.getSymbolName() + "("
                    + OBJ_VAR + ", " + INDEX_VAR + ");");
        }

        bodyBuilder.appendFormalLine("return " + OBJ_VAR + ";");

        final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
                getId(), Modifier.PUBLIC, methodName, entity,
                AnnotatedJavaType.convertFromJavaTypes(parameterType),
                parameterNames, bodyBuilder);
        builder.addMethod(methodBuilder);
        newTransientEntityMethod = methodBuilder.build();
    }

    /**
     * @return the "getRandomEntity():Entity" method (never returns null)
     */
    private void setRandomPersistentEntityMethod() {
        // Method definition to find or build
        final JavaSymbolName methodName = new JavaSymbolName("getRandom"
                + entity.getSimpleTypeName());

        // Locate user-defined method
        final MethodMetadata userMethod = getGovernorMethod(methodName);
        if (userMethod != null) {
            Validate.isTrue(
                    userMethod.getReturnType().equals(entity),
                    "Method '" + methodName + "' on '" + destination
                            + "' must return '"
                            + entity.getNameIncludingTypeParameters() + "'");
            randomPersistentEntityMethod = userMethod;
            return;
        }

        builder.getImportRegistrationResolver().addImport(identifierType);

        // Create method
        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine("init();");
        bodyBuilder.appendFormalLine(entity.getSimpleTypeName() + " " + OBJ_VAR
                + " = " + getDataFieldName().getSymbolName() + ".get("
                + getRndFieldName().getSymbolName() + ".nextInt("
                + getDataField().getFieldName().getSymbolName() + ".size()));");
        bodyBuilder.appendFormalLine(identifierType.getSimpleTypeName()
                + " id = " + OBJ_VAR + "."
                + identifierAccessor.getMethodName().getSymbolName() + "();");
        bodyBuilder.appendFormalLine("return " + findMethod.getMethodCall()
                + ";");

        findMethod.copyAdditionsTo(builder, governorTypeDetails);

        final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
                getId(), Modifier.PUBLIC, methodName, entity, bodyBuilder);
        builder.addMethod(methodBuilder);
        randomPersistentEntityMethod = methodBuilder.build();
    }

    private void setSpecificPersistentEntityMethod() {
        // Method definition to find or build
        final JavaSymbolName methodName = new JavaSymbolName("getSpecific"
                + entity.getSimpleTypeName());
        final JavaType parameterType = JavaType.INT_PRIMITIVE;
        final List<JavaSymbolName> parameterNames = Arrays.asList(INDEX_SYMBOL);

        // Locate user-defined method
        final MethodMetadata userMethod = getGovernorMethod(methodName,
                parameterType);
        if (userMethod != null) {
            Validate.isTrue(
                    userMethod.getReturnType().equals(entity),
                    "Method '" + methodName + "' on '" + destination
                            + "' must return '"
                            + entity.getNameIncludingTypeParameters() + "'");
            specificPersistentEntityMethod = userMethod;
            return;
        }

        builder.getImportRegistrationResolver().addImport(identifierType);

        // Create method
        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine("init();");
        bodyBuilder.appendFormalLine("if (" + INDEX_VAR + " < 0) {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine(INDEX_VAR + " = 0;");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("}");
        bodyBuilder.appendFormalLine("if (" + INDEX_VAR + " > ("
                + getDataFieldName().getSymbolName() + ".size() - 1)) {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine(INDEX_VAR + " = "
                + getDataFieldName().getSymbolName() + ".size() - 1;");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("}");
        bodyBuilder.appendFormalLine(entity.getSimpleTypeName() + " " + OBJ_VAR
                + " = " + getDataFieldName().getSymbolName() + ".get("
                + INDEX_VAR + ");");
        bodyBuilder.appendFormalLine(identifierType.getSimpleTypeName()
                + " id = " + OBJ_VAR + "."
                + identifierAccessor.getMethodName().getSymbolName() + "();");
        bodyBuilder.appendFormalLine("return " + findMethod.getMethodCall()
                + ";");

        findMethod.copyAdditionsTo(builder, governorTypeDetails);

        final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
                getId(), Modifier.PUBLIC, methodName, entity,
                AnnotatedJavaType.convertFromJavaTypes(parameterType),
                parameterNames, bodyBuilder);
        builder.addMethod(methodBuilder);
        specificPersistentEntityMethod = methodBuilder.build();
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

    private void validateNumericAnnotationAttribute(final String fieldName,
            final String annotationName, final String attributeName,
            final Object object) {
        Validate.isTrue(NumberUtils.isNumber(object.toString()), annotationName
                + " '" + attributeName + "' attribute for field '" + fieldName
                + "' in backing type " + entity.getFullyQualifiedTypeName()
                + " must be numeric");
    }
}