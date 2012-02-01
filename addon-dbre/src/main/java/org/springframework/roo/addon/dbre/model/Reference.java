package org.springframework.roo.addon.dbre.model;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * Represents a reference between a column in the local table and a column in
 * another table.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class Reference {

    /** The foreign column. */
    private Column foreignColumn;

    /** The name of the foreign column. */
    private String foreignColumnName;

    private boolean insertableOrUpdatable = true;

    /** The local column. */
    private Column localColumn;

    /** The name of the local column. */
    private String localColumnName;

    /**
     * Creates a new reference between the two given columns.
     * 
     * @param localColumn The local column
     * @param foreignColumn The remote column
     */
    Reference(final Column localColumn, final Column foreignColumn) {
        setLocalColumn(localColumn);
        setForeignColumn(foreignColumn);
    }

    /**
     * Creates a new reference between the two given columns.
     */
    Reference(final String localColumnName, final String foreignColumnName) {
        Validate.isTrue(StringUtils.isNotBlank(localColumnName),
                "Foreign key reference local column name required");
        Validate.isTrue(StringUtils.isNotBlank(foreignColumnName),
                "Foreign key reference foreign column name required");
        this.localColumnName = localColumnName;
        this.foreignColumnName = foreignColumnName;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Reference)) {
            return false;
        }
        final Reference other = (Reference) obj;
        if (foreignColumnName == null) {
            if (other.foreignColumnName != null) {
                return false;
            }
        }
        else if (!foreignColumnName.equals(other.foreignColumnName)) {
            return false;
        }
        if (localColumnName == null) {
            if (other.localColumnName != null) {
                return false;
            }
        }
        else if (!localColumnName.equals(other.localColumnName)) {
            return false;
        }
        return true;
    }

    public Column getForeignColumn() {
        return foreignColumn;
    }

    public String getForeignColumnName() {
        return foreignColumnName;
    }

    public Column getLocalColumn() {
        return localColumn;
    }

    public String getLocalColumnName() {
        return localColumnName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime
                * result
                + (foreignColumnName == null ? 0 : foreignColumnName.hashCode());
        result = prime * result
                + (localColumnName == null ? 0 : localColumnName.hashCode());
        return result;
    }

    public boolean isInsertableOrUpdatable() {
        return insertableOrUpdatable;
    }

    public void setForeignColumn(final Column foreignColumn) {
        this.foreignColumn = foreignColumn;
    }

    public void setInsertableOrUpdatable(final boolean insertableOrUpdatable) {
        this.insertableOrUpdatable = insertableOrUpdatable;
    }

    public void setLocalColumn(final Column localColumn) {
        this.localColumn = localColumn;
    }

    @Override
    public String toString() {
        return String
                .format("Reference [localColumnName=%s, foreignColumnName=%s, insertableOrUpdatable=%s]",
                        localColumnName, foreignColumnName,
                        insertableOrUpdatable);
    }
}
