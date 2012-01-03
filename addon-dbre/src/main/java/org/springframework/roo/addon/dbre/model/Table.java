package org.springframework.roo.addon.dbre.model;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Represents a table in the database model.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class Table {
    private String catalog;
    private final Schema schema;
    private final String name;
    private String description;
    private boolean joinTable;
    private final Set<Column> columns = new LinkedHashSet<Column>();
    private final Set<ForeignKey> importedKeys = new LinkedHashSet<ForeignKey>();
    private final Set<ForeignKey> exportedKeys = new LinkedHashSet<ForeignKey>();
    private final Set<Index> indices = new LinkedHashSet<Index>();
    private boolean includeNonPortableAttributes;

    Table(final String name, final Schema schema) {
        Assert.isTrue(StringUtils.hasText(name), "Table name required");
        Assert.notNull(schema, "Table schema required");
        this.name = name;
        this.schema = schema;
    }

    public String getCatalog() {
        return StringUtils.trimToNull(catalog);
    }

    public void setCatalog(final String catalog) {
        this.catalog = catalog;
    }

    public String getName() {
        return name;
    }

    public Schema getSchema() {
        return schema;
    }

    public String getFullyQualifiedTableName() {
        return DbreModelService.NO_SCHEMA_REQUIRED.equals(schema.getName()) ? name
                : (schema.getName() + "." + name);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public boolean isJoinTable() {
        return joinTable;
    }

    public void setJoinTable(final boolean joinTable) {
        this.joinTable = joinTable;
    }

    public Set<Column> getColumns() {
        return columns;
    }

    public int getColumnCount() {
        return columns.size();
    }

    public boolean addColumn(final Column column) {
        Assert.notNull(column, "Column required");
        return columns.add(column);
    }

    public Column findColumn(final String name) {
        for (Column column : columns) {
            if (column.getName().equalsIgnoreCase(name)) {
                return column;
            }
        }
        return null;
    }

    public Set<Column> getPrimaryKeys() {
        Set<Column> primaryKeys = new LinkedHashSet<Column>();
        for (Column column : columns) {
            if (column.isPrimaryKey()) {
                primaryKeys.add(column);
            }
        }
        return primaryKeys;
    }

    public int getPrimaryKeyCount() {
        return getPrimaryKeys().size();
    }

    public Set<ForeignKey> getImportedKeys() {
        return importedKeys;
    }

    public int getImportedKeyCount() {
        return importedKeys.size();
    }

    public ForeignKey getImportedKey(final String name) {
        for (ForeignKey foreignKey : importedKeys) {
            Assert.isTrue(StringUtils.hasText(foreignKey.getName()),
                    "Foreign key name required");
            if (foreignKey.getName().equalsIgnoreCase(name)) {
                return foreignKey;
            }
        }
        return null;
    }

    public int getImportedKeyCountByForeignTableName(
            final String foreignTableName) {
        int count = 0;
        for (ForeignKey foreignKey : importedKeys) {
            if (foreignKey.getForeignTableName().equalsIgnoreCase(
                    foreignTableName)) {
                count++;
            }
        }
        return count;
    }

    public boolean addImportedKey(final ForeignKey foreignKey) {
        Assert.notNull(foreignKey, "Foreign key required");
        return importedKeys.add(foreignKey);
    }

    public ForeignKey findImportedKeyByLocalColumnName(
            final String localColumnName) {
        for (ForeignKey foreignKey : importedKeys) {
            for (Reference reference : foreignKey.getReferences()) {
                if (reference.getLocalColumnName().equalsIgnoreCase(
                        localColumnName)) {
                    return foreignKey;
                }
            }
        }
        return null;
    }

    public Set<ForeignKey> getExportedKeys() {
        return exportedKeys;
    }

    public int getExportedKeyCountByForeignTableName(
            final String foreignTableName) {
        int count = 0;
        for (ForeignKey exportedKey : exportedKeys) {
            if (exportedKey.getForeignTableName().equalsIgnoreCase(
                    foreignTableName)) {
                count++;
            }
        }
        return count;
    }

    public boolean addExportedKey(final ForeignKey exportedKey) {
        Assert.notNull(exportedKey, "Exported key required");
        return exportedKeys.add(exportedKey);
    }

    public Set<Index> getIndices() {
        return indices;
    }

    public boolean addIndex(final Index index) {
        Assert.notNull(index, "Index required");
        return indices.add(index);
    }

    public boolean isIncludeNonPortableAttributes() {
        return includeNonPortableAttributes;
    }

    public void setIncludeNonPortableAttributes(
            final boolean includeNonPortableAttributes) {
        this.includeNonPortableAttributes = includeNonPortableAttributes;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + name.hashCode();
        result = prime * result + schema.hashCode();
        return result;
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
        Table other = (Table) obj;
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

    @Override
    public String toString() {
        return String
                .format("Table [name=%s, schema=%s, catalog=%s, description=%s, columns=%s, importedKeys=%s, exportedKeys=%s, indices=%s, includeNonPortableAttributes=%s]",
                        name, schema.getName(), catalog, description, columns,
                        importedKeys, exportedKeys, indices,
                        includeNonPortableAttributes);
    }
}
