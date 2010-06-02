package org.springframework.roo.addon.dbre.db.metadata;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.StringTokenizer;

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
	private final String isNullable;
	private final String remarks;
	private final String typeName;

	Column(ResultSet rs) throws SQLException {
		name = rs.getString("COLUMN_NAME");
		dataType = rs.getInt("DATA_TYPE");
		columnSize = rs.getInt("COLUMN_SIZE");
		decimalDigits = rs.getInt("DECIMAL_DIGITS");
		isNullable = rs.getString("IS_NULLABLE");
		remarks = rs.getString("REMARKS");
		typeName = new StringTokenizer(rs.getString("TYPE_NAME"), "() ").nextToken();
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

	public String getIsNullable() {
		return isNullable;
	}

	public String getRemarks() {
		return remarks;
	}

	public String getTypeName() {
		return typeName;
	}

	public String toString() {
		return "    COLUMN_NAME " + name + ", TYPE_NAME " + typeName + ", DATA_TYPE " + dataType + ", COLUMN_SIZE " + columnSize + ", DECIMAL_DIGITS " + decimalDigits + ", IS_NULLABLE " + isNullable + ", REMARKS " + remarks ;
	}
}
