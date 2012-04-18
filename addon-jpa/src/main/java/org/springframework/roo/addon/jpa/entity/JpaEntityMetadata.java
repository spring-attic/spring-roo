package org.springframework.roo.addon.jpa.entity;

import static org.springframework.roo.model.GoogleJavaType.GAE_DATASTORE_KEY;
import static org.springframework.roo.model.JavaType.LONG_OBJECT;
import static org.springframework.roo.model.JdkJavaType.BIG_DECIMAL;
import static org.springframework.roo.model.JdkJavaType.CALENDAR;
import static org.springframework.roo.model.JpaJavaType.COLUMN;
import static org.springframework.roo.model.JpaJavaType.DISCRIMINATOR_COLUMN;
import static org.springframework.roo.model.JpaJavaType.EMBEDDED_ID;
import static org.springframework.roo.model.JpaJavaType.ENTITY;
import static org.springframework.roo.model.JpaJavaType.GENERATED_VALUE;
import static org.springframework.roo.model.JpaJavaType.GENERATION_TYPE;
import static org.springframework.roo.model.JpaJavaType.ID;
import static org.springframework.roo.model.JpaJavaType.INHERITANCE;
import static org.springframework.roo.model.JpaJavaType.INHERITANCE_TYPE;
import static org.springframework.roo.model.JpaJavaType.MAPPED_SUPERCLASS;
import static org.springframework.roo.model.JpaJavaType.SEQUENCE_GENERATOR;
import static org.springframework.roo.model.JpaJavaType.TABLE;
import static org.springframework.roo.model.JpaJavaType.VERSION;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord;
import org.springframework.roo.addon.jpa.identifier.Identifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.ConstructorMetadataBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.EnumAttributeValue;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.operations.InheritanceType;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataItem;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * The metadata for a JPA entity's *_Roo_Jpa_Entity.aj ITD.
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class JpaEntityMetadata extends
        AbstractItdTypeDetailsProvidingMetadataItem {

    private final JpaEntityAnnotationValues annotationValues;
    private final MemberDetails entityMemberDetails;
    private final Identifier identifier;
    private final boolean isDatabaseDotComEnabled;
    private final boolean isGaeEnabled;
    private final JpaEntityMetadata parent;
    private FieldMetadata identifierField;
    private FieldMetadata versionField;

    /**
     * Constructor
     * 
     * @param metadataIdentificationString the JPA_ID of this
     *            {@link MetadataItem}
     * @param itdName the ITD's {@link JavaType} (required)
     * @param entityPhysicalType the entity's physical type (required)
     * @param parent can be <code>null</code> if none of the governor's
     *            ancestors provide {@link JpaEntityMetadata}
     * @param entityMemberDetails details of the entity's members (required)
     * @param identifier information about the entity's identifier field in the
     *            event that the annotation doesn't provide such information;
     *            can be <code>null</code>
     * @param annotationValues the effective annotation values taking into
     *            account the presence of a {@link RooJpaActiveRecord} and/or
     *            {@link RooJpaEntity} annotation (required)
     */
    public JpaEntityMetadata(final String metadataIdentificationString,
            final JavaType itdName,
            final PhysicalTypeMetadata entityPhysicalType,
            final JpaEntityMetadata parent,
            final MemberDetails entityMemberDetails,
            final Identifier identifier,
            final JpaEntityAnnotationValues annotationValues,
            final boolean isGaeEnabled, final boolean isDatabaseDotComEnabled) {
        super(metadataIdentificationString, itdName, entityPhysicalType);
        Validate.notNull(annotationValues, "Annotation values are required");
        Validate.notNull(entityMemberDetails,
                "Entity MemberDetails are required");

        /*
         * Ideally we'd pass these parameters to the methods below rather than
         * storing them in fields, but this isn't an option due to various calls
         * to the parent entity.
         */
        this.annotationValues = annotationValues;
        this.entityMemberDetails = entityMemberDetails;
        this.identifier = identifier;
        this.parent = parent;
        this.isGaeEnabled = isGaeEnabled;
        this.isDatabaseDotComEnabled = isDatabaseDotComEnabled;

        // Add @Entity or @MappedSuperclass annotation
        builder.addAnnotation(annotationValues.isMappedSuperclass() ? getTypeAnnotation(MAPPED_SUPERCLASS)
                : getEntityAnnotation());

        // Add @Table annotation if required
        builder.addAnnotation(getTableAnnotation());

        // Add @Inheritance annotation if required
        builder.addAnnotation(getInheritanceAnnotation());

        // Add @DiscriminatorColumn if required
        builder.addAnnotation(getDiscriminatorColumnAnnotation());

        // Ensure there's a no-arg constructor (explicit or default)
        builder.addConstructor(getNoArgConstructor());

        // Add identifier field and accessor
        identifierField = getIdentifierField();
        builder.addField(identifierField);
        builder.addMethod(getIdentifierAccessor());
        builder.addMethod(getIdentifierMutator());

        // Add version field and accessor
        versionField = getVersionField();
        builder.addField(versionField);
        builder.addMethod(getVersionAccessor());
        builder.addMethod(getVersionMutator());

        // Build the ITD based on what we added to the builder above
        itdTypeDetails = builder.build();
    }

    private AnnotationMetadata getDiscriminatorColumnAnnotation() {
        if (StringUtils.isNotBlank(annotationValues.getInheritanceType())
                && InheritanceType.SINGLE_TABLE.name().equals(
                        annotationValues.getInheritanceType())) {
            // Theoretically not required based on @DiscriminatorColumn
            // JavaDocs, but Hibernate appears to fail if it's missing
            return getTypeAnnotation(DISCRIMINATOR_COLUMN);
        }
        return null;
    }

    /**
     * Generates the JPA @Entity annotation to be applied to the entity
     * 
     * @return
     */
    private AnnotationMetadata getEntityAnnotation() {
        AnnotationMetadata entityAnnotation = getTypeAnnotation(ENTITY);
        if (entityAnnotation == null) {
            return null;
        }

        if (StringUtils.isNotBlank(annotationValues.getEntityName())) {
            final AnnotationMetadataBuilder entityBuilder = new AnnotationMetadataBuilder(
                    entityAnnotation);
            entityBuilder.addStringAttribute("name",
                    annotationValues.getEntityName());
            entityAnnotation = entityBuilder.build();
        }

        return entityAnnotation;
    }

    /**
     * Locates the identifier accessor method.
     * <p>
     * If {@link #getIdentifierField()} returns a field created by this ITD or
     * if the field is declared within the entity itself, a public accessor will
     * automatically be produced in the declaring class.
     * 
     * @return the accessor (never returns null)
     */
    private MethodMetadataBuilder getIdentifierAccessor() {
        if (parent != null) {
            return parent.getIdentifierAccessor();
        }

        // Locate the identifier field, and compute the name of the accessor
        // that will be produced
        JavaSymbolName requiredAccessorName = BeanInfoUtils
                .getAccessorMethodName(identifierField);

        // See if the user provided the field
        if (!getId().equals(identifierField.getDeclaredByMetadataId())) {
            // Locate an existing accessor
            final MethodMetadata method = entityMemberDetails.getMethod(
                    requiredAccessorName, new ArrayList<JavaType>());
            if (method != null) {
                if (Modifier.isPublic(method.getModifier())) {
                    // Method exists and is public so return it
                    return new MethodMetadataBuilder(method);
                }

                // Method is not public so make the required accessor name
                // unique
                requiredAccessorName = new JavaSymbolName(
                        requiredAccessorName.getSymbolName() + "_");
            }
        }

        // We declared the field in this ITD, so produce a public accessor for
        // it
        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine("return this."
                + identifierField.getFieldName().getSymbolName() + ";");

        return new MethodMetadataBuilder(getId(), Modifier.PUBLIC,
                requiredAccessorName, identifierField.getFieldType(),
                bodyBuilder);
    }

    private String getIdentifierColumn() {
        if (StringUtils.isNotBlank(annotationValues.getIdentifierColumn())) {
            return annotationValues.getIdentifierColumn();
        }
        else if (identifier != null
                && StringUtils.isNotBlank(identifier.getColumnName())) {
            return identifier.getColumnName();
        }
        return "";
    }

    /**
     * Locates the identifier field.
     * <p>
     * If a parent is defined, it must provide the field.
     * <p>
     * If no parent is defined, one will be located or created. Any declared or
     * inherited field which has the {@link javax.persistence.Id @Id} or
     * {@link javax.persistence.EmbeddedId @EmbeddedId} annotation will be taken
     * as the identifier and returned. If no such field is located, a private
     * field will be created as per the details contained in the
     * {@link RooJpaActiveRecord} or {@link RooJpaEntity} annotation, as
     * applicable.
     * 
     * @param parent (can be <code>null</code>)
     * @param project the user's project (required)
     * @param annotationValues
     * @param identifier can be <code>null</code>
     * @return the identifier (never returns null)
     */
    private FieldMetadata getIdentifierField() {
        if (parent != null) {
            final FieldMetadata idField = parent.getIdentifierField();
            if (idField != null) {
                if (MemberFindingUtils.getAnnotationOfType(
                        idField.getAnnotations(), ID) != null) {
                    return idField;
                }
                else if (MemberFindingUtils.getAnnotationOfType(
                        idField.getAnnotations(), EMBEDDED_ID) != null) {
                    return idField;
                }
            }
            return parent.getIdentifierField();
        }

        // Try to locate an existing field with @javax.persistence.Id
        final List<FieldMetadata> idFields = governorTypeDetails
                .getFieldsWithAnnotation(ID);
        if (!idFields.isEmpty()) {
            return getIdentifierField(idFields, ID);
        }

        // Try to locate an existing field with @javax.persistence.EmbeddedId
        final List<FieldMetadata> embeddedIdFields = governorTypeDetails
                .getFieldsWithAnnotation(EMBEDDED_ID);
        if (!embeddedIdFields.isEmpty()) {
            return getIdentifierField(embeddedIdFields, EMBEDDED_ID);
        }

        // Ensure there isn't already a field called "id"; if so, compute a
        // unique name (it's not really a fatal situation at the end of the day)
        final JavaSymbolName idField = governorTypeDetails
                .getUniqueFieldName(getIdentifierFieldName());

        // We need to create one
        final JavaType identifierType = getIdentifierType();

        final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
        final boolean hasIdClass = !(identifierType.isCoreType() || identifierType
                .equals(GAE_DATASTORE_KEY));
        final JavaType annotationType = hasIdClass ? EMBEDDED_ID : ID;
        annotations.add(new AnnotationMetadataBuilder(annotationType));

        // Compute the column name, as required
        if (!hasIdClass) {
            String generationType = isGaeEnabled || isDatabaseDotComEnabled ? "IDENTITY"
                    : "AUTO";

            // ROO-746: Use @GeneratedValue(strategy = GenerationType.TABLE) if
            // the root of the governor declares @Inheritance(strategy =
            // InheritanceType.TABLE_PER_CLASS)
            if ("AUTO".equals(generationType)) {
                AnnotationMetadata inheritance = governorTypeDetails
                        .getAnnotation(INHERITANCE);
                if (inheritance == null) {
                    inheritance = getInheritanceAnnotation();
                }
                if (inheritance != null) {
                    final AnnotationAttributeValue<?> value = inheritance
                            .getAttribute(new JavaSymbolName("strategy"));
                    if (value instanceof EnumAttributeValue) {
                        final EnumAttributeValue enumAttributeValue = (EnumAttributeValue) value;
                        final EnumDetails details = enumAttributeValue
                                .getValue();
                        if (details != null
                                && details.getType().equals(INHERITANCE_TYPE)) {
                            if ("TABLE_PER_CLASS".equals(details.getField()
                                    .getSymbolName())) {
                                generationType = "TABLE";
                            }
                        }
                    }
                }
            }

            final AnnotationMetadataBuilder generatedValueBuilder = new AnnotationMetadataBuilder(
                    GENERATED_VALUE);
            generatedValueBuilder.addEnumAttribute("strategy", new EnumDetails(
                    GENERATION_TYPE, new JavaSymbolName(generationType)));

            if (StringUtils.isNotBlank(annotationValues.getSequenceName())
                    && !(isGaeEnabled || isDatabaseDotComEnabled)) {
                final String sequenceKey = StringUtils.uncapitalize(destination
                        .getSimpleTypeName()) + "Gen";
                generatedValueBuilder.addStringAttribute("generator",
                        sequenceKey);
                final AnnotationMetadataBuilder sequenceGeneratorBuilder = new AnnotationMetadataBuilder(
                        SEQUENCE_GENERATOR);
                sequenceGeneratorBuilder
                        .addStringAttribute("name", sequenceKey);
                sequenceGeneratorBuilder.addStringAttribute("sequenceName",
                        annotationValues.getSequenceName());
                annotations.add(sequenceGeneratorBuilder);
            }
            annotations.add(generatedValueBuilder);

            final String identifierColumn = StringUtils
                    .stripToEmpty(getIdentifierColumn());
            String columnName = idField.getSymbolName();
            if (StringUtils.isNotBlank(identifierColumn)) {
                // User has specified an alternate column name
                columnName = identifierColumn;
            }

            final AnnotationMetadataBuilder columnBuilder = new AnnotationMetadataBuilder(
                    COLUMN);
            columnBuilder.addStringAttribute("name", columnName);
            if (identifier != null
                    && StringUtils.isNotBlank(identifier.getColumnDefinition())) {
                columnBuilder.addStringAttribute("columnDefinition",
                        identifier.getColumnDefinition());
            }

            // Add length attribute for String field
            if (identifier != null && identifier.getColumnSize() > 0
                    && identifier.getColumnSize() < 4000
                    && identifierType.equals(JavaType.STRING)) {
                columnBuilder.addIntegerAttribute("length",
                        identifier.getColumnSize());
            }

            // Add precision and scale attributes for numeric field
            if (identifier != null
                    && identifier.getScale() > 0
                    && (identifierType.equals(JavaType.DOUBLE_OBJECT)
                            || identifierType.equals(JavaType.DOUBLE_PRIMITIVE) || identifierType
                                .equals(BIG_DECIMAL))) {
                columnBuilder.addIntegerAttribute("precision",
                        identifier.getColumnSize());
                columnBuilder.addIntegerAttribute("scale",
                        identifier.getScale());
            }

            annotations.add(columnBuilder);
        }

        return new FieldMetadataBuilder(getId(), Modifier.PRIVATE, annotations,
                idField, identifierType).build();
    }

    private FieldMetadata getIdentifierField(
            final List<FieldMetadata> identifierFields,
            final JavaType identifierType) {
        Validate.isTrue(
                identifierFields.size() == 1,
                "More than one field was annotated with @"
                        + identifierType.getSimpleTypeName() + " in '"
                        + destination.getFullyQualifiedTypeName() + "'");
        return new FieldMetadataBuilder(identifierFields.get(0)).build();
    }

    private String getIdentifierFieldName() {
        if (StringUtils.isNotBlank(annotationValues.getIdentifierField())) {
            return annotationValues.getIdentifierField();
        }
        else if (identifier != null && identifier.getFieldName() != null) {
            return identifier.getFieldName().getSymbolName();
        }
        // Use the default
        return RooJpaEntity.ID_FIELD_DEFAULT;
    }

    /**
     * Locates the identifier mutator method.
     * <p>
     * If {@link #getIdentifierField()} returns a field created by this ITD or
     * if the field is declared within the entity itself, a public mutator will
     * automatically be produced in the declaring class.
     * 
     * @return the mutator (never returns null)
     */
    private MethodMetadataBuilder getIdentifierMutator() {
        // TODO: This is a temporary workaround to support web data binding
        // approaches; to be reviewed more thoroughly in future
        if (parent != null) {
            return parent.getIdentifierMutator();
        }

        // Locate the identifier field, and compute the name of the accessor
        // that will be produced
        JavaSymbolName requiredMutatorName = BeanInfoUtils
                .getMutatorMethodName(identifierField);

        final List<JavaType> parameterTypes = Arrays.asList(identifierField
                .getFieldType());
        final List<JavaSymbolName> parameterNames = Arrays
                .asList(new JavaSymbolName("id"));

        // See if the user provided the field
        if (!getId().equals(identifierField.getDeclaredByMetadataId())) {
            // Locate an existing mutator
            final MethodMetadata method = entityMemberDetails.getMethod(
                    requiredMutatorName, parameterTypes);
            if (method != null) {
                if (Modifier.isPublic(method.getModifier())) {
                    // Method exists and is public so return it
                    return new MethodMetadataBuilder(method);
                }

                // Method is not public so make the required mutator name unique
                requiredMutatorName = new JavaSymbolName(
                        requiredMutatorName.getSymbolName() + "_");
            }
        }

        // We declared the field in this ITD, so produce a public mutator for it
        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine("this."
                + identifierField.getFieldName().getSymbolName() + " = id;");

        return new MethodMetadataBuilder(getId(), Modifier.PUBLIC,
                requiredMutatorName, JavaType.VOID_PRIMITIVE,
                AnnotatedJavaType.convertFromJavaTypes(parameterTypes),
                parameterNames, bodyBuilder);
    }

    /**
     * Returns the {@link JavaType} of the identifier field
     * 
     * @param annotationValues the values of the {@link RooJpaEntity} annotation
     *            (required)
     * @param identifier can be <code>null</code>
     * @return a non-<code>null</code> type
     */
    private JavaType getIdentifierType() {
        if (isDatabaseDotComEnabled) {
            return JavaType.STRING;
        }
        if (annotationValues.getIdentifierType() != null) {
            return annotationValues.getIdentifierType();
        }
        else if (identifier != null && identifier.getFieldType() != null) {
            return identifier.getFieldType();
        }
        // Use the default
        return LONG_OBJECT;
    }

    /**
     * Returns the JPA @Inheritance annotation to be applied to the entity, if
     * applicable
     * 
     * @param annotationValues the values of the {@link RooJpaEntity} annotation
     *            (required)
     * @return <code>null</code> if it's already present or not required
     */
    private AnnotationMetadata getInheritanceAnnotation() {
        if (governorTypeDetails.getAnnotation(INHERITANCE) != null) {
            return null;
        }
        if (StringUtils.isNotBlank(annotationValues.getInheritanceType())) {
            final AnnotationMetadataBuilder inheritanceBuilder = new AnnotationMetadataBuilder(
                    INHERITANCE);
            inheritanceBuilder.addEnumAttribute("strategy",
                    new EnumDetails(INHERITANCE_TYPE, new JavaSymbolName(
                            annotationValues.getInheritanceType())));
            return inheritanceBuilder.build();
        }
        return null;
    }

    /**
     * Locates the no-arg constructor for this class, if available.
     * <p>
     * If a class defines a no-arg constructor, it is returned (irrespective of
     * access modifiers).
     * <p>
     * Otherwise, and if there is at least one other constructor declared in the
     * source file, this method creates one with public access.
     * 
     * @return <code>null</code> if no constructor is to be produced
     */
    private ConstructorMetadataBuilder getNoArgConstructor() {
        // Search for an existing constructor
        final ConstructorMetadata existingExplicitConstructor = governorTypeDetails
                .getDeclaredConstructor(null);
        if (existingExplicitConstructor != null) {
            // Found an existing no-arg constructor on this class, so return it
            return new ConstructorMetadataBuilder(existingExplicitConstructor);
        }

        // To get this far, the user did not define a no-arg constructor
        if (governorTypeDetails.getDeclaredConstructors().isEmpty()) {
            // Java creates the default constructor => no need to add one
            return null;
        }

        // Create the constructor
        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine("super();");

        final ConstructorMetadataBuilder constructorBuilder = new ConstructorMetadataBuilder(
                getId());
        constructorBuilder.setBodyBuilder(bodyBuilder);
        constructorBuilder.setModifier(Modifier.PUBLIC);
        return constructorBuilder;
    }

    /**
     * Generates the JPA @Table annotation to be applied to the entity
     * 
     * @param annotationValues
     * @return
     */
    private AnnotationMetadata getTableAnnotation() {
        final AnnotationMetadata tableAnnotation = getTypeAnnotation(TABLE);
        if (tableAnnotation == null) {
            return null;
        }
        final String catalog = annotationValues.getCatalog();
        final String schema = annotationValues.getSchema();
        final String table = annotationValues.getTable();
        if (StringUtils.isNotBlank(table) || StringUtils.isNotBlank(schema)
                || StringUtils.isNotBlank(catalog)) {
            final AnnotationMetadataBuilder tableBuilder = new AnnotationMetadataBuilder(
                    tableAnnotation);
            if (StringUtils.isNotBlank(catalog)) {
                tableBuilder.addStringAttribute("catalog", catalog);
            }
            if (StringUtils.isNotBlank(schema)) {
                tableBuilder.addStringAttribute("schema", schema);
            }
            if (StringUtils.isNotBlank(table)) {
                tableBuilder.addStringAttribute("name", table);
            }
            return tableBuilder.build();
        }
        return null;
    }

    /**
     * Locates the version accessor method.
     * <p>
     * If {@link #getVersionField()} returns a field created by this ITD or if
     * the version field is declared within the entity itself, a public accessor
     * will automatically be produced in the declaring class.
     * 
     * @param memberDetails
     * @return the version accessor (may return null if there is no version
     *         field declared in this class)
     */
    private MethodMetadataBuilder getVersionAccessor() {
        if (versionField == null) {
            // There's no version field, so there certainly won't be an accessor
            // for it
            return null;
        }

        if (parent != null) {
            final FieldMetadata result = parent.getVersionField();
            if (result != null) {
                // It's the parent's responsibility to provide the accessor, not
                // ours
                return parent.getVersionAccessor();
            }
        }

        // Compute the name of the accessor that will be produced
        JavaSymbolName requiredAccessorName = BeanInfoUtils
                .getAccessorMethodName(versionField);

        // See if the user provided the field
        if (!getId().equals(versionField.getDeclaredByMetadataId())) {
            // Locate an existing accessor
            final MethodMetadata method = entityMemberDetails.getMethod(
                    requiredAccessorName, new ArrayList<JavaType>(), getId());
            if (method != null) {
                if (Modifier.isPublic(method.getModifier())) {
                    // Method exists and is public so return it
                    return new MethodMetadataBuilder(method);
                }

                // Method is not public so make the required accessor name
                // unique
                requiredAccessorName = new JavaSymbolName(
                        requiredAccessorName.getSymbolName() + "_");
            }
        }

        // We declared the field in this ITD, so produce a public accessor for
        // it
        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine("return this."
                + versionField.getFieldName().getSymbolName() + ";");

        return new MethodMetadataBuilder(getId(), Modifier.PUBLIC,
                requiredAccessorName, versionField.getFieldType(), bodyBuilder);
    }

    /**
     * Locates the version field.
     * <p>
     * If a parent is defined, it may provide the field.
     * <p>
     * If no parent is defined, one may be located or created. Any declared or
     * inherited field which is annotated with javax.persistence.Version will be
     * taken as the version and returned. If no such field is located, a private
     * field may be created as per the details contained in
     * {@link RooJpaActiveRecord} or {@link RooJpaEntity} annotation, as
     * applicable.
     * 
     * @return the version field (may be null)
     */
    private FieldMetadata getVersionField() {
        if (parent != null) {
            final FieldMetadata result = parent.getVersionField();
            if (result != null) {
                return result;
            }
        }

        // Try to locate an existing field with @Version
        final List<FieldMetadata> versionFields = governorTypeDetails
                .getFieldsWithAnnotation(VERSION);
        if (!versionFields.isEmpty()) {
            Validate.isTrue(versionFields.size() == 1,
                    "More than 1 field was annotated with @Version in '"
                            + destination.getFullyQualifiedTypeName() + "'");
            return versionFields.get(0);
        }

        // Quit at this stage if the user doesn't want a version field
        final String versionField = annotationValues.getVersionField();
        if ("".equals(versionField)) {
            return null;
        }

        // Ensure there isn't already a field called "version"; if so, compute a
        // unique name (it's not really a fatal situation at the end of the day)
        final JavaSymbolName verField = governorTypeDetails
                .getUniqueFieldName(versionField);

        // We're creating one
        JavaType versionType = annotationValues.getVersionType();
        String versionColumn = StringUtils.defaultIfEmpty(
                annotationValues.getVersionColumn(), verField.getSymbolName());
        if (isDatabaseDotComEnabled) {
            versionType = CALENDAR;
            versionColumn = "lastModifiedDate";
        }

        final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
        annotations.add(new AnnotationMetadataBuilder(VERSION));

        final AnnotationMetadataBuilder columnBuilder = new AnnotationMetadataBuilder(
                COLUMN);
        columnBuilder.addStringAttribute("name", versionColumn);
        annotations.add(columnBuilder);

        return new FieldMetadataBuilder(getId(), Modifier.PRIVATE, annotations,
                verField, versionType).build();
    }

    /**
     * Locates the version mutator method.
     * <p>
     * If {@link #getVersionField()} returns a field created by this ITD or if
     * the version field is declared within the entity itself, a public mutator
     * will automatically be produced in the declaring class.
     * 
     * @return the mutator (may return null if there is no version field
     *         declared in this class)
     */
    private MethodMetadataBuilder getVersionMutator() {
        // TODO: This is a temporary workaround to support web data binding
        // approaches; to be reviewed more thoroughly in future
        if (parent != null) {
            return parent.getVersionMutator();
        }

        // Locate the version field, and compute the name of the mutator that
        // will be produced
        if (versionField == null) {
            // There's no version field, so there certainly won't be a mutator
            // for it
            return null;
        }

        // Compute the name of the mutator that will be produced
        JavaSymbolName requiredMutatorName = BeanInfoUtils
                .getMutatorMethodName(versionField);

        final List<JavaType> parameterTypes = Arrays.asList(versionField
                .getFieldType());
        final List<JavaSymbolName> parameterNames = Arrays
                .asList(new JavaSymbolName("version"));

        // See if the user provided the field
        if (!getId().equals(versionField.getDeclaredByMetadataId())) {
            // Locate an existing mutator
            final MethodMetadata method = entityMemberDetails.getMethod(
                    requiredMutatorName, parameterTypes, getId());
            if (method != null) {
                if (Modifier.isPublic(method.getModifier())) {
                    // Method exists and is public so return it
                    return new MethodMetadataBuilder(method);
                }

                // Method is not public so make the required mutator name unique
                requiredMutatorName = new JavaSymbolName(
                        requiredMutatorName.getSymbolName() + "_");
            }
        }

        // We declared the field in this ITD, so produce a public mutator for it
        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine("this."
                + versionField.getFieldName().getSymbolName() + " = version;");

        return new MethodMetadataBuilder(getId(), Modifier.PUBLIC,
                requiredMutatorName, JavaType.VOID_PRIMITIVE,
                AnnotatedJavaType.convertFromJavaTypes(parameterTypes),
                parameterNames, bodyBuilder);
    }
}