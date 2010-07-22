package org.springframework.roo.addon.dbre.model;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.roo.support.util.Assert;

/**
 * Represents a table in the database model.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class Table implements Serializable {
	private static final long serialVersionUID = 6223184549205869347L;
	private String catalog;
	private Schema schema;
	private String name;
	private String description;
	private SortedSet<Column> columns = new TreeSet<Column>();
	private Set<ForeignKey> foreignKeys = new LinkedHashSet<ForeignKey>();
	private Set<ForeignKey> exportedKeys = new LinkedHashSet<ForeignKey>();
	private Set<Index> indices = new LinkedHashSet<Index>();

	public Table() {
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
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public SortedSet<Column> getColumns() {
		return columns;
	}

	public int getColumnCount() {
		return columns.size();
	}

	public boolean addColumns(Set<Column> columns) {
		Assert.notNull(columns, "Columns required");
		return this.columns.addAll(columns);
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

	public Set<ForeignKey> getForeignKeys() {
		return foreignKeys;
	}

	public ForeignKey getForeignKey(String name) {
		for (ForeignKey foreignKey : foreignKeys) {
			if (foreignKey.getName().equalsIgnoreCase(name)) {
				return foreignKey;
			}
		}
		return null;

	}
	public int getForeignKeyCount() {
		return foreignKeys.size();
	}

	public int getForeignKeyCountByForeignTableName(String foreignTableName) {
		int count = 0;
		for (ForeignKey foreignKey : foreignKeys) {
			if (foreignKey.getForeignTableName().equalsIgnoreCase(foreignTableName)) {
				count++;
			}
		}
		return count;
	}

	public boolean addForeignKeys(Set<ForeignKey> foreignKeys) {
		Assert.notNull(foreignKeys, "Foreign keys required");
		return this.foreignKeys.addAll(foreignKeys);
	}

	public boolean addForeignKey(ForeignKey foreignKey) {
		Assert.notNull(foreignKey, "Foreign key required");
		return foreignKeys.add(foreignKey);
	}

	public ForeignKey findForeignKeyByLocalColumnName(String localColumnName) {
		for (ForeignKey foreignKey : foreignKeys) {
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

	public boolean addExportedKeys(Set<ForeignKey> exportedKeys) {
		Assert.notNull(exportedKeys, "Exported keys required");
		return this.exportedKeys.addAll(exportedKeys);
	}

	public boolean addExportedKey(ForeignKey exportedKey) {
		Assert.notNull(exportedKey, "Exported key required");
		return exportedKeys.add(exportedKey);
	}

	public ForeignKey findExportedKeyByLocalColumnName(String localColumnName) {
		for (ForeignKey exportedKey : exportedKeys) {
			for (Reference reference : exportedKey.getReferences()) {
				if (reference.getLocalColumnName().equalsIgnoreCase(localColumnName)) {
					return exportedKey;
				}
			}
		}
		return null;
	}

	public Set<Index> getIndices() {
		return indices;
	}

	public boolean addIndex(Index index) {
		Assert.notNull(index, "Index required");
		return indices.add(index);
	}

	public boolean addIndices(Set<Index> indices) {
		Assert.notNull(indices, "Indices required");
		return this.indices.addAll(indices);
	}

	public Index findUniqueReference(String uniqueColumnName) {
		for (Index index : indices) {			
			if (index.isUnique()) {
				for (IndexColumn column : index.getColumns()) {
					if (column.getName().equalsIgnoreCase(uniqueColumnName)) {
						return index;
					}
				}
			}
		}
		return null;
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
		return String.format("Table [name=%s, catalog=%s, schema=%s, description=%s, columns=%s, foreignKeys=%s, exportedKeys=%s, indices=%s]", name, catalog, schema.getName(), description, columns, foreignKeys, exportedKeys, indices);
	}
}
