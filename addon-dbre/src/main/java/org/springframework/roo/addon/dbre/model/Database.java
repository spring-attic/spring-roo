package org.springframework.roo.addon.dbre.model;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.roo.support.util.Assert;

/**
 * Represents the database model, ie. the tables in the database.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class Database implements Serializable {
	private static final long serialVersionUID = -6066594794515924423L;

	/** The name of the database model. */
	private String name;

	/** The database {@link Schema schema}. */
	private Schema schema;

	/** All tables. */
	private Set<Table> tables = new LinkedHashSet<Table>();

	/** Many-to-many join tables. */
	private Set<JoinTable> joinTables = new LinkedHashSet<JoinTable>();

	/** Database sequences */
	private Set<Sequence> sequences = new LinkedHashSet<Sequence>();

	Database(String name, Schema schema, Set<Table> tables) {
		Assert.notNull(tables, "tables required");
		this.name = name;
		this.schema = schema;
		this.tables = tables;
		initialize();
	}

	public String getName() {
		return name;
	}

	public Schema getSchema() {
		return schema;
	}

	public Set<Table> getTables() {
		return tables;
	}

	public Set<Sequence> getSequences() {
		return sequences;
	}

	public void setSequences(Set<Sequence> sequences) {
		this.sequences = sequences;
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
			initializeColumns(table);
			initializeForeignKeys(table);
			initializeExportedKeys(table);
			initializeIndices(table);
			addJoinTables(table);
		}
	}

	private void initializeColumns(Table table) {
		for (Column column : table.getColumns()) {
			column.setTable(table);
		}
	}

	private void initializeForeignKeys(Table table) {
		Map<String, Short> keySequenceMap = new LinkedHashMap<String, Short>();
		Short keySequence = null;
		Map<Column, Integer> localColumnMap = new LinkedHashMap<Column, Integer>();

		for (ForeignKey foreignKey : table.getForeignKeys()) {
			foreignKey.setTable(table);

			if (foreignKey.getForeignTable() == null) {
				String foreignTableName = foreignKey.getForeignTableName();
				Table targetTable = findTable(foreignTableName);
				if (targetTable != null) {
					keySequence = keySequenceMap.get(foreignTableName);
					if (keySequence == null) {
						keySequence = 0;
						keySequenceMap.put(foreignTableName, keySequence);
					}
					foreignKey.setForeignTable(targetTable);
					if (table.getForeignKeyCountByForeignTableName(foreignTableName) > 1) {
						keySequenceMap.put(foreignTableName, new Short((short) (keySequence.shortValue() + 1)));
					}
					foreignKey.setKeySequence(keySequence);
				}
			}

			for (Reference reference : foreignKey.getReferences()) {
				if (reference.getLocalColumn() == null) {
					Column localColumn = table.findColumn(reference.getLocalColumnName());
					if (localColumn != null) {
						reference.setLocalColumn(localColumn);
						Integer columnCount = localColumnMap.get(localColumn);
						if (columnCount == null) {
							columnCount = 0;
							localColumnMap.put(localColumn, columnCount);
						}
						localColumnMap.put(localColumn, columnCount + 1);
						if (localColumnMap.get(localColumn) > 1) {
							reference.setInsertableOrUpdatable(false);
						}
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
	}

	private void initializeExportedKeys(Table table) {
		Map<String, Short> keySequenceMap = new LinkedHashMap<String, Short>();
		Short keySequence = null;

		for (ForeignKey exportedKey : table.getExportedKeys()) {
			exportedKey.setTable(table);

			if (exportedKey.getForeignTable() == null) {
				String foreignTableName = exportedKey.getForeignTableName();
				Table targetTable = findTable(foreignTableName);
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
			index.setTable(table);

			for (IndexColumn indexColumn : index.getColumns()) {
				Column column = table.findColumn(indexColumn.getName());
				if (column != null) {
					indexColumn.setColumn(column);
				}
			}
		}
	}

	/**
	 * Determines if a table is a many-to-many join table.
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
		if (hasTables()) {
			OutputStream outputStream = new ByteArrayOutputStream();
			DatabaseXmlUtils.writeDatabaseStructureToOutputStream(this, outputStream);
			return outputStream.toString();
		} else {
			return "Schema " + schema.getName() + " does not exist or does not have any tables. Note that the schema names of some databases are case-sensitive";
		}
	}
}
