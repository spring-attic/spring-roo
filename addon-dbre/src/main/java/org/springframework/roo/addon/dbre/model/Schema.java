package org.springframework.roo.addon.dbre.model;

import org.apache.commons.lang3.StringUtils;

/**
 * Represents a schema in the database model.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class Schema {
    private final String name;

    public Schema(final String name) {
        this.name = StringUtils.defaultIfEmpty(name,
                DbreModelService.NO_SCHEMA_REQUIRED);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Schema)) {
            return false;
        }
        final Schema other = (Schema) obj;
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + name.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return name;
    }
}
