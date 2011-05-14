package org.springframework.roo.addon.dbre.model;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.roo.support.util.Assert;

/**
 * Represents a database foreign key.
 * <p>
 * A foreign key is modeled from the {@link java.sql.DatabaseMetaData#getImportedKeys(String, String, String)} or {@link java.sql.DatabaseMetaData#getExportedKeys(String, String, String)} methods.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class ForeignKey implements Serializable {
	private static final long serialVersionUID = -45977088025460000L;

	/** The name of the foreign key, may be <code>null</code>. */
	private String name;
	
	/** Whether the foreign key is an imported or exported key */
	private boolean exported;

	/** The target table. */
	private Table foreignTable;

	/** The name of the foreign (target) table. */
	private String foreignTableName;

	/** The action to perform when the value of the referenced column changes. */
	private CascadeAction onUpdate = CascadeAction.NONE;

	/** The action to perform when the referenced row is deleted. */
	private CascadeAction onDelete = CascadeAction.NONE;

	/** The sequence number of the key within the table for a given foreign table. */
	private Short keySequence;

	/** The references between local and remote columns. */
	private Set<Reference> references = new LinkedHashSet<Reference>();

	ForeignKey(String name, String foreignTableName) {
		this.name = name;
		this.foreignTableName = foreignTableName;
	}

	public String getName() {
		return name;
	}

	public boolean isExported() {
		return exported;
	}

	public void setExported(boolean exported) {
		this.exported = exported;
	}

	public Table getForeignTable() {
		return foreignTable;
	}

	public void setForeignTable(Table foreignTable) {
		this.foreignTable = foreignTable;
	}

	public String getForeignTableName() {
		return foreignTableName;
	}

	public CascadeAction getOnUpdate() {
		return onUpdate;
	}

	public void setOnUpdate(CascadeAction onUpdate) {
		this.onUpdate = onUpdate;
	}

	public CascadeAction getOnDelete() {
		return onDelete;
	}

	public void setOnDelete(CascadeAction onDelete) {
		this.onDelete = onDelete;
	}

	public Short getKeySequence() {
		return keySequence;
	}

	public void setKeySequence(Short keySequence) {
		this.keySequence = keySequence;
	}

	public Set<Reference> getReferences() {
		return references;
	}

	public int getReferenceCount() {
		return references.size();
	}

	public void addReference(Reference reference) {
		Assert.notNull(reference, "Reference required");
		references.add(reference);
	}

	public boolean hasLocalColumn(Column column) {
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
		result = prime * result + ((foreignTable == null) ? 0 : foreignTable.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ForeignKey other = (ForeignKey) obj;
		if (exported != other.exported)
			return false;
		if (foreignTable == null) {
			if (other.foreignTable != null)
				return false;
		} else if (!foreignTable.equals(other.foreignTable))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	public String toString() {
		return String.format("ForeignKey [name=%s, exported=%s, foreignTable=%s, onUpdate=%s, onDelete=%s, keySequence=%s, references=%s]", name, exported, foreignTableName, onUpdate, onDelete, keySequence, references);
	}
}
