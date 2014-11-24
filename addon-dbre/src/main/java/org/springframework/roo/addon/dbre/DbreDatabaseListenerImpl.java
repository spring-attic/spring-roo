package org.springframework.roo.addon.dbre;

import static org.springframework.roo.addon.dbre.model.DbreModelService.DBRE_XML;
import static org.springframework.roo.model.JavaType.OBJECT;
import static org.springframework.roo.model.RooJavaType.ROO_DB_MANAGED;
import static org.springframework.roo.model.RooJavaType.ROO_IDENTIFIER;
import static org.springframework.roo.model.RooJavaType.ROO_JAVA_BEAN;
import static org.springframework.roo.model.RooJavaType.ROO_JPA_ACTIVE_RECORD;
import static org.springframework.roo.model.RooJavaType.ROO_JPA_ENTITY;
import static org.springframework.roo.model.RooJavaType.ROO_TO_STRING;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.dbre.model.Column;
import org.springframework.roo.addon.dbre.model.Database;
import org.springframework.roo.addon.dbre.model.DbreModelService;
import org.springframework.roo.addon.dbre.model.Table;
import org.springframework.roo.addon.jpa.identifier.Identifier;
import org.springframework.roo.addon.jpa.identifier.IdentifierService;
import org.springframework.roo.addon.layers.repository.jpa.RepositoryJpaOperations;
import org.springframework.roo.addon.layers.service.ServiceOperations;
import org.springframework.roo.addon.test.IntegrationTestOperations;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.file.monitor.event.FileEvent;
import org.springframework.roo.file.monitor.event.FileEventListener;
import org.springframework.roo.file.monitor.event.FileOperation;
import org.springframework.roo.metadata.AbstractHashCodeTrackingMetadataNotifier;
import org.springframework.roo.metadata.MetadataItem;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JdkJavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.support.util.CollectionUtils;

