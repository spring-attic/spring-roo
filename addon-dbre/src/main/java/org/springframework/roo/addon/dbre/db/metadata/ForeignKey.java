package org.springframework.roo.addon.dbre.db.metadata;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.support.util.Assert;

/**
 * JDBC foreign key metadata.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class ForeignKey {
	private final String name;
	private final String fkTable;
	private final List<Column> columns = new ArrayList<Column>();

	ForeignKey(ResultSet rs) throws SQLException {
		Assert.notNull(rs, "ResultSet must not be null");
		name = rs.getString("FK_NAME");
		fkTable = rs.getString("FKTABLE_NAME");
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
