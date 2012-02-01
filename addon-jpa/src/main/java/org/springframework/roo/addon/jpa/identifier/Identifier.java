package org.springframework.roo.addon.jpa.identifier;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Represents an entity identifier. Instances are immutable.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class Identifier {

    private final String columnDefinition;
    private final String columnName;
    private final int columnSize;
    private final JavaSymbolName fieldName;
    private final JavaType fieldType;
    private final int scale;

    /**
     * Constructor
     * 
     * @param fieldName required
     * @param fieldType required
     * @param columnName required
     * @param columnSize
     * @param scale
     * @param columnDefinition
     */
    public Identifier(final JavaSymbolName fieldName, final JavaType fieldType,
            final String columnName, final int columnSize, final int scale,
            final String columnDefinition) {
        Validate.notNull(fieldName, "Field name required");
        Validate.notNull(fieldType, "Field type required");
        Validate.notBlank(columnName, "Column name required");
        this.columnDefinition = columnDefinition;
        this.columnName = columnName;
        this.columnSize = columnSize;
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.scale = scale;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Identifier other = (Identifier) obj;
        if (columnDefinition == null) {
            if (other.columnDefinition != null) {
                return false;
            }
        }
        else if (!columnDefinition.equals(other.columnDefinition)) {
            return false;
        }
        if (columnName == null) {
            if (other.columnName != null) {
                return false;
            }
        }
        else if (!columnName.equals(other.columnName)) {
            return false;
        }
        if (columnSize != other.columnSize) {
            return false;
        }
        if (fieldName == null) {
            if (other.fieldName != null) {
                return false;
            }
        }
        else if (!fieldName.equals(other.fieldName)) {
            return false;
        }
        if (fieldType == null) {
            if (other.fieldType != null) {
                return false;
            }
        }
        else if (!fieldType.equals(other.fieldType)) {
            return false;
        }
        if (scale != other.scale) {
            return false;
        }
        return true;
    }

    public String getColumnDefinition() {
        return columnDefinition;
    }

    public String getColumnName() {
        return columnName;
    }

    public int getColumnSize() {
        return columnSize;
    }

    public JavaSymbolName getFieldName() {
        return fieldName;
    }

    public JavaType getFieldType() {
        return fieldType;
    }

    public int getScale() {
        return scale;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + (columnDefinition == null ? 0 : columnDefinition.hashCode());
        result = prime * result
                + (columnName == null ? 0 : columnName.hashCode());
        result = prime * result + columnSize;
        result = prime * result
                + (fieldName == null ? 0 : fieldName.hashCode());
        result = prime * result
                + (fieldType == null ? 0 : fieldType.hashCode());
        result = prime * result + scale;
        return result;
    }

    @Override
    public String toString() {
        return String
                .format("Identifier [fieldName=%s, fieldType=%s, columnName=%s, columnSize=%s, scale=%s, columnDefinition=%s]",
                        fieldName, fieldType, columnName, columnSize, scale,
                        columnDefinition);
    }
}
