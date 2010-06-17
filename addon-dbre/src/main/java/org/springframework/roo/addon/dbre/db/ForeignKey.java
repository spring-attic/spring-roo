package org.springframework.roo.addon.dbre.db;

import java.util.ArrayList;
import java.util.List;

/**
 * Foreign key metadata.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class ForeignKey {
	private final String name;
	private final String fkTable;
	private final List<Column> columns = new ArrayList<Column>();

	ForeignKey(String name, String fkTable) {
		this.name = name;
		this.fkTable = fkTable;
	}

	public String getId() {
		return name + "." + fkTable;
	}

	public String getName() {
		return name;
	}

	public String getFkTable() {
		return fkTable;
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
		return "    FK_NAME " + name + ", FKTABLE_NAME " + fkTable;
	}
}
