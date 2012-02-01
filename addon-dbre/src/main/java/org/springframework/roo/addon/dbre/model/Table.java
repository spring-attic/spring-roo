package org.springframework.roo.addon.dbre.model;

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * Represents a table in the database model.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class Table {
    private String catalog;
    private final Set<Column> columns = new LinkedHashSet<Column>();
    private String description;
    private final Set<ForeignKey> exportedKeys = new LinkedHashSet<ForeignKey>();
    private final Set<ForeignKey> importedKeys = new LinkedHashSet<ForeignKey>();
    private boolean includeNonPortableAttributes;
    private final Set<Index> indices = new LinkedHashSet<Index>();
    private boolean joinTable;
    private final String name;
    private final Schema schema;

    Table(final String name, final Schema schema) {
        Validate.isTrue(StringUtils.isNotBlank(name), "Table name required");
        Validate.notNull(schema, "Table schema required");
        this.name = name;
        this.schema = schema;
    }

    public boolean addColumn(final Column column) {
        Validate.notNull(column, "Column required");
        return columns.add(column);
    }

    public boolean addExportedKey(final ForeignKey exportedKey) {
        Validate.notNull(exportedKey, "Exported key required");
        return exportedKeys.add(exportedKey);
    }

    public boolean addImportedKey(final ForeignKey foreignKey) {
        Validate.notNull(foreignKey, "Foreign key required");
        return importedKeys.add(foreignKey);
    }

    public boolean addIndex(final Index index) {
        Validate.notNull(index, "Index required");
        return indices.add(index);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Table)) {
            return false;
        }
        final Table other = (Table) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        }
        else if (!name.equalsIgnoreCase(other.name)) {
            return false;
        }
        if (schema == null) {
            if (other.schema != null) {
                return false;
            }
        }
        else if (!schema.equals(other.schema)) {
            return false;
        }
        return true;
    }

    public Column findColumn(final String name) {
        for (final Column column : columns) {
            if (column.getName().equalsIgnoreCase(name)) {
                return column;
            }
        }
        return null;
    }

    public ForeignKey findImportedKeyByLocalColumnName(
            final String localColumnName) {
        for (final ForeignKey foreignKey : importedKeys) {
            for (final Reference reference : foreignKey.getReferences()) {
                if (reference.getLocalColumnName().equalsIgnoreCase(
                        localColumnName)) {
                    return foreignKey;
                }
            }
        }
        return null;
    }

    public String getCatalog() {
        return StringUtils.trimToNull(catalog);
    }

    public int getColumnCount() {
        return columns.size();
    }

    public Set<Column> getColumns() {
        return columns;
    }

    public String getDescription() {
        return description;
    }

    public int getExportedKeyCountByForeignTableName(
            final String foreignTableName) {
        int count = 0;
        for (final ForeignKey exportedKey : exportedKeys) {
            if (exportedKey.getForeignTableName().equalsIgnoreCase(
                    foreignTableName)) {
                count++;
            }
        }
        return count;
    }

    public Set<ForeignKey> getExportedKeys() {
        return exportedKeys;
    }

    public String getFullyQualifiedTableName() {
        return DbreModelService.NO_SCHEMA_REQUIRED.equals(schema.getName()) ? name
                : schema.getName() + "." + name;
    }

    public ForeignKey getImportedKey(final String name) {
        for (final ForeignKey foreignKey : importedKeys) {
            Validate.isTrue(StringUtils.isNotBlank(foreignKey.getName()),
                    "Foreign key name required");
            if (foreignKey.getName().equalsIgnoreCase(name)) {
                return foreignKey;
            }
        }
        return null;
    }

    public int getImportedKeyCount() {
        return importedKeys.size();
    }

    public int getImportedKeyCountByForeignTableName(
            final String foreignTableName) {
        int count = 0;
        for (final ForeignKey foreignKey : importedKeys) {
            if (foreignKey.getForeignTableName().equalsIgnoreCase(
                    foreignTableName)) {
                count++;
            }
        }
        return count;
    }

    public Set<ForeignKey> getImportedKeys() {
        return importedKeys;
    }

    public Set<Index> getIndices() {
        return indices;
    }

    public String getName() {
        return name;
    }

    public int getPrimaryKeyCount() {
        return getPrimaryKeys().size();
    }

    public Set<Column> getPrimaryKeys() {
        final Set<Column> primaryKeys = new LinkedHashSet<Column>();
        for (final Column column : columns) {
            if (column.isPrimaryKey()) {
                primaryKeys.add(column);
            }
        }
        return primaryKeys;
    }

    public Schema getSchema() {
        return schema;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + name.hashCode();
        result = prime * result + schema.hashCode();
        return result;
    }

    public boolean isIncludeNonPortableAttributes() {
        return includeNonPortableAttributes;
    }

    public boolean isJoinTable() {
        return joinTable;
    }

    public void setCatalog(final String catalog) {
        this.catalog = catalog;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setIncludeNonPortableAttributes(
            final boolean includeNonPortableAttributes) {
        this.includeNonPortableAttributes = includeNonPortableAttributes;
    }

    public void setJoinTable(final boolean joinTable) {
        this.joinTable = joinTable;
    }

    @Override
    public String toString() {
        return String
                .format("Table [name=%s, schema=%s, catalog=%s, description=%s, columns=%s, importedKeys=%s, exportedKeys=%s, indices=%s, includeNonPortableAttributes=%s]",
                        name, schema.getName(), catalog, description, columns,
                        importedKeys, exportedKeys, indices,
                        includeNonPortableAttributes);
    }
}
