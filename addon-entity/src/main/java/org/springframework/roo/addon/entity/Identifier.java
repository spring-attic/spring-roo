package org.springframework.roo.addon.entity;

import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;

/**
 * Represents an entity identifier.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class Identifier {
	private JavaSymbolName fieldName;
	private JavaType fieldType;
	private String columnName;
	
	public Identifier(JavaSymbolName fieldName, JavaType fieldType, String columnName) {
		Assert.notNull(fieldName, "Field name required");
		Assert.notNull(fieldType, "Field type required");
		Assert.hasText(columnName, "Column name required");
		this.fieldName = fieldName;
		this.fieldType = fieldType;
		this.columnName = columnName;
	}

	public JavaSymbolName getFieldName() {
		return fieldName;
	}

	public JavaType getFieldType() {
		return fieldType;
	}

	public String getColumnName() {
		return columnName;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((columnName == null) ? 0 : columnName.hashCode());
		result = prime * result + ((fieldName == null) ? 0 : fieldName.hashCode());
		result = prime * result + ((fieldType == null) ? 0 : fieldType.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Identifier)) {
			return false;
		}
		Identifier other = (Identifier) obj;
		if (columnName == null) {
			if (other.columnName != null) {
				return false;
			}
		} else if (!columnName.equals(other.columnName)) {
			return false;
		}
		if (fieldName == null) {
			if (other.fieldName != null) {
				return false;
			}
		} else if (!fieldName.equals(other.fieldName)) {
			return false;
		}
		if (fieldType == null) {
			if (other.fieldType != null) {
				return false;
			}
		} else if (!fieldType.equals(other.fieldType)) {
			return false;
		}
		return true;
	}

	public String toString() {
		return String.format("Identifier [fieldName=%s, fieldType=%s, columnName=%s]", fieldName, fieldType, columnName);
	}
}
