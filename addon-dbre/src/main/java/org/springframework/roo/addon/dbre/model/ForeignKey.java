package org.springframework.roo.addon.dbre.model;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.roo.support.util.Assert;

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

    /** The name of the foreign key, may be <code>null</code>. */
    private final String name;

    /** Whether the foreign key is an imported or exported key */
    private boolean exported;

    /** The target table. */
    private Table foreignTable;

    /** The name of the foreign (target) table. */
    private final String foreignTableName;

    /** The schema name of the foreign table. */
    private String foreignSchemaName;

    /** The action to perform when the value of the referenced column changes. */
    private CascadeAction onUpdate = CascadeAction.NONE;

    /** The action to perform when the referenced row is deleted. */
    private CascadeAction onDelete = CascadeAction.NONE;

    /**
     * The sequence number of the key within the table for a given foreign
     * table.
     */
    private Short keySequence;

    /** The references between local and remote columns. */
    private final Set<Reference> references = new LinkedHashSet<Reference>();

    ForeignKey(final String name, final String foreignTableName) {
        this.name = name;
        this.foreignTableName = foreignTableName;
    }

    public String getName() {
        return name;
    }

    public boolean isExported() {
        return exported;
    }

    public void setExported(final boolean exported) {
        this.exported = exported;
    }

    public Table getForeignTable() {
        return foreignTable;
    }

    void setForeignTable(final Table foreignTable) {
        this.foreignTable = foreignTable;
    }

    String getForeignTableName() {
        return foreignTableName;
    }

    String getForeignSchemaName() {
        return foreignSchemaName;
    }

    void setForeignSchemaName(final String foreignSchemaName) {
        this.foreignSchemaName = foreignSchemaName;
    }

    public CascadeAction getOnUpdate() {
        return onUpdate;
    }

    public void setOnUpdate(final CascadeAction onUpdate) {
        this.onUpdate = onUpdate;
    }

    public CascadeAction getOnDelete() {
        return onDelete;
    }

    public void setOnDelete(final CascadeAction onDelete) {
        this.onDelete = onDelete;
    }

    public Short getKeySequence() {
        return keySequence;
    }

    public void setKeySequence(final Short keySequence) {
        this.keySequence = keySequence;
    }

    public Set<Reference> getReferences() {
        return references;
    }

    public int getReferenceCount() {
        return references.size();
    }

    public void addReference(final Reference reference) {
        Assert.notNull(reference, "Reference required");
        references.add(reference);
    }

    public boolean hasLocalColumn(final Column column) {
        for (Reference reference : references) {
            if (reference.getLocalColumn().equals(column)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (exported ? 1231 : 1237);
        result = prime
                * result
                + ((foreignSchemaName == null) ? 0 : foreignSchemaName
                        .hashCode());
        result = prime
                * result
                + ((foreignTableName == null) ? 0 : foreignTableName.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ForeignKey other = (ForeignKey) obj;
        if (exported != other.exported)
            return false;
        if (foreignSchemaName == null) {
            if (other.foreignSchemaName != null)
                return false;
        }
        else if (!foreignSchemaName.equals(other.foreignSchemaName))
            return false;
        if (foreignTableName == null) {
            if (other.foreignTableName != null)
                return false;
        }
        else if (!foreignTableName.equals(other.foreignTableName))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        }
        else if (!name.equals(other.name))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return String
                .format("ForeignKey [name=%s, exported=%s, foreignTableName=%s, foreignSchemaName=%s, onUpdate=%s, onDelete=%s, keySequence=%s, references=%s]",
                        name, exported, foreignTableName, foreignSchemaName,
                        onUpdate, onDelete, keySequence, references);
    }
}
