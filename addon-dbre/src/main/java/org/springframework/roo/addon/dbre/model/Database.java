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

			// for (Index index : table.getIndices()) {
			// String indexName = (index.getName() == null ? "" : index.getName());
			// String indexDesc = (indexName.length() == 0 ? "nr. " + idx : indexName);
			//
			// if (indexName.length() > 0) {
			// if (namesOfProcessedIndices.contains(indexName)) {
			// // throw new ModelException("There are multiple indices in table " + curTable.getName() + " with the name " + indexName);
			// }
			// namesOfProcessedIndices.add(indexName);
			// }
			// if (index.getColumnCount() == 0) {
			// // throw new ModelException("The index " + indexDesc + " in table " + curTable.getName() + " does not have any columns");
			// }
			//
			// for (int indexColumnIdx = 0; indexColumnIdx < index.getColumnCount(); indexColumnIdx++) {
			// IndexColumn indexColumn = index.getColumn(indexColumnIdx);
			// Column column = curTable.findColumn(indexColumn.getName(), true);
			//
			// if (column == null) {
			// // throw new ModelException("The index " + indexDesc + " in table " + curTable.getName() + " references the undefined column " + indexColumn.getName());
			// } else {
			// indexColumn.setColumn(column);
			// }
			// }
			// }
		}
	}

	public String toString() {
		return String.format("Database [name=%s, javaPackage=%s, tables=%s]", name, javaPackage, tables);
	}
}
