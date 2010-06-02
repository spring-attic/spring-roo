package org.springframework.roo.addon.dbre.db.metadata;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC index metadata.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class Index {
	private final String name;
	private final String columnName;
	private Boolean nonUnique;
	private Short type;
	private final List<Column> columns = new ArrayList<Column>();

	Index(ResultSet rs) throws SQLException {
		name = rs.getString("INDEX_NAME");
		columnName = rs.getString("COLUMN_NAME");
		nonUnique = Boolean.valueOf(rs.getBoolean("NON_UNIQUE"));
		type = new Short(rs.getShort("TYPE"));
	}

	public String getName() {
		return name;
	}

	public String getColumnName() {
		return columnName;
	}

	public Boolean getNonUnique() {
		return nonUnique;
	}

	public Short getType() {
		return type;
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
		return "    INDEX_NAME " + name +  ", COLUMN_NAME " + columnName + ", NON_UNIQUE " + nonUnique + ", TYPE " + type;
		
	}
}
