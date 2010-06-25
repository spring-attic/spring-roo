package org.springframework.roo.addon.dbre.db;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Types;
import java.util.Date;

import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.StringUtils;

/**
 * Column metadata.
 * 
 * @author Alan Stewart.
 * @since 1.1
 */
public class Column implements Comparable<Column> {
	private final String name;
	private final int dataType;
	private final int columnSize;
	private final int decimalDigits;
	private boolean nullable;
	private final String remarks;
	private final String typeName;
	private final boolean primaryKey; 
	private final Short primaryKeySequence;
	
	Column(String name, int dataType, int columnSize, int decimalDigits, boolean nullable, String remarks, String typeName, boolean primaryKey, Short primaryKeySequence) {
		this.name = name;
		this.dataType = dataType;
		this.columnSize = columnSize;
		this.decimalDigits = decimalDigits;
		this.nullable = nullable;
		this.remarks = remarks;
		this.typeName = typeName;
		this.primaryKey = primaryKey;
		this.primaryKeySequence = primaryKeySequence;
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

	public boolean isPrimaryKey() {
		return primaryKey;
	}

	public Short getPrimaryKeySequence() {
		return primaryKeySequence;
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
			case Types.BLOB:
				// TODO byte arrays
				type = JavaType.BYTE_PRIMITIVE;
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
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Column other = (Column) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	public int compareTo(Column o) {
		if (primaryKeySequence == null && o.getPrimaryKeySequence() == null) return 1;
		if (primaryKeySequence != null && o.getPrimaryKeySequence() == null) return -1;
		if (primaryKeySequence == null && o.getPrimaryKeySequence() != null) return 1;
		return primaryKeySequence.compareTo(o.getPrimaryKeySequence());
	}

	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(primaryKey ?  "    PK " : "       ");
		builder.append(name).append(" (").append(getType().getFullyQualifiedTypeName()).append(")");
		builder.append(", TYPE_NAME").append(typeName);
		builder.append(", DATA_TYPE ").append(dataType);
		builder.append(", COLUMN_SIZE ").append(columnSize);
		builder.append(", DECIMAL_DIGITS ").append(decimalDigits);
		builder.append(", IS_NULLABLE ").append(nullable);
		if (StringUtils.hasText(remarks)) {
			builder.append(", REMARKS ").append(remarks);
		}
		
		return builder.toString();
	}
}
