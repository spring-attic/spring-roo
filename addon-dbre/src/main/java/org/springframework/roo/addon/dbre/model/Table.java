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
	private Schema schema;
	private String name;
	private String description;
	private boolean joinTable;
	private Set<Column> columns = new LinkedHashSet<Column>();
	private Set<ForeignKey> importedKeys = new LinkedHashSet<ForeignKey>();
	private Set<ForeignKey> exportedKeys = new LinkedHashSet<ForeignKey>();
	private Set<Index> indices = new LinkedHashSet<Index>();
	private boolean includeNonPortableAttributes;

	Table() {
	}

	public String getCatalog() {
		return catalog;
	}

	public void setCatalog(String catalog) {
		this.catalog = catalog;
	}

	public Schema getSchema() {
		return schema;
	}

	public void setSchema(Schema schema) {
		this.schema = schema;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		Assert.isTrue(StringUtils.hasText(name), "Table name required");
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isJoinTable() {
		return joinTable;
	}

	public void setJoinTable(boolean joinTable) {
		this.joinTable = joinTable;
	}

	public Set<Column> getColumns() {
		return columns;
	}

	public int getColumnCount() {
		return columns.size();
	}

	public boolean addColumn(Column column) {
		Assert.notNull(column, "Column required");
		return columns.add(column);
	}

	public Column findColumn(String name) {
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

	public ForeignKey getImportedKey(String name) {
		for (ForeignKey foreignKey : importedKeys) {
			Assert.isTrue(StringUtils.hasText(foreignKey.getName()), "Foreign key name required");
			if (foreignKey.getName().equalsIgnoreCase(name)) {
				return foreignKey;
			}
		}
		return null;
	}

	public int getImportedKeyCountByForeignTableName(String foreignTableName) {
		int count = 0;
		for (ForeignKey foreignKey : importedKeys) {
			if (foreignKey.getForeignTableName().equalsIgnoreCase(foreignTableName)) {
				count++;
			}
		}
		return count;
	}

	public boolean addImportedKey(ForeignKey foreignKey) {
		Assert.notNull(foreignKey, "Foreign key required");
		return importedKeys.add(foreignKey);
	}

	public ForeignKey findImportedKeyByLocalColumnName(String localColumnName) {
		for (ForeignKey foreignKey : importedKeys) {
			for (Reference reference : foreignKey.getReferences()) {
				if (reference.getLocalColumnName().equalsIgnoreCase(localColumnName)) {
					return foreignKey;
				}
			}
		}
		return null;
	}

	public Set<ForeignKey> getExportedKeys() {
		return exportedKeys;
	}

	public int getExportedKeyCountByForeignTableName(String foreignTableName) {
		int count = 0;
		for (ForeignKey exportedKey : exportedKeys) {
			if (exportedKey.getForeignTableName().equalsIgnoreCase(foreignTableName)) {
				count++;
			}
		}
		return count;
	}

	public boolean addExportedKey(ForeignKey exportedKey) {
		Assert.notNull(exportedKey, "Exported key required");
		return exportedKeys.add(exportedKey);
	}

	public Set<Index> getIndices() {
		return indices;
	}

	public boolean addIndex(Index index) {
		Assert.notNull(index, "Index required");
		return indices.add(index);
	}
	
	public boolean isIncludeNonPortableAttributes() {
		return includeNonPortableAttributes;
	}

	public void setIncludeNonPortableAttributes(boolean includeNonPortableAttributes) {
		this.includeNonPortableAttributes = includeNonPortableAttributes;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((schema == null) ? 0 : schema.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
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
		} else if (!name.equalsIgnoreCase(other.name)) {
			return false;
		}
		if (schema == null) {
			if (other.schema != null) {
				return false;
			}
		} else if (!schema.equals(other.schema)) {
			return false;
		}
		return true;
	}

	public String toString() {
		return String.format("Table [name=%s, catalog=%s, schema=%s, description=%s, columns=%s, importedKeys=%s, exportedKeys=%s, indices=%s, includeNonPortableAttributes=%s]", name, catalog, (schema != null ? schema.getName() : ""), description, columns, importedKeys, exportedKeys, indices, includeNonPortableAttributes);
	}
}
