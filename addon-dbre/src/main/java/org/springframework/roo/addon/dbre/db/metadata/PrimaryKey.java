package org.springframework.roo.addon.dbre.db.metadata;

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
		return "    PK_NAME " + name +  ", COLUMN_NAME " + columnName  + ", KEY_SEQ " + keySeq.toString();
	}
}
