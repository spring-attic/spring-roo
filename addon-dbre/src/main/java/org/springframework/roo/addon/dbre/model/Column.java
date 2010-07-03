package org.springframework.roo.addon.dbre.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Types;
import java.util.Date;

import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaType;

/**
 * Represents a column in the database model.
 * 
 * @author Alan Stewart.
 * @since 1.1
 */
public class Column {
	private String name;
	private String description;
	private boolean primaryKey;
	private boolean required;
	private boolean autoIncrement;
	private int typeCode;
	private String type;
	private int size;
	private int scale;
	private String defaultValue;
	private JavaType javaType;

	Column(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isPrimaryKey() {
		return primaryKey;
	}

	public void setPrimaryKey(boolean primaryKey) {
		this.primaryKey = primaryKey;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public boolean isAutoIncrement() {
		return autoIncrement;
	}

	public void setAutoIncrement(boolean autoIncrement) {
		this.autoIncrement = autoIncrement;
	}

	public int getTypeCode() {
		return typeCode;
	}

	public void setTypeCode(int typeCode) {
		this.typeCode = typeCode;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public int getScale() {
		return scale;
	}

	public void setScale(int scale) {
		this.scale = scale;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public JavaType getJavaType() {
		return javaType;
	}

	public void setJavaType(JavaType javaType) {
		this.javaType = javaType;
	}

	public JavaType getJavaTypeFromTypeCode() {
		JavaType javaType;
		switch (typeCode) {
		case Types.INTEGER:
			javaType = JavaType.INT_OBJECT;
			break;
		case Types.FLOAT:
		case Types.REAL:
			javaType = JavaType.FLOAT_OBJECT;
			break;
		case Types.DOUBLE:
		case Types.DECIMAL:
			javaType = scale > 0 ? JavaType.DOUBLE_OBJECT : JavaType.LONG_OBJECT;
			break;
		case Types.NUMERIC:
			javaType = scale > 0 ? new JavaType(BigDecimal.class.getName()) : JavaType.LONG_OBJECT;
			break;
		case Types.BIGINT:
			javaType = new JavaType(BigInteger.class.getName());
			break;
		case Types.BOOLEAN:
		case Types.BIT:
			javaType = JavaType.BOOLEAN_PRIMITIVE;
			break;
		case Types.DATE:
		case Types.TIME:
		case Types.TIMESTAMP:
			javaType = new JavaType(Date.class.getName());
			break;
		case Types.CHAR:
			javaType = JavaType.CHAR_OBJECT;
			break;
		case Types.VARCHAR:
			javaType = JavaType.STRING_OBJECT;
			break;
		case Types.BLOB:
		case Types.BINARY:
		case Types.VARBINARY:
		case Types.LONGVARBINARY:
			javaType = new JavaType("java.lang.Byte", 1, DataType.PRIMITIVE, null, null);
			break;
		case Types.OTHER:
			javaType = JavaType.STRING_OBJECT;
			break;
		default:
			javaType = JavaType.STRING_OBJECT;
			break;
		}
		return javaType;
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
		return String.format("Column [name=%s, javaType=%s, description=%s, primaryKey=%s, required=%s, autoIncrement=%s, typeCode=%s, type=%s, size=%s, scale=%s, defaultValue=%s]", name, getJavaType(), description, primaryKey, required, autoIncrement, typeCode, type, size, scale, defaultValue);
	}
}
