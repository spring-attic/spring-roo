package org.springframework.roo.addon.dbre.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.support.util.Assert;

/**
 * JDBC index metadata.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class Index {
	private final String name;
	private final String columnName;
	private boolean nonUnique;
	private Short type;
	private final List<Column> columns = new ArrayList<Column>();

	Index(ResultSet rs) throws SQLException {
		Assert.notNull(rs, "ResultSet must not be null");
		this.name = rs.getString("INDEX_NAME");
		this.columnName = rs.getString("COLUMN_NAME");
		this.nonUnique = rs.getBoolean("NON_UNIQUE");
		this.type = new Short(rs.getShort("TYPE"));
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

	public boolean isNonUnique() {
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
		return "    INDEX_NAME " + name + ", COLUMN_NAME " + columnName + ", NON_UNIQUE " + nonUnique + ", TYPE " + type;
	}
}
