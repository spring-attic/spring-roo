package org.springframework.roo.addon.dbre.db.metadata;

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
		name = rs.getString("INDEX_NAME");
		columnName = rs.getString("COLUMN_NAME");
		nonUnique = rs.getBoolean("NON_UNIQUE");
		type = new Short(rs.getShort("TYPE"));
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

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((columnName == null) ? 0 : columnName.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + (nonUnique ? 1231 : 1237);
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Index other = (Index) obj;
		if (columnName == null) {
			if (other.columnName != null)
				return false;
		} else if (!columnName.equals(other.columnName))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (nonUnique != other.nonUnique)
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}

	public String toString() {
		return "    INDEX_NAME " + name + ", COLUMN_NAME " + columnName + ", NON_UNIQUE " + nonUnique + ", TYPE " + type;
	}
}
