package org.springframework.roo.addon.dbre.model;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.support.util.Assert;

/**
 * Represents the database model, ie. the tables in the database.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class Database {

	/** The name of the database model. */
	private String name;

	/** The package where entities are placed */
	private JavaPackage javaPackage;

	/** The tables. */
	private Set<Table> tables = new LinkedHashSet<Table>();

	Database() {
	}

	Database(String name, JavaPackage javaPackage) {
		this.name = name;
		this.javaPackage = javaPackage;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

	public void addTables(Set<Table> tables) {
		Assert.notNull(tables, "Tables required");
		this.tables.addAll(tables);
		initialize();
	}

	public Table findTable(String name) {
		for (Table table : tables) {
			if (table.getName().equalsIgnoreCase(name)) {
				return table;
			}
		}
		return null;
	}

	/**
	 * Initialises the model by establishing the relationships between elements in this model eg. in foreign keys etc.
	 */
	private void initialize() {
		for (Table table : tables) {
			for (ForeignKey foreignKey : table.getForeignKeys()) {
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

			for (Index index : table.getIndices()) {
				for (IndexColumn indexColumn : index.getColumns()) {
					Column column = table.findColumn(indexColumn.getName());
					if (column != null) {
						indexColumn.setColumn(column);
					}
				}
			}
		}
	}

	public String toString() {
		return String.format("Database [name=%s, javaPackage=%s, tables=%s]", name, javaPackage, tables);
	}
}
