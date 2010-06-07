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

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fkTable == null) ? 0 : fkTable.hashCode());
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
		ForeignKey other = (ForeignKey) obj;
		if (fkTable == null) {
			if (other.fkTable != null)
				return false;
		} else if (!fkTable.equals(other.fkTable))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	public String toString() {
		return "    FK_NAME " + name + ", FKTABLE_NAME " + fkTable;
	}
}
