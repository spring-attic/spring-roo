package org.springframework.roo.addon.dbre.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.roo.model.JavaPackage;

/**
 * Represents the database model, ie. the tables in the database.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class Database {

    // Whether to generate active record CRUD methods for each entity
    private boolean activeRecord;

    /** The JavaPackage where entities are created */
    private JavaPackage destinationPackage;

    /**
     * Whether or not to included non-portable JPA attributes in the @Column
     * annotation
     */
    private boolean includeNonPortableAttributes;

    /** The module where the entities are created */
    private String moduleName;

    /** Whether or not this database has multiple schemas */
    private boolean multipleSchemas;

    /** All tables. */
    private final Set<Table> tables;

    /** Whether to create integration tests */
    private boolean testAutomatically;

    /**
     * Constructor
     * 
     * @param tables (required)
     */
    Database(final Set<Table> tables) {
        Validate.notNull(tables, "Tables required");
        this.tables = tables;
        init();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Database other = (Database) obj;
        if (tables == null) {
            if (other.tables != null) {
                return false;
            }
        }
        else if (!tables.equals(other.tables)) {
            return false;
        }
        return true;
    }

    public JavaPackage getDestinationPackage() {
        return destinationPackage;
    }

    public String getModuleName() {
        return moduleName;
    }

    public Table getTable(final String name, final String schemaName) {
        for (final Table table : tables) {
            if (table.getName().equals(name)) {
                if (StringUtils.isBlank(schemaName)
                        || DbreModelService.NO_SCHEMA_REQUIRED
                                .equals(schemaName)
                        || table.getSchema().getName().equals(schemaName)) {
                    return table;
                }
            }
        }
        return null;
    }

    public Set<Table> getTables() {
        return Collections.unmodifiableSet(tables);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (tables == null ? 0 : tables.hashCode());
        return result;
    }

    public boolean hasMultipleSchemas() {
        return multipleSchemas;
    }

    public boolean hasTables() {
        return !tables.isEmpty();
    }

    /**
     * Initialises the model by establishing the relationships between elements
     * in this model eg. in foreign keys etc.
     */
    private void init() {
        final Set<Schema> schemas = new HashSet<Schema>();
        for (final Table table : tables) {
            schemas.add(table.getSchema());
            initializeImportedKeys(table);
            initializeExportedKeys(table);
            initializeIndices(table);
            initializeJoinTable(table);
        }
        multipleSchemas = schemas.size() > 1;
    }

    private void initializeExportedKeys(final Table table) {
        final Map<String, Short> keySequenceMap = new LinkedHashMap<String, Short>();
        Short keySequence = null;

        for (final ForeignKey exportedKey : table.getExportedKeys()) {
            if (exportedKey.getForeignTable() != null) {
                continue;
            }
            final String foreignTableName = exportedKey.getForeignTableName();
            final String foreignSchemaName = exportedKey.getForeignSchemaName();
            final Table targetTable = getTable(foreignTableName,
                    foreignSchemaName);
            if (targetTable != null) {
                exportedKey.setForeignTable(targetTable);
                keySequence = keySequenceMap.get(foreignTableName);
                if (keySequence == null) {
                    keySequence = 0;
                    keySequenceMap.put(foreignTableName, keySequence);
                }
                if (table
                        .getExportedKeyCountByForeignTableName(foreignTableName) > 1) {
                    keySequenceMap.put(foreignTableName,
                            (short) (keySequence.shortValue() + 1));
                }
                exportedKey.setKeySequence(keySequence);
            }

            for (final Reference reference : exportedKey.getReferences()) {
                if (reference.getLocalColumn() == null) {
                    final Column localColumn = table.findColumn(reference
                            .getLocalColumnName());
                    if (localColumn != null) {
                        reference.setLocalColumn(localColumn);
                    }
                }
                if (reference.getForeignColumn() == null
                        && exportedKey.getForeignTable() != null) {
                    final Column foreignColumn = exportedKey.getForeignTable()
                            .findColumn(reference.getForeignColumnName());
                    if (foreignColumn != null) {
                        reference.setForeignColumn(foreignColumn);
                    }
                }
            }
        }
    }

    private void initializeImportedKeys(final Table table) {
        final Map<String, Short> keySequenceMap = new LinkedHashMap<String, Short>();
        Short keySequence = null;
        final Map<Column, Set<ForeignKey>> repeatedColumns = new LinkedHashMap<Column, Set<ForeignKey>>();

        for (final ForeignKey foreignKey : table.getImportedKeys()) {
            if (foreignKey.getForeignTable() != null) {
                continue;
            }
            final String foreignTableName = foreignKey.getForeignTableName();
            final String foreignSchemaName = foreignKey.getForeignSchemaName();
            final Table targetTable = getTable(foreignTableName,
                    foreignSchemaName);
            if (targetTable != null) {
                keySequence = keySequenceMap.get(foreignTableName);
                if (keySequence == null) {
                    keySequence = 0;
                    keySequenceMap.put(foreignTableName, keySequence);
                }
                foreignKey.setForeignTable(targetTable);
                if (table
                        .getImportedKeyCountByForeignTableName(foreignTableName) > 1) {
                    keySequenceMap.put(foreignTableName,
                            (short) (keySequence.shortValue() + 1));
                }
                foreignKey.setKeySequence(keySequence);
            }

            for (final Reference reference : foreignKey.getReferences()) {
                if (reference.getLocalColumn() == null) {
                    final Column localColumn = table.findColumn(reference
                            .getLocalColumnName());
                    if (localColumn != null) {
                        reference.setLocalColumn(localColumn);

                        final Set<ForeignKey> fkSet = repeatedColumns
                                .containsKey(localColumn) ? repeatedColumns
                                .get(localColumn)
                                : new LinkedHashSet<ForeignKey>();
                        fkSet.add(foreignKey);
                        repeatedColumns.put(localColumn, fkSet);
                    }
                }
                if (reference.getForeignColumn() == null
                        && foreignKey.getForeignTable() != null) {
                    final Column foreignColumn = foreignKey.getForeignTable()
                            .findColumn(reference.getForeignColumnName());
                    if (foreignColumn != null) {
                        reference.setForeignColumn(foreignColumn);
                    }
                }
            }
        }

        // Mark repeated columns with insertable = false and updatable = false
        for (final Map.Entry<Column, Set<ForeignKey>> entrySet : repeatedColumns
                .entrySet()) {
            final Set<ForeignKey> foreignKeys = entrySet.getValue();
            for (final ForeignKey foreignKey : foreignKeys) {
                if (foreignKeys.size() > 1
                        || foreignKey.getForeignTableName().equals(
                                table.getName())) {
                    for (final Reference reference : foreignKey.getReferences()) {
                        reference.setInsertableOrUpdatable(false);
                    }
                }
            }
        }
    }

    private void initializeIndices(final Table table) {
        for (final Index index : table.getIndices()) {
            for (final IndexColumn indexColumn : index.getColumns()) {
                final Column column = table.findColumn(indexColumn.getName());
                if (column != null && index.isUnique()) {
                    column.setUnique(true);
                }
            }
        }
    }

    /**
     * Determines if a table is a many-to-many join table.
     * <p>
     * To be identified as a many-to-many join table, the table must have have
     * exactly two primary keys and have exactly two foreign-keys pointing to
     * other entity tables and have no other columns.
     */
    private void initializeJoinTable(final Table table) {
        boolean equals = table.getColumnCount() == 2
                && table.getPrimaryKeyCount() == 2
                && table.getImportedKeyCount() == 2
                && table.getPrimaryKeyCount() == table.getImportedKeyCount();
        final Iterator<Column> iter = table.getColumns().iterator();
        while (equals && iter.hasNext()) {
            final Column column = iter.next();
            equals &= table.findImportedKeyByLocalColumnName(column.getName()) != null;
        }
        if (equals) {
            table.setJoinTable(true);
        }
    }

    /**
     * Indicates whether active record CRUD methods should be generated for each
     * entity
     * 
     * @return see above
     * @since 1.2.0
     */
    public boolean isActiveRecord() {
        return activeRecord;
    }

    public boolean isIncludeNonPortableAttributes() {
        return includeNonPortableAttributes;
    }

    public boolean isTestAutomatically() {
        return testAutomatically;
    }

    /**
     * Sets whether active record CRUD methods should be generated for each
     * entity
     * 
     * @param activeRecord
     * @since 1.2.0
     */
    public void setActiveRecord(final boolean activeRecord) {
        this.activeRecord = activeRecord;
    }

    public void setDestinationPackage(final JavaPackage destinationPackage) {
        this.destinationPackage = destinationPackage;
    }

    public void setIncludeNonPortableAttributes(
            final boolean includeNonPortableAttributes) {
        this.includeNonPortableAttributes = includeNonPortableAttributes;
    }

    public void setModuleName(final String moduleName) {
        this.moduleName = moduleName;
    }

    public void setTestAutomatically(final boolean testAutomatically) {
        this.testAutomatically = testAutomatically;
    }

    @Override
    public String toString() {
        return String
                .format("Database [tables=%s, destinationPackage=%s, testAutomatically=%s, includeNonPortableAttributes=%s]",
                        tables, destinationPackage, testAutomatically,
                        includeNonPortableAttributes);
    }
}
