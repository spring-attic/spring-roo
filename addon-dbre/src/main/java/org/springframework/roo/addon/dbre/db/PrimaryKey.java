package org.springframework.roo.addon.dbre.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.support.util.Assert;

/**
 * JDBC primary key metadata.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class PrimaryKey {
	private final String name;
	private final String columnName;
	private final Short keySeq;
	private final List<Column> columns = new ArrayList<Column>();

	PrimaryKey(ResultSet rs) throws SQLException {
		Assert.notNull(rs, "ResultSet must not be null");
		name = rs.getString("PK_NAME");
		columnName = rs.getString("COLUMN_NAME");
		keySeq = new Short(rs.getShort("KEY_SEQ"));
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

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((columnName == null) ? 0 : columnName.hashCode());
		result = prime * result + ((keySeq == null) ? 0 : keySeq.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PrimaryKey other = (PrimaryKey) obj;
		if (columnName == null) {
			if (other.columnName != null)
				return false;
		} else if (!columnName.equals(other.columnName))
			return false;
		if (keySeq == null) {
			if (other.keySeq != null)
				return false;
		} else if (!keySeq.equals(other.keySeq))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	public String toString() {
		return "    PK_NAME " + name + ", COLUMN_NAME " + columnName + ", KEY_SEQ " + keySeq.toString();
	}
}
