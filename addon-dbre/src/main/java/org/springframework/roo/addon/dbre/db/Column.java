package org.springframework.roo.addon.dbre.db;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Types;
import java.util.Date;

import org.springframework.roo.model.JavaType;

/**
 * Column metadata.
 * 
 * @author Alan Stewart.
 * @since 1.1
 */
public class Column {
	private final String name;
	private final int dataType;
	private final int columnSize;
	private final int decimalDigits;
	private boolean nullable;
	private final String remarks;
	private final String typeName;

	Column(String name, int dataType, int columnSize, int decimalDigits, boolean nullable, String remarks, String typeName) {
		this.name = name;
		this.dataType = dataType;
		this.columnSize = columnSize;
		this.decimalDigits = decimalDigits;
		this.nullable = nullable;
		this.remarks = remarks;
		this.typeName = typeName;
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

	public boolean isNullable() {
		return nullable;
	}

	public String getRemarks() {
		return remarks;
	}

	public String getTypeName() {
		return typeName;
	}

	public JavaType getType() {
		JavaType type;
		switch (dataType) {
			case Types.INTEGER:
				type = JavaType.INT_OBJECT;
				break;
			case Types.FLOAT:
				type = JavaType.FLOAT_OBJECT;
				break;
			case Types.DOUBLE:
			case Types.DECIMAL:
				type = decimalDigits > 0 ? JavaType.DOUBLE_OBJECT : JavaType.LONG_OBJECT;
				break;
			case Types.NUMERIC:
				type = new JavaType(BigDecimal.class.getName());
				break;
			case Types.BIGINT:
				type = new JavaType(BigInteger.class.getName());
				break;
			case Types.BOOLEAN:
			case Types.BIT:
				type = JavaType.BOOLEAN_OBJECT;
				break;
			case Types.DATE:
			case Types.TIME:
			case Types.TIMESTAMP:
				type = new JavaType(Date.class.getName());
				break;
			case Types.VARCHAR:
			case Types.CHAR:
				type = JavaType.STRING_OBJECT;
				break;
			default:
				type = JavaType.STRING_OBJECT;
				break;
		}
		return type;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Column)) {
			return false;
		}
		Column other = (Column) obj;
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		return true;
	}

	public String toString() {
		return "    COLUMN_NAME " + name + " (" + getType().getFullyQualifiedTypeName() + "), TYPE_NAME " + typeName + ", DATA_TYPE " + dataType + ", COLUMN_SIZE " + columnSize + ", DECIMAL_DIGITS " + decimalDigits + ", IS_NULLABLE " + nullable + ", REMARKS " + remarks;
	}
}
