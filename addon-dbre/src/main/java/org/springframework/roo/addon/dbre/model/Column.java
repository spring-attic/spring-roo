package org.springframework.roo.addon.dbre.model;

import java.io.Serializable;

/**
 * Represents a column in the database model.
 * 
 * @author Alan Stewart.
 * @since 1.1
 */
public class Column implements Serializable, Comparable<Column> {
	private static final long serialVersionUID = -4826133462002775388L;
	private String name;
	private String description;
	private Table table;
	private boolean primaryKey;
	private boolean required;
	private boolean autoIncrement;
	private int typeCode;
	private ColumnType type;
	private int size;
	private int scale;
	private String defaultValue;
	private String javaType;
	private String javaName;
	private int ordinalPosition;

	public Column(String name) {
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

	public Table getTable() {
		return table;
	}

	public void setTable(Table table) {
		this.table = table;
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

	public ColumnType getType() {
		return type;
	}

	public void setType(ColumnType type) {
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

	public String getJavaType() {
		return javaType;
	}

	public void setJavaType(String javaType) {
		this.javaType = javaType;
	}

	public String getJavaName() {
		return javaName;
	}

	public void setJavaName(String javaName) {
		this.javaName = javaName;
	}

	public int getOrdinalPosition() {
		return ordinalPosition;
	}

	public void setOrdinalPosition(int ordinalPosition) {
		this.ordinalPosition = ordinalPosition;
	}

	public int compareTo(Column o) {
		return new Integer(ordinalPosition).compareTo(new Integer(o.getOrdinalPosition()));
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
		return String.format("Column [name=%s, description=%s, primaryKey=%s, required=%s, autoIncrement=%s, typeCode=%s, type=%s, size=%s, scale=%s, defaultValue=%s, javaType=%s, javaName=%s, ordinalPosition=%s]", name, description, primaryKey, required, autoIncrement, typeCode, type, size, scale, defaultValue, javaType, javaName, ordinalPosition);
	}
}
