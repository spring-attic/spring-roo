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
	private int columnSize;
	private int scale;
	private String columnDefinition;
	
	public Identifier(JavaSymbolName fieldName, JavaType fieldType, String columnName, int columnSize, int scale, String columnDefinition) {
		Assert.notNull(fieldName, "Field name required");
		Assert.notNull(fieldType, "Field type required");
		Assert.hasText(columnName, "Column name required");
		this.fieldName = fieldName;
		this.fieldType = fieldType;
		this.columnName = columnName;
		this.columnSize = columnSize;
		this.scale = scale;
		this.columnDefinition = columnDefinition;
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

	public int getColumnSize() {
		return columnSize;
	}

	public int getScale() {
		return scale;
	}

	public String getColumnDefinition() {
		return columnDefinition;
	}

	@Override 
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((columnDefinition == null) ? 0 : columnDefinition.hashCode());
		result = prime * result + ((columnName == null) ? 0 : columnName.hashCode());
		result = prime * result + columnSize;
		result = prime * result + ((fieldName == null) ? 0 : fieldName.hashCode());
		result = prime * result + ((fieldType == null) ? 0 : fieldType.hashCode());
		result = prime * result + scale;
		return result;
	}

	@Override 
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Identifier other = (Identifier) obj;
		if (columnDefinition == null) {
			if (other.columnDefinition != null) {
				return false;
			}
		} else if (!columnDefinition.equals(other.columnDefinition)) {
			return false;
		}
		if (columnName == null) {
			if (other.columnName != null) {
				return false;
			}
		} else if (!columnName.equals(other.columnName)) {
			return false;
		}
		if (columnSize != other.columnSize) {
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
		if (scale != other.scale) {
			return false;
		}
		return true;
	}

	@Override 
	public String toString() {
		return String.format("Identifier [fieldName=%s, fieldType=%s, columnName=%s, columnSize=%s, scale=%s, columnDefinition=%s]", fieldName, fieldType, columnName, columnSize, scale, columnDefinition);
	}
}
