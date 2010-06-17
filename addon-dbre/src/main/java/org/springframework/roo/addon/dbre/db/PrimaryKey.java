package org.springframework.roo.addon.dbre.db;

import java.util.ArrayList;
import java.util.List;

/**
 * Primary key metadata.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class PrimaryKey {
	private final String name;
	private final String columnName;
	private final Short keySeq;
	private final List<Column> columns = new ArrayList<Column>();

	PrimaryKey(String name, String columnName, Short keySeq) {
		this.name = name;
		this.columnName = columnName;
		this.keySeq = keySeq;
	}

	public String getId() {
		return name + "." + columnName;
	}

	public String getName() {
		return name;
	}

	public String getColumnName() {
		return columnName;
	}

	public Short getKeySeq() {
		return keySeq;
	}

	void addColumn(Column column) {
		if (column != null) {
			columns.add(column);
		}
	}

	public List<Column> getColumns() {
		return columns;
	}

	public String toString() {
		return "    PK_NAME " + name + ", COLUMN_NAME " + columnName + ", KEY_SEQ " + keySeq.toString();
	}
}
