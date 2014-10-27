package org.springframework.roo.addon.dbre;

import static org.springframework.roo.model.JdkJavaType.CALENDAR;
import static org.springframework.roo.model.JdkJavaType.DATE;
import static org.springframework.roo.model.JdkJavaType.SET;
import static org.springframework.roo.model.JpaJavaType.CASCADE_TYPE;
import static org.springframework.roo.model.JpaJavaType.COLUMN;
import static org.springframework.roo.model.JpaJavaType.JOIN_COLUMN;
import static org.springframework.roo.model.JpaJavaType.JOIN_COLUMNS;
import static org.springframework.roo.model.JpaJavaType.JOIN_TABLE;
import static org.springframework.roo.model.JpaJavaType.LOB;
import static org.springframework.roo.model.JpaJavaType.MANY_TO_MANY;
import static org.springframework.roo.model.JpaJavaType.MANY_TO_ONE;
import static org.springframework.roo.model.JpaJavaType.ONE_TO_MANY;
import static org.springframework.roo.model.JpaJavaType.ONE_TO_ONE;
import static org.springframework.roo.model.JpaJavaType.TEMPORAL;
import static org.springframework.roo.model.JpaJavaType.TEMPORAL_TYPE;
import static org.springframework.roo.model.Jsr303JavaType.NOT_NULL;
import static org.springframework.roo.model.RooJavaType.ROO_TO_STRING;
import static org.springframework.roo.model.SpringJavaType.DATE_TIME_FORMAT;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.jvnet.inflector.Noun;
import org.springframework.roo.addon.dbre.model.CascadeAction;
import org.springframework.roo.addon.dbre.model.Column;
import org.springframework.roo.addon.dbre.model.Database;
import org.springframework.roo.addon.dbre.model.ForeignKey;
import org.springframework.roo.addon.dbre.model.Reference;
import org.springframework.roo.addon.dbre.model.Table;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ArrayAttributeValue;
import org.springframework.roo.classpath.details.annotations.EnumAttributeValue;
import org.springframework.roo.classpath.details.annotations.NestedAnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JdkJavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata for {@link RooDbManaged}.
 * <p>
 * Creates and manages entity relationships, such as many-valued and
 * single-valued associations.
 * <p>
 * One-to-many and one-to-one associations are created based on the following
 * laws:
 * <ul>
 * <li>Primary Key (PK) - Foreign Key (FK) LAW #1: If the foreign key column is
 * part of the primary key (or part of an index) then the relationship between
 * the tables will be one to many (1:M).
 * <li>Primary Key (PK) - Foreign Key (FK) LAW #2: If the foreign key column
 * represents the entire primary key (or the entire index) then the relationship
 * between the tables will be one to one (1:1).
 * </ul>
 * <p>
 * Many-to-many associations are created if a join table is detected. To be
 * identified as a many-to-many join table, the table must have have exactly two
 * primary keys and have exactly two foreign-keys pointing to other entity
 * tables and have no other columns.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class DbreMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

    private static final String CREATED = "created";
    private static final String MAPPED_BY = "mappedBy";
    private static final String NAME = "name";
    private static final String PROVIDES_TYPE_STRING = DbreMetadata.class
            .getName();
    private static final String PROVIDES_TYPE = MetadataIdentificationUtils
            .create(PROVIDES_TYPE_STRING);
    private static final String VALUE = "value";

    private DbManagedAnnotationValues annotationValues;
    private Database database;
    private IdentifierHolder identifierHolder;
    private Iterable<ClassOrInterfaceTypeDetails> managedEntities;
    private ClassOrInterfaceTypeDetailsBuilder updatedGovernorBuilder;
    private FieldMetadata versionField;

    // XXX DiSiD: Move var from method to class (store successive modifications)
    // http://projects.disid.com/issues/7455
    private AnnotationMetadata toStringAnnotation = governorTypeDetails
            .getAnnotation(ROO_TO_STRING);

    public DbreMetadata(final String identifier, final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final DbManagedAnnotationValues annotationValues,
            final IdentifierHolder identifierHolder,
            final FieldMetadata versionField,
            final Iterable<ClassOrInterfaceTypeDetails> managedEntities,
            final Database database) {
        super(identifier, aspectName, governorPhysicalTypeMetadata);
        Validate.isTrue(
                isValid(identifier),
                "Metadata identification string '%s' does not appear to be a valid",
                identifier);
        Validate.notNull(annotationValues, "Annotation values required");
        Validate.notNull(identifierHolder, "Identifier holder required");
        Validate.notNull(managedEntities, "Managed entities required");
        Validate.notNull(database, "Database required");

        this.annotationValues = annotationValues;
        this.identifierHolder = identifierHolder;
        this.versionField = versionField;
        this.managedEntities = managedEntities;
        this.database = database;

        final Table table = this.database.getTable(
                DbreTypeUtils.getTableName(governorTypeDetails),
                DbreTypeUtils.getSchemaName(governorTypeDetails));
        if (table == null) {
            valid = false;
            return;
        }

        // Add fields for many-valued associations with many-to-many
        // multiplicity
        addManyToManyFields(table);

        // Add fields for single-valued associations to other entities that have
        // one-to-one multiplicity
        addOneToOneFields(table);

        // Add fields for many-valued associations with one-to-many multiplicity
        addOneToManyFields(table);

        // Add fields for single-valued associations to other entities that have
        // many-to-one multiplicity
        addManyToOneFields(table);

        // Add remaining fields from columns
        addOtherFields(table);

        // Create a representation of the desired output ITD
        itdTypeDetails = builder.build();
    }

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

    public ClassOrInterfaceTypeDetails getUpdatedGovernor() {
        return updatedGovernorBuilder == null ? null : updatedGovernorBuilder
                .build();
    }

    public boolean isAutomaticallyDelete() {
        return annotationValues.isAutomaticallyDelete();
    }

    private void addCascadeType(
            final AnnotationMetadataBuilder annotationBuilder,
            final CascadeAction onUpdate, final CascadeAction onDelete) {
        final String attributeName = "cascade";
        boolean hasCascadeType = true;
        if (onUpdate == CascadeAction.CASCADE
                && onDelete == CascadeAction.CASCADE) {
            annotationBuilder.addEnumAttribute(attributeName, CASCADE_TYPE,
                    "ALL");
        }
        else if (onUpdate == CascadeAction.CASCADE
                && onDelete != CascadeAction.CASCADE) {
            final List<EnumAttributeValue> arrayValues = new ArrayList<EnumAttributeValue>();
            arrayValues.add(new EnumAttributeValue(new JavaSymbolName(
                    attributeName), new EnumDetails(CASCADE_TYPE,
                    new JavaSymbolName("PERSIST"))));
            arrayValues.add(new EnumAttributeValue(new JavaSymbolName(
                    attributeName), new EnumDetails(CASCADE_TYPE,
                    new JavaSymbolName("MERGE"))));
            annotationBuilder
                    .addAttribute(new ArrayAttributeValue<EnumAttributeValue>(
                            new JavaSymbolName(attributeName), arrayValues));
        }
        else if (onUpdate != CascadeAction.CASCADE
                && onDelete == CascadeAction.CASCADE) {
            annotationBuilder.addEnumAttribute(attributeName,
                    CASCADE_TYPE.getSimpleTypeName(), "REMOVE");
        }
        else {
            hasCascadeType = false;
        }
        if (hasCascadeType) {
            builder.getImportRegistrationResolver().addImport(CASCADE_TYPE);
        }
    }

    private void addManyToManyFields(final Table table) {
        final Map<Table, Integer> owningSideTables = new LinkedHashMap<Table, Integer>();
        final Map<JavaSymbolName, FieldMetadataBuilder> uniqueOwningSideFields = new LinkedHashMap<JavaSymbolName, FieldMetadataBuilder>();
        final Map<JavaSymbolName, FieldMetadataBuilder> uniqueInverseSideFields = new LinkedHashMap<JavaSymbolName, FieldMetadataBuilder>();

        for (final Table joinTable : database.getTables()) {
            if (!joinTable.isJoinTable()) {
                continue;
            }

            final String errMsg = "table in join table '"
                    + joinTable.getName()
                    + "' for many-to-many relationship could not be found. Note that table names are case sensitive in some databases such as MySQL.";
            final Iterator<ForeignKey> iter = joinTable.getImportedKeys()
                    .iterator();

            // First foreign key in set
            final ForeignKey foreignKey1 = iter.next();

            // Second and last foreign key in set
            final ForeignKey foreignKey2 = iter.next();

            final Table owningSideTable = foreignKey1.getForeignTable();
            Validate.notNull(owningSideTable, "Owning-side %s", errMsg);

            final Table inverseSideTable = foreignKey2.getForeignTable();
            Validate.notNull(inverseSideTable, "Inverse-side %s", errMsg);

            final Integer tableCount = owningSideTables
                    .containsKey(owningSideTable) ? owningSideTables
                    .get(owningSideTable) + 1 : 0;
            owningSideTables.put(owningSideTable, tableCount);
            final String fieldSuffix = owningSideTables.get(owningSideTable) > 0 ? String
                    .valueOf(owningSideTables.get(owningSideTable)) : "";

            final boolean sameTable = owningSideTable.equals(inverseSideTable);

            if (owningSideTable.equals(table)) {
                final JavaSymbolName fieldName = new JavaSymbolName(
                        getInflectorPlural(DbreTypeUtils
                                .suggestFieldName(inverseSideTable))
                                + (sameTable ? "1" : fieldSuffix));
                final FieldMetadataBuilder fieldBuilder = getManyToManyOwningSideField(
                        fieldName, joinTable, inverseSideTable,
                        foreignKey1.getOnUpdate(), foreignKey1.getOnDelete());
                uniqueOwningSideFields.put(fieldName, fieldBuilder);
            }

            if (inverseSideTable.equals(table)) {
                final JavaSymbolName fieldName = new JavaSymbolName(
                        getInflectorPlural(DbreTypeUtils
                                .suggestFieldName(owningSideTable))
                                + (sameTable ? "2" : fieldSuffix));
                final JavaSymbolName mappedByFieldName = new JavaSymbolName(
                        getInflectorPlural(DbreTypeUtils
                                .suggestFieldName(inverseSideTable))
                                + (sameTable ? "1" : fieldSuffix));
                final FieldMetadataBuilder fieldBuilder = getManyToManyInverseSideField(
                        fieldName, mappedByFieldName, owningSideTable,
                        foreignKey2.getOnUpdate(), foreignKey2.getOnDelete());
                uniqueInverseSideFields.put(fieldName, fieldBuilder);
            }
        }

        // Add unique owning-side many-to-one fields
        for (final FieldMetadataBuilder fieldBuilder : uniqueOwningSideFields
                .values()) {
            addToBuilder(fieldBuilder);

            // Exclude these fields in @RooToString to avoid circular references
            // - ROO-1399
            excludeFieldsInToStringAnnotation(fieldBuilder.getFieldName()
                    .getSymbolName());
        }
        // Add unique inverse-side many-to-one fields
        for (final FieldMetadataBuilder fieldBuilder : uniqueInverseSideFields
                .values()) {
            addToBuilder(fieldBuilder);

            // Exclude these fields in @RooToString to avoid circular references
            // - ROO-1399
            excludeFieldsInToStringAnnotation(fieldBuilder.getFieldName()
                    .getSymbolName());
        }
    }

    private void addManyToOneFields(final Table table) {
        // Add unique many-to-one fields
        final Map<JavaSymbolName, FieldMetadataBuilder> uniqueFields = new LinkedHashMap<JavaSymbolName, FieldMetadataBuilder>();

        for (final ForeignKey foreignKey : table.getImportedKeys()) {
            final Table foreignTable = foreignKey.getForeignTable();
            if (foreignTable == null || isOneToOne(table, foreignKey)) {
                continue;
            }

            // Assume many-to-one multiplicity
            JavaSymbolName fieldName = null;
            final String foreignTableName = foreignTable.getName();
            final String foreignSchemaName = foreignTable.getSchema().getName();
            if (foreignKey.getReferenceCount() == 1) {
                final Reference reference = foreignKey.getReferences()
                        .iterator().next();
                fieldName = new JavaSymbolName(
                        DbreTypeUtils.suggestFieldName(reference
                                .getLocalColumnName()));
            }
            else {
                final Short keySequence = foreignKey.getKeySequence();
                final String fieldSuffix = keySequence != null
                        && keySequence > 0 ? String.valueOf(keySequence) : "";
                fieldName = new JavaSymbolName(
                        DbreTypeUtils.suggestFieldName(foreignTableName)
                                + fieldSuffix);
            }
            final JavaType fieldType = DbreTypeUtils.findTypeForTableName(
                    managedEntities, foreignTableName, foreignSchemaName);
            Validate.notNull(
                    fieldType,
                    "Attempted to create many-to-one field '%s' in '%s' %s",
                    fieldName,
                    destination.getFullyQualifiedTypeName(),
                    getErrorMsg(foreignTable.getFullyQualifiedTableName(),
                            table.getFullyQualifiedTableName()));

            // Fields are stored in a field-keyed map first before adding them
            // to the builder.
            // This ensures the fields from foreign keys with multiple columns
            // will only get created once.
            final FieldMetadataBuilder fieldBuilder = getOneToOneOrManyToOneField(
                    fieldName, fieldType, foreignKey, MANY_TO_ONE, true);
            uniqueFields.put(fieldName, fieldBuilder);
        }

        for (final FieldMetadataBuilder fieldBuilder : uniqueFields.values()) {
            addToBuilder(fieldBuilder);

            // Exclude these fields in @RooToString to avoid circular references
            // - ROO-1399
            excludeFieldsInToStringAnnotation(fieldBuilder.getFieldName()
                    .getSymbolName());
        }
    }

    private void addOneToManyFields(final Table table) {
        Validate.notNull(table, "Table required");
        if (table.isJoinTable()) {
            return;
        }

        for (final ForeignKey exportedKey : table.getExportedKeys()) {
            final Table exportedKeyForeignTable = exportedKey.getForeignTable();
            Validate.notNull(
                    exportedKeyForeignTable,
                    "Foreign key table for foreign key '%s' in table '%s' does not exist. One-to-many relationship not created",
                    exportedKey.getName(), table.getFullyQualifiedTableName());
            if (exportedKeyForeignTable.isJoinTable()) {
                continue;
            }

            final String foreignTableName = exportedKeyForeignTable.getName();
            final String foreignSchemaName = exportedKeyForeignTable
                    .getSchema().getName();
            final Table foreignTable = database.getTable(foreignTableName,
                    foreignSchemaName);
            Validate.notNull(
                    foreignTable,
                    "Related table '%s' could not be found but was referenced by table '%s'",
                    exportedKeyForeignTable.getFullyQualifiedTableName(),
                    table.getFullyQualifiedTableName());

            if (isOneToOne(foreignTable,
                    foreignTable.getImportedKey(exportedKey.getName()))) {
                continue;
            }

            final Short keySequence = exportedKey.getKeySequence();
            final String fieldSuffix = keySequence != null && keySequence > 0 ? String
                    .valueOf(keySequence) : "";
            JavaSymbolName fieldName = new JavaSymbolName(
                    getInflectorPlural(DbreTypeUtils
                            .suggestFieldName(foreignTableName)) + fieldSuffix);
            JavaSymbolName mappedByFieldName = null;
            if (exportedKey.getReferenceCount() == 1) {
                final Reference reference = exportedKey.getReferences()
                        .iterator().next();
                mappedByFieldName = new JavaSymbolName(
                        DbreTypeUtils.suggestFieldName(reference
                                .getForeignColumnName()));
            }
            else {
                mappedByFieldName = new JavaSymbolName(
                        DbreTypeUtils.suggestFieldName(table) + fieldSuffix);
            }

            // Check for existence of same field - ROO-1691
            while (true) {
                if (!hasFieldInItd(fieldName)) {
                    break;
                }
                fieldName = new JavaSymbolName(fieldName.getSymbolName() + "_");
            }

            final FieldMetadataBuilder fieldBuilder = getOneToManyMappedByField(
                    fieldName, mappedByFieldName, foreignTableName,
                    foreignSchemaName, exportedKey.getOnUpdate(),
                    exportedKey.getOnDelete());
            addToBuilder(fieldBuilder);

            // Exclude these fields in @RooToString to avoid circular references
            // - ROO-1399
            excludeFieldsInToStringAnnotation(fieldBuilder.getFieldName()
                    .getSymbolName());
        }
    }

    private void addOneToOneFields(final Table table) {
        // Add unique one-to-one fields
        final Map<JavaSymbolName, FieldMetadataBuilder> uniqueFields = new LinkedHashMap<JavaSymbolName, FieldMetadataBuilder>();

        for (final ForeignKey foreignKey : table.getImportedKeys()) {
            if (!isOneToOne(table, foreignKey)) {
                continue;
            }

            final Table importedKeyForeignTable = foreignKey.getForeignTable();
            Validate.notNull(
                    importedKeyForeignTable,
                    "Foreign key table for foreign key '%s' in table '%s' does not exist. One-to-one relationship not created",
                    foreignKey.getName(), table.getFullyQualifiedTableName());

            final String foreignTableName = importedKeyForeignTable.getName();
            final String foreignSchemaName = importedKeyForeignTable
                    .getSchema().getName();
            final Short keySequence = foreignKey.getKeySequence();
            final String fieldSuffix = keySequence != null && keySequence > 0 ? String
                    .valueOf(keySequence) : "";
            final JavaSymbolName fieldName = new JavaSymbolName(
                    DbreTypeUtils.suggestFieldName(foreignTableName)
                            + fieldSuffix);
            final JavaType fieldType = DbreTypeUtils.findTypeForTableName(
                    managedEntities, foreignTableName, foreignSchemaName);
            Validate.notNull(
                    fieldType,
                    "Attempted to create one-to-one field '%s' in '%s' %s",
                    fieldName,
                    destination.getFullyQualifiedTypeName(),
                    getErrorMsg(importedKeyForeignTable
                            .getFullyQualifiedTableName(), table
                            .getFullyQualifiedTableName()));

            // Fields are stored in a field-keyed map first before adding them
            // to the builder.
            // This ensures the fields from foreign keys with multiple columns
            // will only get created once.
            final FieldMetadataBuilder fieldBuilder = getOneToOneOrManyToOneField(
                    fieldName, fieldType, foreignKey, ONE_TO_ONE, false);
            uniqueFields.put(fieldName, fieldBuilder);
        }

        for (final FieldMetadataBuilder fieldBuilder : uniqueFields.values()) {
            addToBuilder(fieldBuilder);

            // Exclude these fields in @RooToString to avoid circular references
            // - ROO-1399
            excludeFieldsInToStringAnnotation(fieldBuilder.getFieldName()
                    .getSymbolName());
        }

        // Add one-to-one mapped-by fields
        if (table.isJoinTable()) {
            return;
        }

        for (final ForeignKey exportedKey : table.getExportedKeys()) {
            final Table exportedKeyForeignTable = exportedKey.getForeignTable();
            Validate.notNull(
                    exportedKeyForeignTable,
                    "Foreign key table for foreign key '%s' in table '%s' does not exist. One-to-one relationship not created",
                    exportedKey.getName(), table.getFullyQualifiedTableName());
            if (exportedKeyForeignTable.isJoinTable()) {
                continue;
            }

            final String foreignTableName = exportedKeyForeignTable.getName();
            final String foreignSchemaName = exportedKeyForeignTable
                    .getSchema().getName();
            final Table foreignTable = database.getTable(foreignTableName,
                    foreignSchemaName);
            Validate.notNull(
                    foreignTable,
                    "Related table '%s' could not be found but has a foreign-key reference to table '%s'",
                    exportedKeyForeignTable.getFullyQualifiedTableName(),
                    table.getFullyQualifiedTableName());
            if (!isOneToOne(foreignTable,
                    foreignTable.getImportedKey(exportedKey.getName()))) {
                continue;
            }
            final Short keySequence = exportedKey.getKeySequence();
            final String fieldSuffix = keySequence != null && keySequence > 0 ? String
                    .valueOf(keySequence) : "";
            JavaSymbolName fieldName = new JavaSymbolName(
                    DbreTypeUtils.suggestFieldName(foreignTableName)
                            + fieldSuffix);

            final JavaType fieldType = DbreTypeUtils.findTypeForTableName(
                    managedEntities, foreignTableName, foreignSchemaName);
            Validate.notNull(
                    fieldType,
                    "Attempted to create one-to-one mapped-by field '%s' in '%s' %s",
                    fieldName, destination.getFullyQualifiedTypeName(),
                    getErrorMsg(foreignTable.getFullyQualifiedTableName()));

            // Check for existence of same field - ROO-1691
            while (true) {
                if (!hasFieldInItd(fieldName)) {
                    break;
                }
                fieldName = new JavaSymbolName(fieldName.getSymbolName() + "_");
            }

            final JavaSymbolName mappedByFieldName = new JavaSymbolName(
                    DbreTypeUtils.suggestFieldName(table.getName())
                            + fieldSuffix);

            final FieldMetadataBuilder fieldBuilder = getOneToOneMappedByField(
                    fieldName, fieldType, mappedByFieldName,
                    exportedKey.getOnUpdate(), exportedKey.getOnDelete());
            addToBuilder(fieldBuilder);

            // Exclude these fields in @RooToString to avoid circular references
            // - ROO-1399
            excludeFieldsInToStringAnnotation(fieldBuilder.getFieldName()
                    .getSymbolName());
        }
    }

    private void addOtherFields(final Table table) {
        final Map<JavaSymbolName, FieldMetadataBuilder> uniqueFields = new LinkedHashMap<JavaSymbolName, FieldMetadataBuilder>();

        for (final Column column : table.getColumns()) {
            final String columnName = column.getName();
            JavaSymbolName fieldName = new JavaSymbolName(
                    DbreTypeUtils.suggestFieldName(columnName));

            final boolean isIdField = isIdField(fieldName)
                    || column.isPrimaryKey();
            final boolean isVersionField = isVersionField(fieldName)
                    || (columnName.equals("version") && !database
                            .isDisableVersionFields());
            final boolean isCompositeKeyField = isCompositeKeyField(fieldName);
            final boolean isForeignKey = table
                    .findImportedKeyByLocalColumnName(columnName) != null;
            if (isIdField || isVersionField || isCompositeKeyField
                    || isForeignKey) {
                continue;
            }

            final boolean hasEmbeddedIdField = isEmbeddedIdField(fieldName)
                    && !isCompositeKeyField;
            if (hasEmbeddedIdField) {
                fieldName = governorTypeDetails.getUniqueFieldName(fieldName
                        .getSymbolName());
            }
            final FieldMetadataBuilder fieldBuilder = getField(fieldName,
                    column, table.getName(),
                    table.isIncludeNonPortableAttributes());
            if (fieldBuilder.getFieldType().equals(DATE)
                    && fieldName.getSymbolName().equals(CREATED)) {
                fieldBuilder.setFieldInitializer("new Date()");
            }
            uniqueFields.put(fieldName, fieldBuilder);
        }

        for (final FieldMetadataBuilder fieldBuilder : uniqueFields.values()) {
            addToBuilder(fieldBuilder);
        }
    }

    private void addToBuilder(final FieldMetadataBuilder fieldBuilder) {
        final JavaSymbolName fieldName = fieldBuilder.getFieldName();
        if (hasField(fieldName, fieldBuilder.buildAnnotations())
                || hasFieldInItd(fieldName)) {
            return;
        }

        builder.addField(fieldBuilder);

        // Check for an existing accessor in the governor
        final JavaType fieldType = fieldBuilder.getFieldType();
        builder.addMethod(getAccessorMethod(fieldName, fieldType));

        // Check for an existing mutator in the governor
        builder.addMethod(getMutatorMethod(fieldName, fieldType));
    }

    // XXX DiSiD: Invoke this method when add non other fields to builder
    // http://projects.disid.com/issues/7455
    private void excludeFieldsInToStringAnnotation(final String fieldName) {
        if (toStringAnnotation == null) {
            return;
        }

        final List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
        final List<StringAttributeValue> ignoreFields = new ArrayList<StringAttributeValue>();

        // Copy the existing attributes, excluding the "ignoreFields" attribute
        boolean alreadyAdded = false;
        AnnotationAttributeValue<?> value = toStringAnnotation
                .getAttribute(new JavaSymbolName("excludeFields"));
        if (value == null) {
            value = new ArrayAttributeValue<StringAttributeValue>(
                    new JavaSymbolName("excludeFields"),
                    new ArrayList<StringAttributeValue>());
        }

        // Ensure we have an array of strings
        final String errMsg = "@RooToString attribute 'excludeFields' must be an array of strings";
        Validate.isInstanceOf(ArrayAttributeValue.class, value, errMsg);
        final ArrayAttributeValue<?> arrayVal = (ArrayAttributeValue<?>) value;
        for (final Object obj : arrayVal.getValue()) {
            Validate.isInstanceOf(StringAttributeValue.class, obj, errMsg);
            final StringAttributeValue sv = (StringAttributeValue) obj;
            if (sv.getValue().equals(fieldName)) {
                alreadyAdded = true;
            }
            ignoreFields.add(sv);
        }

        // Add the desired field to ignore to the end
        if (!alreadyAdded) {
            ignoreFields.add(new StringAttributeValue(new JavaSymbolName(
                    "ignored"), fieldName));
        }

        attributes.add(new ArrayAttributeValue<StringAttributeValue>(
                new JavaSymbolName("excludeFields"), ignoreFields));
        final AnnotationMetadataBuilder toStringAnnotationBuilder = new AnnotationMetadataBuilder(
                ROO_TO_STRING, attributes);
        updatedGovernorBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                governorTypeDetails);
        toStringAnnotation = toStringAnnotationBuilder.build();
        updatedGovernorBuilder.updateTypeAnnotation(toStringAnnotation,
                new HashSet<JavaSymbolName>());
    }

    private String getErrorMsg(final String tableName) {
        return String
                .format(" but type for table '%s' could not be found or is not database managed (not annotated with @RooDbManaged)",
                        tableName);
    }

    private String getErrorMsg(final String foreignTableName,
            final String tableName) {
        return getErrorMsg(foreignTableName)
                + String.format(
                        " and table '%s' has a foreign-key reference to table '%s'",
                        tableName, foreignTableName);
    }

    private FieldMetadataBuilder getField(final JavaSymbolName fieldName,
            final Column column, final String tableName,
            final boolean includeNonPortable) {
        JavaType fieldType = column.getJavaType();
        Validate.notNull(fieldType,
                "Field type for column '%s' in table '%s' is null",
                column.getName(), tableName);

        // Check if field is a Boolean object and is required, then change to
        // boolean primitive
        if (fieldType.equals(JavaType.BOOLEAN_OBJECT) && column.isRequired()) {
            fieldType = JavaType.BOOLEAN_PRIMITIVE;
        }

        // Add annotations to field
        final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

        // Add @Column annotation
        final AnnotationMetadataBuilder columnBuilder = new AnnotationMetadataBuilder(
                COLUMN);
        columnBuilder.addStringAttribute(NAME, column.getEscapedName());
        if (includeNonPortable) {
            columnBuilder.addStringAttribute("columnDefinition",
                    column.getTypeName());
        }

        // Add length attribute for Strings
        int columnSize = column.getColumnSize();
        if (columnSize < 4000 && fieldType.equals(JavaType.STRING)) {
            columnBuilder.addIntegerAttribute("length", columnSize);
        }

        // Add precision and scale attributes for numeric fields
        if (columnSize > 0 && JdkJavaType.isDecimalType(fieldType)) {
            columnBuilder.addIntegerAttribute("precision", columnSize);
            int scale = column.getScale();
            if (scale > 0) {
                columnBuilder.addIntegerAttribute("scale", scale);
            }
        }

        // Add unique = true to @Column if applicable
        if (column.isUnique()) {
            columnBuilder.addBooleanAttribute("unique", true);
        }

        annotations.add(columnBuilder);

        // Add @NotNull if applicable
        if (column.isRequired()) {
            annotations.add(new AnnotationMetadataBuilder(NOT_NULL));
        }

        // Add JSR 220 @Temporal annotation to date fields
        if (fieldType.equals(DATE) || fieldType.equals(CALENDAR)) {
            final AnnotationMetadataBuilder temporalBuilder = new AnnotationMetadataBuilder(
                    TEMPORAL);
            temporalBuilder.addEnumAttribute(VALUE, new EnumDetails(
                    TEMPORAL_TYPE, new JavaSymbolName(column.getJdbcType())));
            annotations.add(temporalBuilder);

            final AnnotationMetadataBuilder dateTimeFormatBuilder = new AnnotationMetadataBuilder(
                    DATE_TIME_FORMAT);
            if (fieldType.equals(DATE)) {
                dateTimeFormatBuilder.addStringAttribute("style", "M-");
            }
            else {
                dateTimeFormatBuilder.addStringAttribute("style", "MM");
            }

            if (fieldName.getSymbolName().equals(CREATED)) {
                columnBuilder.addBooleanAttribute("updatable", false);
            }
            annotations.add(dateTimeFormatBuilder);
        }

        // Add @Lob for CLOB fields if applicable
        if (column.getJdbcType().equals("CLOB")) {
            annotations.add(new AnnotationMetadataBuilder(LOB));
        }

        final FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(
                getId(), Modifier.PRIVATE, annotations, fieldName, fieldType);
        if (fieldName.getSymbolName().equals(CREATED)) {
            if (fieldType.equals(DATE)) {
                fieldBuilder.setFieldInitializer("new java.util.Date()");
            }
            else {
                fieldBuilder
                        .setFieldInitializer("java.util.Calendar.getInstance()");
            }
        }
        return fieldBuilder;
    }

    private String getInflectorPlural(final String term) {
        try {
            return Noun.pluralOf(term, Locale.ENGLISH);
        }
        catch (final RuntimeException e) {
            // Inflector failed (see for example ROO-305), so don't pluralize it
            return term;
        }
    }

    private AnnotationMetadataBuilder getJoinColumnAnnotation(
            final Reference reference, final boolean referencedColumn) {
        return getJoinColumnAnnotation(reference, referencedColumn, null);
    }

    private AnnotationMetadataBuilder getJoinColumnAnnotation(
            final Reference reference, final boolean referencedColumn,
            final JavaType fieldType) {
        return getJoinColumnAnnotation(reference, referencedColumn, fieldType,
                null);
    }

    private AnnotationMetadataBuilder getJoinColumnAnnotation(
            final Reference reference, final boolean referencedColumn,
            final JavaType fieldType, final Boolean nullable) {
        final Column localColumn = reference.getLocalColumn();
        Validate.notNull(localColumn, "Foreign-key reference local column '"
                + reference.getLocalColumnName() + "' must not be null");
        final AnnotationMetadataBuilder joinColumnBuilder = new AnnotationMetadataBuilder(
                JOIN_COLUMN);
        joinColumnBuilder
                .addStringAttribute(NAME, localColumn.getEscapedName());

        if (referencedColumn) {
            final Column foreignColumn = reference.getForeignColumn();
            Validate.notNull(
                    foreignColumn,
                    "Foreign-key reference foreign column '%s' must not be null",
                    reference.getForeignColumnName());
            joinColumnBuilder.addStringAttribute("referencedColumnName",
                    foreignColumn.getEscapedName());
        }

        if (nullable == null) {
            if (localColumn.isRequired()) {
                joinColumnBuilder.addBooleanAttribute("nullable", false);
            }
        }
        else {
            joinColumnBuilder.addBooleanAttribute("nullable", nullable);
        }

        if (fieldType != null) {
            if (isCompositeKeyColumn(localColumn) || localColumn.isPrimaryKey()
                    || !reference.isInsertableOrUpdatable()) {
                joinColumnBuilder.addBooleanAttribute("insertable", false);
                joinColumnBuilder.addBooleanAttribute("updatable", false);
            }
        }

        return joinColumnBuilder;
    }

    private AnnotationMetadataBuilder getJoinColumnsAnnotation(
            final Set<Reference> references, final JavaType fieldType) {
        final List<NestedAnnotationAttributeValue> arrayValues = new ArrayList<NestedAnnotationAttributeValue>();

        // Nullable attribute will have same value for each
        // If some column not required, all JoinColumn will be nullable
        boolean nullable = false;
        for (final Reference reference : references) {
            if (!reference.getLocalColumn().isRequired()) {
                nullable = true;
            }
        }

        for (final Reference reference : references) {
            final AnnotationMetadataBuilder joinColumnAnnotation = getJoinColumnAnnotation(
                    reference, true, fieldType, nullable);
            arrayValues.add(new NestedAnnotationAttributeValue(
                    new JavaSymbolName(VALUE), joinColumnAnnotation.build()));
        }
        final List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
        attributes.add(new ArrayAttributeValue<NestedAnnotationAttributeValue>(
                new JavaSymbolName(VALUE), arrayValues));
        return new AnnotationMetadataBuilder(JOIN_COLUMNS, attributes);
    }

    private FieldMetadataBuilder getManyToManyInverseSideField(
            final JavaSymbolName fieldName,
            final JavaSymbolName mappedByFieldName,
            final Table owningSideTable, final CascadeAction onUpdate,
            final CascadeAction onDelete) {
        final JavaType element = DbreTypeUtils.findTypeForTable(
                managedEntities, owningSideTable);
        Validate.notNull(
                element,
                "Attempted to create many-to-many inverse-side field '%s' in '%s' %s",
                fieldName, destination.getFullyQualifiedTypeName(),
                getErrorMsg(owningSideTable.getFullyQualifiedTableName()));

        final List<JavaType> params = Arrays.asList(element);
        final JavaType fieldType = new JavaType(
                SET.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, params);

        // Add annotations to field
        final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
        final AnnotationMetadataBuilder manyToManyBuilder = new AnnotationMetadataBuilder(
                MANY_TO_MANY);
        manyToManyBuilder.addStringAttribute(MAPPED_BY,
                mappedByFieldName.getSymbolName());
        addCascadeType(manyToManyBuilder, onUpdate, onDelete);
        annotations.add(manyToManyBuilder);

        return new FieldMetadataBuilder(getId(), Modifier.PRIVATE, annotations,
                fieldName, fieldType);
    }

    private FieldMetadataBuilder getManyToManyOwningSideField(
            final JavaSymbolName fieldName, final Table joinTable,
            final Table inverseSideTable, final CascadeAction onUpdate,
            final CascadeAction onDelete) {
        final JavaType element = DbreTypeUtils.findTypeForTable(
                managedEntities, inverseSideTable);
        Validate.notNull(
                element,
                "Attempted to create many-to-many owning-side field '%s' in '%s' %s",
                fieldName, destination.getFullyQualifiedTypeName(),
                getErrorMsg(inverseSideTable.getFullyQualifiedTableName()));

        final List<JavaType> params = Arrays.asList(element);
        final JavaType fieldType = new JavaType(
                SET.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, params);

        // Add annotations to field
        final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

        // Add @ManyToMany annotation
        final AnnotationMetadataBuilder manyToManyBuilder = new AnnotationMetadataBuilder(
                MANY_TO_MANY);
        annotations.add(manyToManyBuilder);

        // Add @JoinTable annotation
        final AnnotationMetadataBuilder joinTableBuilder = new AnnotationMetadataBuilder(
                JOIN_TABLE);
        final List<AnnotationAttributeValue<?>> joinTableAnnotationAttributes = new ArrayList<AnnotationAttributeValue<?>>();
        joinTableAnnotationAttributes.add(new StringAttributeValue(
                new JavaSymbolName(NAME), joinTable.getName()));

        final Iterator<ForeignKey> iter = joinTable.getImportedKeys()
                .iterator();

        // Add "joinColumns" attribute containing nested @JoinColumn annotations
        final List<NestedAnnotationAttributeValue> joinColumnArrayValues = new ArrayList<NestedAnnotationAttributeValue>();
        final Set<Reference> firstKeyReferences = iter.next().getReferences();
        for (final Reference reference : firstKeyReferences) {
            final AnnotationMetadataBuilder joinColumnBuilder = getJoinColumnAnnotation(
                    reference, firstKeyReferences.size() > 1);
            joinColumnArrayValues.add(new NestedAnnotationAttributeValue(
                    new JavaSymbolName(VALUE), joinColumnBuilder.build()));
        }
        joinTableAnnotationAttributes
                .add(new ArrayAttributeValue<NestedAnnotationAttributeValue>(
                        new JavaSymbolName("joinColumns"),
                        joinColumnArrayValues));

        // Add "inverseJoinColumns" attribute containing nested @JoinColumn
        // annotations
        final List<NestedAnnotationAttributeValue> inverseJoinColumnArrayValues = new ArrayList<NestedAnnotationAttributeValue>();
        final Set<Reference> lastKeyReferences = iter.next().getReferences();
        for (final Reference reference : lastKeyReferences) {
            final AnnotationMetadataBuilder joinColumnBuilder = getJoinColumnAnnotation(
                    reference, lastKeyReferences.size() > 1);
            inverseJoinColumnArrayValues
                    .add(new NestedAnnotationAttributeValue(new JavaSymbolName(
                            VALUE), joinColumnBuilder.build()));
        }
        joinTableAnnotationAttributes
                .add(new ArrayAttributeValue<NestedAnnotationAttributeValue>(
                        new JavaSymbolName("inverseJoinColumns"),
                        inverseJoinColumnArrayValues));

        // Add attributes to a @JoinTable annotation builder
        joinTableBuilder.setAttributes(joinTableAnnotationAttributes);
        annotations.add(joinTableBuilder);

        return new FieldMetadataBuilder(getId(), Modifier.PRIVATE, annotations,
                fieldName, fieldType);
    }

    private FieldMetadataBuilder getOneToManyMappedByField(
            final JavaSymbolName fieldName,
            final JavaSymbolName mappedByFieldName,
            final String foreignTableName, final String foreignSchemaName,
            final CascadeAction onUpdate, final CascadeAction onDelete) {
        final JavaType element = DbreTypeUtils.findTypeForTableName(
                managedEntities, foreignTableName, foreignSchemaName);
        Validate.notNull(
                element,
                "Attempted to create one-to-many mapped-by field '%s' in '%s' %s",
                fieldName, destination.getFullyQualifiedTypeName(),
                getErrorMsg(foreignTableName + "." + foreignSchemaName));

        final JavaType fieldType = new JavaType(
                SET.getFullyQualifiedTypeName(), 0, DataType.TYPE, null,
                Arrays.asList(element));
        // Add @OneToMany annotation
        final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
        final AnnotationMetadataBuilder oneToManyBuilder = new AnnotationMetadataBuilder(
                ONE_TO_MANY);
        oneToManyBuilder.addStringAttribute(MAPPED_BY,
                mappedByFieldName.getSymbolName());
        addCascadeType(oneToManyBuilder, onUpdate, onDelete);
        annotations.add(oneToManyBuilder);

        return new FieldMetadataBuilder(getId(), Modifier.PRIVATE, annotations,
                fieldName, fieldType);
    }

    private FieldMetadataBuilder getOneToOneMappedByField(
            final JavaSymbolName fieldName, final JavaType fieldType,
            final JavaSymbolName mappedByFieldName,
            final CascadeAction onUpdate, final CascadeAction onDelete) {
        final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
        final AnnotationMetadataBuilder oneToOneBuilder = new AnnotationMetadataBuilder(
                ONE_TO_ONE);
        oneToOneBuilder.addStringAttribute(MAPPED_BY,
                mappedByFieldName.getSymbolName());
        addCascadeType(oneToOneBuilder, onUpdate, onDelete);
        annotations.add(oneToOneBuilder);

        return new FieldMetadataBuilder(getId(), Modifier.PRIVATE, annotations,
                fieldName, fieldType);
    }

    private FieldMetadataBuilder getOneToOneOrManyToOneField(
            final JavaSymbolName fieldName, final JavaType fieldType,
            final ForeignKey foreignKey, final JavaType annotationType,
            final boolean referencedColumn) {
        // Add annotations to field
        final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

        // Add annotation
        final AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(
                annotationType);
        if (foreignKey.isExported()) {
            addCascadeType(annotationBuilder, foreignKey.getOnUpdate(),
                    foreignKey.getOnDelete());
        }
        annotations.add(annotationBuilder);

        final Set<Reference> references = foreignKey.getReferences();
        if (references.size() == 1) {
            // Add @JoinColumn annotation
            annotations.add(getJoinColumnAnnotation(references.iterator()
                    .next(), referencedColumn, fieldType));
        }
        else {
            // Add @JoinColumns annotation
            annotations.add(getJoinColumnsAnnotation(references, fieldType));
        }

        return new FieldMetadataBuilder(getId(), Modifier.PRIVATE, annotations,
                fieldName, fieldType);
    }

    private boolean hasField(final JavaSymbolName fieldName,
            final List<AnnotationMetadata> annotations) {
        // Check governor for field
        if (governorTypeDetails.getField(fieldName) != null) {
            return true;
        }

        // Check @Column and @JoinColumn annotations on fields in governor with
        // the same 'name' as the generated field
        final List<FieldMetadata> governorFields = governorTypeDetails
                .getFieldsWithAnnotation(COLUMN);
        governorFields.addAll(governorTypeDetails
                .getFieldsWithAnnotation(JOIN_COLUMN));
        for (final FieldMetadata governorField : governorFields) {
            governorFieldAnnotations: for (final AnnotationMetadata governorFieldAnnotation : governorField
                    .getAnnotations()) {
                if (governorFieldAnnotation.getAnnotationType().equals(COLUMN)
                        || governorFieldAnnotation.getAnnotationType().equals(
                                JOIN_COLUMN)) {
                    final AnnotationAttributeValue<?> name = governorFieldAnnotation
                            .getAttribute(new JavaSymbolName(NAME));
                    if (name == null) {
                        continue governorFieldAnnotations;
                    }
                    for (final AnnotationMetadata annotationMetadata : annotations) {
                        final AnnotationAttributeValue<?> columnName = annotationMetadata
                                .getAttribute(new JavaSymbolName(NAME));
                        if (columnName != null && columnName.equals(name)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * Indicates whether the ITD being built has a field of the given name
     * 
     * @param fieldName
     * @return true if the field exists in the builder, otherwise false
     */
    private boolean hasFieldInItd(final JavaSymbolName fieldName) {
        for (final FieldMetadataBuilder declaredField : builder
                .getDeclaredFields()) {
            if (declaredField.getFieldName().equals(fieldName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCompositeKeyColumn(final Column column) {
        if (!identifierHolder.isEmbeddedIdField()) {
            return false;
        }

        for (final FieldMetadata field : identifierHolder
                .getEmbeddedIdentifierFields()) {
            for (final AnnotationMetadata annotation : field.getAnnotations()) {
                if (!annotation.getAnnotationType().equals(COLUMN)) {
                    continue;
                }
                final AnnotationAttributeValue<?> nameAttribute = annotation
                        .getAttribute(new JavaSymbolName(NAME));
                if (nameAttribute != null) {
                    final String name = (String) nameAttribute.getValue();
                    if (column.getName().equals(name)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isCompositeKeyField(final JavaSymbolName fieldName) {
        if (!identifierHolder.isEmbeddedIdField()) {
            return false;
        }

        for (final FieldMetadata field : identifierHolder
                .getEmbeddedIdentifierFields()) {
            if (field.getFieldName().equals(fieldName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isEmbeddedIdField(final JavaSymbolName fieldName) {
        return identifierHolder.isEmbeddedIdField()
                && identifierHolder.getIdentifierField().getFieldName()
                        .equals(fieldName);
    }

    private boolean isIdField(final JavaSymbolName fieldName) {
        return !identifierHolder.isEmbeddedIdField()
                && identifierHolder.getIdentifierField().getFieldName()
                        .equals(fieldName);
    }

    private boolean isOneToOne(final Table table, final ForeignKey foreignKey) {
        Validate.notNull(table,
                "Table must not be null in determining a one-to-one relationship");
        Validate.notNull(foreignKey,
                "Foreign key must not be null in determining a one-to-one relationship");
        boolean equals = table.getPrimaryKeyCount() == foreignKey
                .getReferenceCount();
        final Iterator<Column> primaryKeyIterator = table.getPrimaryKeys()
                .iterator();
        while (equals && primaryKeyIterator.hasNext()) {
            equals &= foreignKey.hasLocalColumn(primaryKeyIterator.next());
        }
        return equals;
    }

    private boolean isVersionField(final JavaSymbolName fieldName) {
        return versionField != null
                && versionField.getFieldName().equals(fieldName);
    }
}