/**
 * Implementation of {@link DbreDatabaseListener}.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Component
@Service
public class DbreDatabaseListenerImpl extends
        AbstractHashCodeTrackingMetadataNotifier implements IdentifierService,
        FileEventListener {

    private static final JavaSymbolName DB_MANAGED = new JavaSymbolName(
            "dbManaged");
    private static final String IDENTIFIER_TYPE = "identifierType";
    private static final String PRIMARY_KEY_SUFFIX = "PK";
    private static final String SEQUENCE_NAME_FIELD = "sequenceName";
    private static final String VERSION = "version";
    private static final String VERSION_FIELD = "versionField";

    @Reference private DbreModelService dbreModelService;
    @Reference private FileManager fileManager;
    @Reference private IntegrationTestOperations integrationTestOperations;
    @Reference private ProjectOperations projectOperations;
    @Reference private RepositoryJpaOperations repositoryJpaOperations;
    @Reference private ServiceOperations serviceOperations;
    @Reference private Shell shell;
    @Reference private TypeLocationService typeLocationService;
    @Reference private TypeManagementService typeManagementService;

    private Map<JavaType, List<Identifier>> identifierResults;

    private void createIdentifierClass(final JavaType identifierType) {
        final List<AnnotationMetadataBuilder> identifierAnnotations = new ArrayList<AnnotationMetadataBuilder>();

        final AnnotationMetadataBuilder identifierBuilder = new AnnotationMetadataBuilder(
                ROO_IDENTIFIER);
        identifierBuilder.addBooleanAttribute(DB_MANAGED.getSymbolName(), true);
        identifierAnnotations.add(identifierBuilder);

        // Produce identifier itself
        final String declaredByMetadataId = PhysicalTypeIdentifier
                .createIdentifier(identifierType, projectOperations
                        .getPathResolver().getFocusedPath(Path.SRC_MAIN_JAVA));
        final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                declaredByMetadataId, Modifier.PUBLIC | Modifier.FINAL,
                identifierType, PhysicalTypeCategory.CLASS);
        cidBuilder.setAnnotations(identifierAnnotations);
        typeManagementService.createOrUpdateTypeOnDisk(cidBuilder.build());

        shell.flash(Level.FINE,
                "Created " + identifierType.getFullyQualifiedTypeName(),
                DbreDatabaseListenerImpl.class.getName());
        shell.flash(Level.FINE, "", DbreDatabaseListenerImpl.class.getName());
    }

    /**
     * Creates a new DBRE-managed entity from the given table
     * 
     * @param javaType the name of the entity to be created (required)
     * @param table the table from which to create the entity (required)
     * @param activeRecord whether to create "active record" CRUD methods in the
     *            new entity
     * @return the newly created entity
     */
    private ClassOrInterfaceTypeDetails createNewManagedEntityFromTable(
            final JavaType javaType, final Table table,
            final boolean activeRecord) {
        // Create type annotations for new entity
        final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
        annotations.add(new AnnotationMetadataBuilder(ROO_JAVA_BEAN));
        annotations.add(new AnnotationMetadataBuilder(ROO_TO_STRING));

        // Find primary key from db metadata and add identifier attributes to
        // @RooJpaEntity
        final AnnotationMetadataBuilder jpaAnnotationBuilder = new AnnotationMetadataBuilder(
                activeRecord ? ROO_JPA_ACTIVE_RECORD : ROO_JPA_ENTITY);
        manageIdentifier(javaType, jpaAnnotationBuilder,
                new HashSet<JavaSymbolName>(), table);

        if (!hasVersionField(table)) {
            jpaAnnotationBuilder.addStringAttribute(VERSION_FIELD, "");
        }
        if (table.isDisableGeneratedIdentifiers()) {
            jpaAnnotationBuilder.addStringAttribute(SEQUENCE_NAME_FIELD, "");
        }

        jpaAnnotationBuilder.addStringAttribute("table", table.getName());
        if (!DbreModelService.NO_SCHEMA_REQUIRED.equals(table.getSchema()
                .getName())) {
            jpaAnnotationBuilder.addStringAttribute("schema", table.getSchema()
                    .getName());
        }

        annotations.add(jpaAnnotationBuilder);

        // Add @RooDbManaged
        annotations.add(getRooDbManagedAnnotation());

        final JavaType superclass = OBJECT;
        final List<JavaType> extendsTypes = new ArrayList<JavaType>();
        extendsTypes.add(superclass);

        // Create entity class
        final String declaredByMetadataId = PhysicalTypeIdentifier
                .createIdentifier(javaType, projectOperations.getPathResolver()
                        .getFocusedPath(Path.SRC_MAIN_JAVA));
        final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                declaredByMetadataId, Modifier.PUBLIC, javaType,
                PhysicalTypeCategory.CLASS);
        cidBuilder.setExtendsTypes(extendsTypes);
        cidBuilder.setAnnotations(annotations);

        final ClassOrInterfaceTypeDetails entity = cidBuilder.build();
        typeManagementService.createOrUpdateTypeOnDisk(entity);

        shell.flash(Level.FINE,
                "Created " + javaType.getFullyQualifiedTypeName(),
                DbreDatabaseListenerImpl.class.getName());
        shell.flash(Level.FINE, "", DbreDatabaseListenerImpl.class.getName());

        return entity;
    }

    /**
     * Deletes the given {@link JavaType} for the given reason
     * 
     * @param javaType the type to be deleted (required)
     * @param reason the reason for deletion (can be blank)
     */
    private void deleteJavaType(final JavaType javaType, final String reason) {
        final PhysicalTypeMetadata governorPhysicalTypeMetadata = getPhysicalTypeMetadata(javaType);
        if (governorPhysicalTypeMetadata != null) {
            final String filePath = governorPhysicalTypeMetadata
                    .getPhysicalLocationCanonicalPath();
            if (fileManager.exists(filePath)) {
                fileManager.delete(filePath, reason);
                shell.flash(Level.FINE,
                        "Deleted " + javaType.getFullyQualifiedTypeName(),
                        DbreDatabaseListenerImpl.class.getName());
            }

            shell.flash(Level.FINE, "",
                    DbreDatabaseListenerImpl.class.getName());
        }
    }

    private void deleteManagedType(
            final ClassOrInterfaceTypeDetails managedEntity, final String reason) {
        if (!isEntityDeletable(managedEntity)) {
            return;
        }
        deleteJavaType(managedEntity.getName(), reason);

        final JavaType identifierType = getIdentifierType(managedEntity
                .getName());
        for (final ClassOrInterfaceTypeDetails managedIdentifier : getManagedIdentifiers()) {
            if (managedIdentifier.getName().equals(identifierType)) {
                deleteJavaType(
                        identifierType,
                        "managed identifier of deleted type "
                                + managedEntity.getName());
                break;
            }
        }
    }

    private void deserializeDatabase() {
        final Database database = dbreModelService.getDatabase(true);
        if (database != null) {
            identifierResults = new LinkedHashMap<JavaType, List<Identifier>>();
            reverseEngineer(database);
        }
    }

    private JavaPackage getDestinationPackage(final Database database,
            final Set<ClassOrInterfaceTypeDetails> managedEntities) {
        JavaPackage destinationPackage = database.getDestinationPackage();
        if (destinationPackage == null) {
            if (!managedEntities.isEmpty() && !database.hasMultipleSchemas()) {
                // Take the package of the first one
                destinationPackage = managedEntities.iterator().next()
                        .getName().getPackage();
            }
        }

        // Fall back to project's top level package
        if (destinationPackage == null) {
            destinationPackage = projectOperations.getFocusedTopLevelPackage();
        }
        return destinationPackage;
    }

    public List<Identifier> getIdentifiers(final JavaType pkType) {
        if (identifierResults == null) {
            // Need to populate the identifier results before returning from
            // this method
            deserializeDatabase();
        }
        if (identifierResults == null) {
            // It's still null, so maybe the DBRE XML file isn't available at
            // this time or similar
            return null;
        }
        return identifierResults.get(pkType);
    }

    private List<Identifier> getIdentifiers(final Table table,
            final boolean usePrimaryKeys) {
        final List<Identifier> result = new ArrayList<Identifier>();

        // Add fields to the identifier class
        final Set<Column> columns = usePrimaryKeys ? table.getPrimaryKeys()
                : table.getColumns();
        for (final Column column : columns) {
            final String columnName = column.getName();
            JavaSymbolName fieldName;
            try {
                fieldName = new JavaSymbolName(
                        DbreTypeUtils.suggestFieldName(columnName));
            }
            catch (final RuntimeException e) {
                throw new IllegalArgumentException(
                        "Failed to create field name for column '" + columnName
                                + "' in table '" + table.getName() + "': "
                                + e.getMessage());
            }
            final JavaType fieldType = column.getJavaType();
            final String columnDefinition = table
                    .isIncludeNonPortableAttributes() ? column.getTypeName()
                    : "";
            result.add(new Identifier(fieldName, fieldType, columnName, column
                    .getColumnSize(), column.getScale(), columnDefinition));
        }
        return result;
    }

    private List<Identifier> getIdentifiersFromColumns(final Table table) {
        return getIdentifiers(table, false);
    }

    private List<Identifier> getIdentifiersFromPrimaryKeys(final Table table) {
        return getIdentifiers(table, true);
    }

    /**
     * Returns the type of ID that DBRE should use for the given entity
     * 
     * @param entity the entity for which to get the ID type (required)
     * @return a non-<code>null</code> ID type
     */
    private JavaType getIdentifierType(final JavaType entity) {
        final PhysicalTypeMetadata governorPhysicalTypeMetadata = getPhysicalTypeMetadata(entity);
        if (governorPhysicalTypeMetadata != null) {
            final ClassOrInterfaceTypeDetails governorTypeDetails = governorPhysicalTypeMetadata
                    .getMemberHoldingTypeDetails();
            final AnnotationMetadata jpaAnnotation = getJpaAnnotation(governorTypeDetails);
            if (jpaAnnotation != null) {
                final AnnotationAttributeValue<?> identifierTypeAttribute = jpaAnnotation
                        .getAttribute(new JavaSymbolName(IDENTIFIER_TYPE));
                if (identifierTypeAttribute != null) {
                    // The identifierType attribute exists, so get its value
                    final JavaType identifierType = (JavaType) identifierTypeAttribute
                            .getValue();
                    if (identifierType != null
                            && !JdkJavaType.isPartOfJavaLang(identifierType)) {
                        return identifierType;
                    }
                }
            }
        }

        // The JPA annotation's "identifierType" attribute does not exist or is
        // not a simple type, so return a default
        return new JavaType(entity.getFullyQualifiedTypeName()
                + PRIMARY_KEY_SUFFIX);
    }

    /**
     * Returns the JPA-related annotation on the given managed entity
     * 
     * @param managedEntity an existing DBRE-managed entity (required)
     * @return <code>null</code> if there isn't one
     */
    private AnnotationMetadata getJpaAnnotation(
            final ClassOrInterfaceTypeDetails managedEntity) {
        // The @RooJpaEntity annotation takes precedence if present
        final AnnotationMetadata rooJpaEntity = managedEntity
                .getAnnotation(ROO_JPA_ENTITY);
        if (rooJpaEntity != null) {
            return rooJpaEntity;
        }
        return managedEntity.getAnnotation(ROO_JPA_ACTIVE_RECORD);
    }

    private Set<ClassOrInterfaceTypeDetails> getManagedIdentifiers() {
        final Set<ClassOrInterfaceTypeDetails> managedIdentifierTypes = new LinkedHashSet<ClassOrInterfaceTypeDetails>();

        final Set<ClassOrInterfaceTypeDetails> identifierTypes = typeLocationService
                .findClassesOrInterfaceDetailsWithAnnotation(ROO_IDENTIFIER);
        for (final ClassOrInterfaceTypeDetails managedIdentifierType : identifierTypes) {
            final AnnotationMetadata identifierAnnotation = managedIdentifierType
                    .getTypeAnnotation(ROO_IDENTIFIER);
            final AnnotationAttributeValue<?> attrValue = identifierAnnotation
                    .getAttribute(DB_MANAGED);
            if (attrValue != null && (Boolean) attrValue.getValue()) {
                managedIdentifierTypes.add(managedIdentifierType);
            }
        }
        return managedIdentifierTypes;
    }

    private PhysicalTypeMetadata getPhysicalTypeMetadata(final JavaType javaType) {
        final String declaredByMetadataId = typeLocationService
                .getPhysicalTypeIdentifier(javaType);
        if (StringUtils.isBlank(declaredByMetadataId)) {
            return null;
        }
        return (PhysicalTypeMetadata) getMetadataService().get(declaredByMetadataId);
    }

    private AnnotationMetadataBuilder getRooDbManagedAnnotation() {
        final AnnotationMetadataBuilder rooDbManagedBuilder = new AnnotationMetadataBuilder(
                ROO_DB_MANAGED);
        rooDbManagedBuilder.addBooleanAttribute("automaticallyDelete", true);
        return rooDbManagedBuilder;
    }

    /**
     * Indicates whether the given entity has the standard annotations applied
     * by Roo, and no others.
     * 
     * @param entity the entity to check (required)
     * @return <code>false</code> if any of the standard ones are missing or any
     *         extra ones have been added
     */
    private boolean hasStandardEntityAnnotations(
            final ClassOrInterfaceTypeDetails entity) {
        final List<? extends AnnotationMetadata> typeAnnotations = entity
                .getAnnotations();
        // We expect four: RooDbManaged, RooJavaBean, RooToString, and either
        // RooEntity or RooJpaEntity
        if (typeAnnotations.size() != 4) {
            return false;
        }
        // There are exactly four - check for any non-standard ones
        for (final AnnotationMetadata annotation : typeAnnotations) {
            final JavaType annotationType = annotation.getAnnotationType();
            final boolean entityAnnotation = ROO_JPA_ACTIVE_RECORD
                    .equals(annotationType)
                    || ROO_JPA_ENTITY.equals(annotationType);
            if (!entityAnnotation && !ROO_DB_MANAGED.equals(annotationType)
                    && !ROO_JAVA_BEAN.equals(annotationType)
                    && !ROO_TO_STRING.equals(annotationType)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasVersionField(final Table table) {
        if (!table.isDisableVersionFields()) {
            for (final Column column : table.getColumns()) {
                if (VERSION.equalsIgnoreCase(column.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Indicates whether active record CRUD methods should be generated for the
     * given entities (this being an all or nothing decision)
     * 
     * @param database the database being reverse-engineered (required)
     * @param managedEntities any existing DB-managed entities in the user
     *            project (can be <code>null</code> or empty)
     * @return see above
     */
    private boolean isActiveRecord(final Database database,
            final Collection<ClassOrInterfaceTypeDetails> managedEntities) {
        if (CollectionUtils.isEmpty(managedEntities)) {
            // There are no existing entities; use the given setting
            return database.isActiveRecord();
        }
        /*
         * There are one or more existing entities; preserve the existing
         * decision, based on the first such entity. This saves the user from
         * having to enter the same value for the "activeRecord" option each
         * time they run the database reverse engineer command.
         */
        return managedEntities.iterator().next()
                .getAnnotation(ROO_JPA_ACTIVE_RECORD) != null;
    }

    private boolean isEntityDeletable(
            final ClassOrInterfaceTypeDetails managedEntity) {
        final String declaredByMetadataId = DbreMetadata.createIdentifier(
                managedEntity.getName(), PhysicalTypeIdentifier
                        .getPath(managedEntity.getDeclaredByMetadataId()));
        final DbreMetadata dbreMetadata = (DbreMetadata) getMetadataService()
                .get(declaredByMetadataId);
        if (dbreMetadata == null || !dbreMetadata.isAutomaticallyDelete()) {
            return false;
        }

        // Check whether the type's annotations have been customised
        if (!hasStandardEntityAnnotations(managedEntity)) {
            return false;
        }

        // Finally, check for added constructors, fields and methods
        return managedEntity.getDeclaredConstructors().isEmpty()
                && managedEntity.getDeclaredFields().isEmpty()
                && managedEntity.getDeclaredMethods().isEmpty();
    }

    private boolean isIdentifierDeletable(final JavaType identifierType) {
        final PhysicalTypeMetadata governorPhysicalTypeMetadata = getPhysicalTypeMetadata(identifierType);
        if (governorPhysicalTypeMetadata == null) {
            return false;
        }

        // Check for added constructors, fields and methods
        final ClassOrInterfaceTypeDetails managedIdentifier = governorPhysicalTypeMetadata
                .getMemberHoldingTypeDetails();
        return managedIdentifier.getDeclaredConstructors().isEmpty()
                && managedIdentifier.getDeclaredFields().isEmpty()
                && managedIdentifier.getDeclaredMethods().isEmpty();
    }

    private void manageIdentifier(final JavaType javaType,
            final AnnotationMetadataBuilder jpaAnnotationBuilder,
            final Set<JavaSymbolName> attributesToDeleteIfPresent,
            final Table table) {
        final JavaType identifierType = getIdentifierType(javaType);
        final PhysicalTypeMetadata identifierPhysicalTypeMetadata = getPhysicalTypeMetadata(identifierType);

        // Process primary keys and add 'identifierType' attribute
        final int pkCount = table.getPrimaryKeyCount();
        if (pkCount == 1) {
            // Table has one primary key
            // Check for redundant, managed identifier class and delete if found
            if (isIdentifierDeletable(identifierType)) {
                deleteJavaType(identifierType, "the " + table.getName()
                        + " table has only one primary key");
            }

            attributesToDeleteIfPresent
                    .add(new JavaSymbolName(IDENTIFIER_TYPE));

            // We don't need a PK class, so we just tell the
            // JpaActiveRecordProvider via IdentifierService the column name,
            // field type and field name to use
            final List<Identifier> identifiers = getIdentifiersFromPrimaryKeys(table);
            identifierResults.put(javaType, identifiers);
        }
        else if (pkCount == 0 || pkCount > 1) {
            // Table has either no primary keys or more than one primary key so
            // create a composite key

            // Check if identifier class already exists and if not, create it
            if (identifierPhysicalTypeMetadata == null
                    || !identifierPhysicalTypeMetadata.isValid()
                    || identifierPhysicalTypeMetadata
                            .getMemberHoldingTypeDetails() == null) {
                createIdentifierClass(identifierType);
            }

            jpaAnnotationBuilder.addClassAttribute(IDENTIFIER_TYPE,
                    identifierType);

            // We need a PK class, so we tell the IdentifierMetadataProvider via
            // IdentifierService the various column names, field types and field
            // names to use
            // For tables with no primary keys, create a composite key using all
            // the table's columns
            final List<Identifier> identifiers = pkCount == 0 ? getIdentifiersFromColumns(table)
                    : getIdentifiersFromPrimaryKeys(table);
            identifierResults.put(identifierType, identifiers);
        }
    }

    private void notify(final List<ClassOrInterfaceTypeDetails> entities) {
        for (final ClassOrInterfaceTypeDetails managedIdentifierType : getManagedIdentifiers()) {
            final MetadataItem metadataItem = getMetadataService()
                    .evictAndGet(managedIdentifierType
                            .getDeclaredByMetadataId());
            if (metadataItem != null) {
                notifyIfRequired(metadataItem);
            }
        }

        for (final ClassOrInterfaceTypeDetails entity : entities) {
            final MetadataItem metadataItem = getMetadataService()
                    .evictAndGet(entity.getDeclaredByMetadataId());
            if (metadataItem != null) {
                notifyIfRequired(metadataItem);
            }
        }
    }

    public void onFileEvent(final FileEvent fileEvent) {
        if (fileEvent.getFileDetails().getCanonicalPath().endsWith(DBRE_XML)) {
            final FileOperation operation = fileEvent.getOperation();
            if (operation == FileOperation.UPDATED
                    || operation == FileOperation.CREATED) {
                deserializeDatabase();
            }
        }
    }

    private void reverseEngineer(final Database database) {
        final Set<ClassOrInterfaceTypeDetails> managedEntities = typeLocationService
                .findClassesOrInterfaceDetailsWithAnnotation(ROO_DB_MANAGED);
        // Determine whether to create "active record" CRUD methods
        database.setActiveRecord(isActiveRecord(database, managedEntities));

        // Lookup the relevant destination package if not explicitly given
        final JavaPackage destinationPackage = getDestinationPackage(database,
                managedEntities);

        // Set the destination package in the database
        database.setDestinationPackage(destinationPackage);

        // Get tables from database
        final Set<Table> tables = new LinkedHashSet<Table>(database.getTables());

        // Manage existing entities with @RooDbManaged annotation
        for (final ClassOrInterfaceTypeDetails managedEntity : managedEntities) {
            // Remove table from set as each managed entity is processed.
            // The tables that remain in the set will be used for creation of
            // new entities later
            final Table table = updateOrDeleteManagedEntity(managedEntity,
                    database);
            if (table != null) {
                tables.remove(table);
            }
        }

        // Create new entities from tables
        final List<ClassOrInterfaceTypeDetails> newEntities = new ArrayList<ClassOrInterfaceTypeDetails>();
        for (final Table table : tables) {
            // Don't create types from join tables in many-to-many associations
            if (!table.isJoinTable()) {
                JavaPackage schemaPackage = destinationPackage;
                if (database.hasMultipleSchemas()) {
                    schemaPackage = new JavaPackage(
                            destinationPackage.getFullyQualifiedPackageName()
                                    + "."
                                    + DbreTypeUtils.suggestPackageName(table
                                            .getSchema().getName()));
                }
                final JavaType javaType = DbreTypeUtils
                        .suggestTypeNameForNewTable(table.getName(),
                                schemaPackage);
                final boolean activeRecord = database.isActiveRecord()
                        && !database.isRepository();
                if (typeLocationService.getTypeDetails(javaType) == null) {
                    table.setIncludeNonPortableAttributes(database
                            .isIncludeNonPortableAttributes());
                    table.setDisableVersionFields(database
                            .isDisableVersionFields());
                    table.setDisableGeneratedIdentifiers(database
                            .isDisableGeneratedIdentifiers());
                    newEntities.add(createNewManagedEntityFromTable(javaType,
                            table, activeRecord));
                }
            }
        }

        // Create repositories if required
        if (database.isRepository()) {
            for (final ClassOrInterfaceTypeDetails entity : newEntities) {
                final JavaType type = entity.getType();
                repositoryJpaOperations.setupRepository(
                        new JavaType(type.getFullyQualifiedTypeName()
                                + "Repository"), type);
            }
        }

        // Create services if required
        if (database.isService()) {
            for (final ClassOrInterfaceTypeDetails entity : newEntities) {
                final JavaType type = entity.getType();
                final String typeName = type.getFullyQualifiedTypeName();
                serviceOperations.setupService(new JavaType(typeName
                        + "Service"), new JavaType(typeName + "ServiceImpl"),
                        type, false, "", false, false);
            }
        }

        // Create integration tests if required
        if (database.isTestAutomatically()) {
            for (final ClassOrInterfaceTypeDetails entity : newEntities) {
                integrationTestOperations.newIntegrationTest(entity.getType());
            }
        }

        // Notify
        final List<ClassOrInterfaceTypeDetails> allEntities = new ArrayList<ClassOrInterfaceTypeDetails>();
        allEntities.addAll(newEntities);
        allEntities.addAll(managedEntities);
        notify(allEntities);
    }

    private Table updateOrDeleteManagedEntity(
            final ClassOrInterfaceTypeDetails managedEntity,
            final Database database) {
        // Update the attributes of the existing JPA-related annotation
        final AnnotationMetadata jpaAnnotation = getJpaAnnotation(managedEntity);
        Validate.validState(jpaAnnotation != null,
                "Neither @%s nor @%s found on existing DBRE-managed entity %s",
                ROO_JPA_ACTIVE_RECORD.getSimpleTypeName(), ROO_JPA_ENTITY
                        .getSimpleTypeName(), managedEntity.getName()
                        .getFullyQualifiedTypeName());

        // Find table in database using 'table' and 'schema' attributes from the
        // JPA annotation
        final AnnotationAttributeValue<?> tableAttribute = jpaAnnotation
                .getAttribute(new JavaSymbolName("table"));
        final String errMsg = "Unable to maintain database-managed entity "
                + managedEntity.getName().getFullyQualifiedTypeName()
                + " because its associated table could not be found";
        Validate.notNull(tableAttribute, errMsg);
        final String tableName = (String) tableAttribute.getValue();
        Validate.notBlank(tableName, errMsg);

        final AnnotationAttributeValue<?> schemaAttribute = jpaAnnotation
                .getAttribute(new JavaSymbolName("schema"));
        final String schemaName = schemaAttribute != null ? (String) schemaAttribute
                .getValue() : null;

        final Table table = database.getTable(tableName, schemaName);
        if (table == null) {
            // Table is missing and probably has been dropped so delete managed
            // type and its identifier if applicable
            deleteManagedType(managedEntity, "no database table called '"
                    + tableName + "'");
            return null;
        }

        table.setIncludeNonPortableAttributes(database
                .isIncludeNonPortableAttributes());
        table.setDisableVersionFields(database.isDisableVersionFields());
        table.setDisableGeneratedIdentifiers(database
                .isDisableGeneratedIdentifiers());

        // Update the @RooJpaEntity/@RooJpaActiveRecord attributes
        final AnnotationMetadataBuilder jpaAnnotationBuilder = new AnnotationMetadataBuilder(
                jpaAnnotation);
        final Set<JavaSymbolName> attributesToDeleteIfPresent = new LinkedHashSet<JavaSymbolName>();
        manageIdentifier(managedEntity.getName(), jpaAnnotationBuilder,
                attributesToDeleteIfPresent, table);

        // Manage versionField attribute
        final AnnotationAttributeValue<?> versionFieldAttribute = jpaAnnotation
                .getAttribute(new JavaSymbolName(VERSION_FIELD));
        if (versionFieldAttribute == null) {
            if (hasVersionField(table)) {
                attributesToDeleteIfPresent.add(new JavaSymbolName(
                        VERSION_FIELD));
            }
            else {
                jpaAnnotationBuilder.addStringAttribute(VERSION_FIELD, "");
            }
        }
        else {
            final String versionFieldValue = (String) versionFieldAttribute
                    .getValue();
            if (hasVersionField(table)
                    && (StringUtils.isBlank(versionFieldValue) || VERSION
                            .equals(versionFieldValue))) {
                attributesToDeleteIfPresent.add(new JavaSymbolName(
                        VERSION_FIELD));
            }
        }

        final AnnotationAttributeValue<?> sequenceNameFieldAttribute = jpaAnnotation
                .getAttribute(new JavaSymbolName(SEQUENCE_NAME_FIELD));
        if (sequenceNameFieldAttribute == null) {
            if (!table.isDisableGeneratedIdentifiers()) {
                attributesToDeleteIfPresent.add(new JavaSymbolName(
                        SEQUENCE_NAME_FIELD));
            }
            else {
                jpaAnnotationBuilder
                        .addStringAttribute(SEQUENCE_NAME_FIELD, "");
            }
        }
        else {
            final String sequenceNameFieldValue = (String) sequenceNameFieldAttribute
                    .getValue();
            if (!table.isDisableGeneratedIdentifiers()
                    && ("".equals(sequenceNameFieldValue))) {
                attributesToDeleteIfPresent.add(new JavaSymbolName(
                        SEQUENCE_NAME_FIELD));
            }
        }

        // Update the annotation on disk
        final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                managedEntity);
        cidBuilder.updateTypeAnnotation(jpaAnnotationBuilder.build(),
                attributesToDeleteIfPresent);
        typeManagementService.createOrUpdateTypeOnDisk(cidBuilder.build());
        return table;
    }
}
