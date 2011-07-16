package org.springframework.roo.addon.dbre.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Represents the database model, ie. the tables in the database.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class Database {

	/** All tables. */
	private Set<Table> tables;

	/** The JavaPackage where entities are created */
	private JavaPackage destinationPackage;
	
	/** Whether to create integration tests */
	private boolean testAutomatically;
	
	/** Whether or not to included non-portable JPA attribues in the @Column annotation */
	private boolean includeNonPortableAttributes;
	
	private boolean multipleSchemas;
	
	Database(Set<Table> tables) {
		Assert.notNull(tables, "Tables required");
		this.tables = tables;
		initialize();
	}

	public Set<Table> getTables() {
		return Collections.unmodifiableSet(tables);
	}

	public boolean hasTables() {
		return !tables.isEmpty();
	}

	public Table getTable(String name, String schemaName) {
		for (Table table : tables) {
			if (table.getName().equals(name)) {
				if (!StringUtils.hasText(schemaName) || DbreModelService.NO_SCHEMA_REQUIRED.equals(schemaName) || table.getSchema().getName().equals(schemaName)) {
					return table;
				}
			}
		}
		return null;
	}

	public JavaPackage getDestinationPackage() {
		return destinationPackage;
	}

	public void setDestinationPackage(JavaPackage destinationPackage) {
		this.destinationPackage = destinationPackage;
	}

	public boolean isTestAutomatically() {
		return testAutomatically;
	}

	public void setTestAutomatically(boolean testAutomatically) {
		this.testAutomatically = testAutomatically;
	}

	public boolean isIncludeNonPortableAttributes() {
		return includeNonPortableAttributes;
	}

	public void setIncludeNonPortableAttributes(boolean includeNonPortableAttributes) {
		this.includeNonPortableAttributes = includeNonPortableAttributes;
	}

	public boolean hasMultipleSchemas() {
		return multipleSchemas;
	}

	/**
	 * Initialises the model by establishing the relationships between elements in this model eg. in foreign keys etc.
	 */
	private void initialize() {
		Set<Schema> schemas = new HashSet<Schema>();
		for (Table table : tables) {
			schemas.add(table.getSchema());
			initializeImportedKeys(table);
			initializeExportedKeys(table);
			initializeIndices(table);
			initializeJoinTable(table);
		}
		multipleSchemas = schemas.size() > 1;
	}

	private void initializeImportedKeys(Table table) {
		Map<String, Short> keySequenceMap = new LinkedHashMap<String, Short>();
		Short keySequence = null;
		Map<Column, Set<ForeignKey>> repeatedColumns = new LinkedHashMap<Column, Set<ForeignKey>>();

		for (ForeignKey foreignKey : table.getImportedKeys()) {
			if (foreignKey.getForeignTable() != null) {
				continue;
			}
			String foreignTableName = foreignKey.getForeignTableName();
			String foreignSchemaName = foreignKey.getForeignSchemaName();
			Table targetTable = getTable(foreignTableName, foreignSchemaName);
			if (targetTable != null) {
				keySequence = keySequenceMap.get(foreignTableName);
				if (keySequence == null) {
					keySequence = 0;
					keySequenceMap.put(foreignTableName, keySequence);
				}
				foreignKey.setForeignTable(targetTable);
				if (table.getImportedKeyCountByForeignTableName(foreignTableName) > 1) {
					keySequenceMap.put(foreignTableName, new Short((short) (keySequence.shortValue() + 1)));
				}
				foreignKey.setKeySequence(keySequence);
			}

			for (Reference reference : foreignKey.getReferences()) {
				if (reference.getLocalColumn() == null) {
					Column localColumn = table.findColumn(reference.getLocalColumnName());
					if (localColumn != null) {
						reference.setLocalColumn(localColumn);

						Set<ForeignKey> fkSet = repeatedColumns.containsKey(localColumn) ? repeatedColumns.get(localColumn) : new LinkedHashSet<ForeignKey>();
						fkSet.add(foreignKey);
						repeatedColumns.put(localColumn, fkSet);
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

		// Mark repeated columns with insertable = false and updatable = false
		for (Map.Entry<Column, Set<ForeignKey>> entrySet : repeatedColumns.entrySet()) {
			Set<ForeignKey> foreignKeys = entrySet.getValue();
			for (ForeignKey foreignKey : foreignKeys) {
				if (foreignKeys.size() > 1 || foreignKey.getForeignTableName().equals(table.getName())) {
					for (Reference reference : foreignKey.getReferences()) {
						reference.setInsertableOrUpdatable(false);
					}
				}
			}
		}
	}

	private void initializeExportedKeys(Table table) {
		Map<String, Short> keySequenceMap = new LinkedHashMap<String, Short>();
		Short keySequence = null;

		for (ForeignKey exportedKey : table.getExportedKeys()) {
			if (exportedKey.getForeignTable() != null) {
				continue;
			}
			String foreignTableName = exportedKey.getForeignTableName();
			String foreignSchemaName = exportedKey.getForeignSchemaName();
			Table targetTable = getTable(foreignTableName, foreignSchemaName);
			if (targetTable != null) {
				exportedKey.setForeignTable(targetTable);
				keySequence = keySequenceMap.get(foreignTableName);
				if (keySequence == null) {
					keySequence = 0;
					keySequenceMap.put(foreignTableName, keySequence);
				}
				if (table.getExportedKeyCountByForeignTableName(foreignTableName) > 1) {
					keySequenceMap.put(foreignTableName, new Short((short) (keySequence.shortValue() + 1)));
				}
				exportedKey.setKeySequence(keySequence);
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
	}

	private void initializeIndices(Table table) {
		for (Index index : table.getIndices()) {
			for (IndexColumn indexColumn : index.getColumns()) {
				Column column = table.findColumn(indexColumn.getName());
				if (column != null && index.isUnique()) {
					column.setUnique(true);
				}
			}
		}
	}

	/**
	 * Determines if a table is a many-to-many join table.
	 * <p>
	 * To be identified as a many-to-many join table, the table must have have exactly 
	 * two primary keys and have exactly two foreign-keys pointing to other 
	 * entity tables and have no other columns.
	 */
	private void initializeJoinTable(Table table) {
		boolean equals = table.getColumnCount() == 2 && table.getPrimaryKeyCount() == 2 && table.getImportedKeyCount() == 2 && table.getPrimaryKeyCount() == table.getImportedKeyCount();
		Iterator<Column> iter = table.getColumns().iterator();
		while (equals && iter.hasNext()) {
			Column column = iter.next();
			equals &= table.findImportedKeyByLocalColumnName(column.getName()) != null;
		}
		if (equals) {
			table.setJoinTable(true);
		}
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((tables == null) ? 0 : tables.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Database other = (Database) obj;
		if (tables == null) {
			if (other.tables != null)
				return false;
		} else if (!tables.equals(other.tables))
			return false;
		return true;
	}

	public String toString() {
		return String.format("Database [tables=%s, destinationPackage=%s, testAutomatically=%s, includeNonPortableAttributes=%s]", tables, destinationPackage, testAutomatically, includeNonPortableAttributes);
	}
}
