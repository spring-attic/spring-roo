package org.springframework.roo.addon.dbre.model;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.roo.support.util.Assert;

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
	private Set<Column> columns = new LinkedHashSet<Column>();
	private Set<ForeignKey> foreignKeys = new LinkedHashSet<ForeignKey>();
	private Set<Index> indices = new LinkedHashSet<Index>();

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
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Set<Column> getColumns() {
		return columns;
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

	public Set<ForeignKey> getForeignKeys() {
		return foreignKeys;
	}

	public boolean addForeignKey(ForeignKey foreignKey) {
		Assert.notNull(foreignKey, "Foreign key required");
		return foreignKeys.add(foreignKey);
	}

	public boolean addForeignKeys(Set<ForeignKey> foreignKeys) {
		Assert.notNull(foreignKeys, "Foreign keys required");
		return this.foreignKeys.addAll(foreignKeys);
	}

	public Set<Index> getIndices() {
		return indices;
	}

	public boolean addIndex(Index index) {
		Assert.notNull(index, "Indexrequired");
		return indices.add(index);
	}

	public boolean addIndices(Set<Index> indices) {
		Assert.notNull(indices, "Indices required");
		return this.indices.addAll(indices);
	}

	public String toString() {
		return String.format("Table [name=%s, catalog=%s, schema=%s, description=%s, columns=%s, foreignKeys=%s, indices=%s]", name, catalog, schema.getName(), description, columns, foreignKeys, indices);
	}
}
