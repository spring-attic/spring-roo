package org.springframework.roo.addon.dbre.model;

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang3.Validate;

/**
 * Represents a database foreign key.
 * <p>
 * A foreign key is modeled from the
 * {@link java.sql.DatabaseMetaData#getImportedKeys(String, String, String)} or
 * {@link java.sql.DatabaseMetaData#getExportedKeys(String, String, String)}
 * methods.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class ForeignKey {

    /** Whether the foreign key is an imported or exported key */
    private boolean exported;

    /** The schema name of the foreign table. */
    private String foreignSchemaName;

    /** The target table. */
    private Table foreignTable;

    /** The name of the foreign (target) table. */
    private final String foreignTableName;

    /**
     * The sequence number of the key within the table for a given foreign
     * table.
     */
    private Short keySequence;

    /** The name of the foreign key, may be <code>null</code>. */
    private final String name;

    /** The action to perform when the referenced row is deleted. */
    private CascadeAction onDelete = CascadeAction.NONE;

    /** The action to perform when the value of the referenced column changes. */
    private CascadeAction onUpdate = CascadeAction.NONE;

    /** The references between local and remote columns. */
    private final Set<Reference> references = new LinkedHashSet<Reference>();

    ForeignKey(final String name, final String foreignTableName) {
        this.name = name;
        this.foreignTableName = foreignTableName;
    }

    public void addReference(final Reference reference) {
        Validate.notNull(reference, "Reference required");
        references.add(reference);
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
        final ForeignKey other = (ForeignKey) obj;
        if (exported != other.exported) {
            return false;
        }
        if (foreignSchemaName == null) {
            if (other.foreignSchemaName != null) {
                return false;
            }
        }
        else if (!foreignSchemaName.equals(other.foreignSchemaName)) {
            return false;
        }
        if (foreignTableName == null) {
            if (other.foreignTableName != null) {
                return false;
            }
        }
        else if (!foreignTableName.equals(other.foreignTableName)) {
            return false;
        }
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

    String getForeignSchemaName() {
        return foreignSchemaName;
    }

    public Table getForeignTable() {
        return foreignTable;
    }

    String getForeignTableName() {
        return foreignTableName;
    }

    public Short getKeySequence() {
        return keySequence;
    }

    public String getName() {
        return name;
    }

    public CascadeAction getOnDelete() {
        return onDelete;
    }

    public CascadeAction getOnUpdate() {
        return onUpdate;
    }

    public int getReferenceCount() {
        return references.size();
    }

    public Set<Reference> getReferences() {
        return references;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (exported ? 1231 : 1237);
        result = prime
                * result
                + (foreignSchemaName == null ? 0 : foreignSchemaName.hashCode());
        result = prime * result
                + (foreignTableName == null ? 0 : foreignTableName.hashCode());
        result = prime * result + (name == null ? 0 : name.hashCode());
        return result;
    }

    public boolean hasLocalColumn(final Column column) {
        for (final Reference reference : references) {
            if (reference.getLocalColumn().equals(column)) {
                return true;
            }
        }
        return false;
    }

    public boolean isExported() {
        return exported;
    }

    public void setExported(final boolean exported) {
        this.exported = exported;
    }

    void setForeignSchemaName(final String foreignSchemaName) {
        this.foreignSchemaName = foreignSchemaName;
    }

    void setForeignTable(final Table foreignTable) {
        this.foreignTable = foreignTable;
    }

    public void setKeySequence(final Short keySequence) {
        this.keySequence = keySequence;
    }

    public void setOnDelete(final CascadeAction onDelete) {
        this.onDelete = onDelete;
    }

    public void setOnUpdate(final CascadeAction onUpdate) {
        this.onUpdate = onUpdate;
    }

    @Override
    public String toString() {
        return String
                .format("ForeignKey [name=%s, exported=%s, foreignTableName=%s, foreignSchemaName=%s, onUpdate=%s, onDelete=%s, keySequence=%s, references=%s]",
                        name, exported, foreignTableName, foreignSchemaName,
                        onUpdate, onDelete, keySequence, references);
    }
}
