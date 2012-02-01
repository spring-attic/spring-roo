package org.springframework.roo.addon.dbre.model;

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang3.Validate;

/**
 * Represents an index definition for a table which may be either unique or
 * non-unique.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class Index {

    private final Set<IndexColumn> columns = new LinkedHashSet<IndexColumn>();
    private String name;
    private boolean unique;

    /**
     * Constructor
     * 
     * @param name
     */
    Index(final String name) {
        this.name = name;
    }

    public boolean addColumn(final IndexColumn indexColumn) {
        Validate.notNull(indexColumn, "Column required");
        return columns.add(indexColumn);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Index)) {
            return false;
        }
        final Index other = (Index) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        }
        else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    public Set<IndexColumn> getColumns() {
        return columns;
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (name == null ? 0 : name.hashCode());
        return result;
    }

    public boolean isUnique() {
        return unique;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setUnique(final boolean unique) {
        this.unique = unique;
    }

    @Override
    public String toString() {
        return String.format("Index [name=%s, unique=%s, columns=%s]", name,
                unique, columns);
    }
}
