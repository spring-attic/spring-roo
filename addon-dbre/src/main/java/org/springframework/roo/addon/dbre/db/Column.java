package org.springframework.roo.addon.dbre.db;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
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
		this.name = rs.getString("COLUMN_NAME");
		this.dataType = rs.getInt("DATA_TYPE");
		this.columnSize = rs.getInt("COLUMN_SIZE");
		this.decimalDigits = rs.getInt("DECIMAL_DIGITS");
		this.isNullable = rs.getBoolean("IS_NULLABLE");
		this.remarks = rs.getString("REMARKS");
		this.typeName = new StringTokenizer(rs.getString("TYPE_NAME"), "() ").nextToken();
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

	public Class<?> getType() {
		Class<?> type;
		switch (dataType) {
			case Types.INTEGER:
				type = Integer.class;
				break;
			case Types.FLOAT:
				type = Float.class;
				break;
			case Types.DOUBLE:
			case Types.DECIMAL:
				type = Double.class;
				break;
			case Types.NUMERIC:
				type = BigDecimal.class;
				break;
			case Types.BIGINT:
				type = BigInteger.class;
				break;
			case Types.BOOLEAN:
			case Types.BIT:
				type = Boolean.class;
				break;
			case Types.DATE:
			case Types.TIME:
			case Types.TIMESTAMP:
				type = Date.class;
				break;
			case Types.VARCHAR:
			case Types.CHAR:
				type = String.class;
				break;
			default:
				type = String.class;
				break;
		}
		return type;
	}

	public String toString() {
		return "    COLUMN_NAME " + name + " (" + getType().getName() + "), TYPE_NAME " + typeName + ", DATA_TYPE " + dataType + ", COLUMN_SIZE " + columnSize + ", DECIMAL_DIGITS " + decimalDigits + ", IS_NULLABLE " + isNullable + ", REMARKS " + remarks;
	}
}
