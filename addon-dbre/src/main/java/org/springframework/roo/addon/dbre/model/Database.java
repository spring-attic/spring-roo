package org.springframework.roo.addon.dbre.model;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.roo.model.JavaPackage;

/**
 * Represents the database model, ie. the tables in the database.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class Database implements Serializable {
	private static final long serialVersionUID = 4051253195181886730L;

	/** The name of the database model. */
	private String name;

	/** The database {@link Schema schema}. */
	private Schema schema;
	
	/** The package where entities are placed. */
	private JavaPackage javaPackage;

	/** All tables. */
	private Set<Table> tables = new LinkedHashSet<Table>();

	/** Many-to-many join tables. */
	private Set<JoinTable> joinTables = new LinkedHashSet<JoinTable>();

	public Database(String name, Schema schema, JavaPackage javaPackage, Set<Table> tables) {
		this.name = name;
		this.schema = schema;
		this.javaPackage = javaPackage;
		this.tables = tables;
		initialize();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Schema getSchema() {
		return schema;
	}

	public void setSchema(Schema schema) {
		this.schema = schema;
	}

	public JavaPackage getJavaPackage() {
		return javaPackage;
	}

	public void setJavaPackage(JavaPackage javaPackage) {
		this.javaPackage = javaPackage;
	}

	public Set<Table> getTables() {
		return tables;
	}
	
	public boolean hasTables() {
		return !tables.isEmpty();
	}

	public Table findTable(String name) {
		for (Table table : tables) {
			if (table.getName().equalsIgnoreCase(name)) {
				return table;
			}
		}
		return null;
	}

	public Set<JoinTable> getJoinTables() {
		return joinTables;
	}

	public boolean isJoinTable(Table table) {
		for (JoinTable joinTable : joinTables) {
			if (joinTable.getTable().equals(table)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Initialises the model by establishing the relationships between elements in this model eg. in foreign keys etc.
	 */
	private void initialize() {
		for (Table table : tables) {
			for (Column column : table.getColumns()) {
				column.setTable(table);
			}

			for (ForeignKey foreignKey : table.getForeignKeys()) {
				foreignKey.setTable(table);

				if (foreignKey.getForeignTable() == null) {
					Table targetTable = findTable(foreignKey.getForeignTableName());
					if (targetTable != null) {
						foreignKey.setForeignTable(targetTable);
					}
				}

				for (Reference reference : foreignKey.getReferences()) {
					if (reference.getLocalColumn() == null) {
						Column localColumn = table.findColumn(reference.getLocalColumnName());
						if (localColumn != null) {
							reference.setLocalColumn(localColumn);
						}
					}
					if (reference.getForeignColumn() == null && foreignKey.getForeignTable() != null) {
						Column foreignColumn = foreignKey.getForeignTable().findColumn(reference.getForeignColumnName());
						if (foreignColumn != null) {
							reference.setForeignColumn(foreignColumn);
						}
					}
				}
			}

			for (ForeignKey exportedKey : table.getExportedKeys()) {
				exportedKey.setTable(table);

				if (exportedKey.getForeignTable() == null) {
					Table targetTable = findTable(exportedKey.getForeignTableName());
					if (targetTable != null) {
						exportedKey.setForeignTable(targetTable);
					}
				}

				for (Reference reference : exportedKey.getReferences()) {
					if (reference.getLocalColumn() == null) {
						Column localColumn = table.findColumn(reference.getLocalColumnName());
						if (localColumn != null) {
							reference.setLocalColumn(localColumn);
						}
					}
					if (reference.getForeignColumn() == null && exportedKey.getForeignTable() != null) {
						Column foreignColumn = exportedKey.getForeignTable().findColumn(reference.getForeignColumnName());
						if (foreignColumn != null) {
							reference.setForeignColumn(foreignColumn);
						}
					}
				}
			}

			for (Index index : table.getIndices()) {
				index.setTable(table);

				for (IndexColumn indexColumn : index.getColumns()) {
					Column column = table.findColumn(indexColumn.getName());
					if (column != null) {
						indexColumn.setColumn(column);
					}
				}
			}

			addJoinTables(table);
		}
	}

	/**
	 * Determines if a table is a many-to-many join table.
	 * 
	 * <p>
	 * To be identified as a many-to-many join table, the table must have have exactly two primary keys and have exactly two foreign-keys pointing to other entity tables and have no other columns.
	 */
	private void addJoinTables(Table table) {
		boolean equals = table.getColumnCount() == 2 && table.getPrimaryKeyCount() == 2 && table.getForeignKeyCount() == 2 && table.getPrimaryKeyCount() == table.getForeignKeyCount();
		Iterator<Column> iter = table.getColumns().iterator();
		while (equals && iter.hasNext()) {
			Column column = iter.next();
			equals &= table.findForeignKeyByLocalColumnName(column.getName()) != null;
		}
		if (equals) {
			joinTables.add(new JoinTable(table));
		}
	}

	public String toString() {
		return String.format("Database [name=%s, schema=%s, javaPackage=%s, tables=%s]", name, schema, javaPackage, tables);
	}
}
