package org.springframework.roo.addon.dbre.model;

/**
 * Represents a column of an index in the database model.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class IndexColumn {
    private String name;
    private int size;

    IndexColumn(final String name) {
        this.name = name;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof IndexColumn)) {
            return false;
        }
        final IndexColumn other = (IndexColumn) obj;
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

    public String getName() {
        return name;
    }

    public int getSize() {
        return size;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (name == null ? 0 : name.hashCode());
        return result;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setSize(final int size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return String.format("IndexColumn [name=%s, size=%s]", name, size);
    }
}
