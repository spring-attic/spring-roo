package org.springframework.roo.addon.dbre.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.support.util.Assert;

/**
 * Represents the database model, ie. the tables in the database.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class Database implements Serializable {
	private static final long serialVersionUID = -6699287170489794958L;

	/** The name of the database model. Defaults to the catalog name if the schema name is not available. */
	private String name;

	/** All tables. */
	private Set<Table> tables;

	/** The JavaPackage where entities are created */
	private JavaPackage destinationPackage;
	
	/** Whether to create integration tests */
	private boolean testAutomatically;
	
	/** Whether or not to included non-portable JPA attribues in the @Column annotation */
	private boolean includeNonPortableAttributes;
	
	Database(String name, Set<Table> tables) {
		Assert.hasText(name, "Database name required");
		Assert.notNull(tables, "Tables required");
		this.name = name;
		this.tables = tables;
		initialize();
	}

	public String getName() {
		return name;
	}

	public Schema getSchema() {
		return new Schema(name);
	}

	public Set<Table> getTables() {
		return Collections.unmodifiableSet(tables);
	}

	public boolean hasTables() {
		return !tables.isEmpty();
	}

	public Table getTable(String name) {
		for (Table table : tables) {
			if (table.getName().equals(name)) {
				return table;
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

	/**
	 * Initialises the model by establishing the relationships between elements in this model eg. in foreign keys etc.
	 */
	private void initialize() {
		for (Table table : tables) {
			initializeImportedKeys(table);
			initializeExportedKeys(table);
			initializeIndices(table);
			initializeJoinTable(table);
		}
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
			Table targetTable = getTable(foreignTableName);
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
			if (foreignKeys.size() <= 1) {
				continue;
			}
			fk: for (ForeignKey foreignKey : foreignKeys) {
				if (foreignKey.getReferenceCount() == 1) {
					Reference reference = foreignKey.getReferences().iterator().next();
					reference.setInsertableOrUpdatable(false);
					break fk;
				}
				
				for (Reference reference : foreignKey.getReferences()) {
					reference.setInsertableOrUpdatable(false);
				}
				break fk;
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
			Table targetTable = getTable(foreignTableName);
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
}
