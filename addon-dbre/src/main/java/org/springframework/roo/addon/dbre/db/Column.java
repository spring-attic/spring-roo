package org.springframework.roo.addon.dbre.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.StringTokenizer;

import org.springframework.roo.support.util.Assert;

/**
 * JDBC column metadata.
 * 
 * @author Alan Stewart.
 * @since 1.1
 */
public class Column {
	private final String name;
	private final int dataType;
	private final int columnSize;
	private final int decimalDigits;
	private boolean isNullable;
	private final String remarks;
	private final String typeName;

	Column(ResultSet rs) throws SQLException {
		Assert.notNull(rs, "ResultSet must not be null");
		name = rs.getString("COLUMN_NAME");
		dataType = rs.getInt("DATA_TYPE");
		columnSize = rs.getInt("COLUMN_SIZE");
		decimalDigits = rs.getInt("DECIMAL_DIGITS");
		isNullable = rs.getBoolean("IS_NULLABLE");
		remarks = rs.getString("REMARKS");
		typeName = new StringTokenizer(rs.getString("TYPE_NAME"), "() ").nextToken();
	}
	
	public String getId() {
		return name;
	}
	
	public String getName() {
		return name;
	}

	public int getDataType() {
		return dataType;
	}

	public int getColumnSize() {
		return columnSize;
	}

	public int getDecimalDigits() {
		return decimalDigits;
	}

	public boolean getIsNullable() {
		return isNullable;
	}

	public String getRemarks() {
		return remarks;
	}

	public String getTypeName() {
		return typeName;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + columnSize;
		result = prime * result + dataType;
		result = prime * result + decimalDigits;
		result = prime * result + (isNullable ? 1231 : 1237);
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((remarks == null) ? 0 : remarks.hashCode());
		result = prime * result + ((typeName == null) ? 0 : typeName.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Column other = (Column) obj;
		if (columnSize != other.columnSize)
			return false;
		if (dataType != other.dataType)
			return false;
		if (decimalDigits != other.decimalDigits)
			return false;
		if (isNullable != other.isNullable)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (remarks == null) {
			if (other.remarks != null)
				return false;
		} else if (!remarks.equals(other.remarks))
			return false;
		if (typeName == null) {
			if (other.typeName != null)
				return false;
		} else if (!typeName.equals(other.typeName))
			return false;
		return true;
	}

	public String toString() {
		return "    COLUMN_NAME " + name + ", TYPE_NAME " + typeName + ", DATA_TYPE " + dataType + ", COLUMN_SIZE " + columnSize + ", DECIMAL_DIGITS " + decimalDigits + ", IS_NULLABLE " + isNullable + ", REMARKS " + remarks;
	}
}
