package org.springframework.roo.addon.dbre.model;

import java.io.Serializable;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Represents a database foreign key.
 * 
 * <p>
 * A foreign key is modelled from the {@link java.sql.DatabaseMetaData#getImportedKeys(String, String, String)} or {@link java.sql.DatabaseMetaData#getExportedKeys(String, String, String)} methods.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class ForeignKey implements Serializable {
	private static final long serialVersionUID = -7679438879219261466L;

	/** The name of the foreign key, may be <code>null</code>. */
	private String name;

	/** The owning table. */
	private Table table;

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
	private SortedSet<Reference> references = new TreeSet<Reference>();

	public ForeignKey() {
	}

	public ForeignKey(String name, String foreignTableName) {
		this.name = name;
		this.foreignTableName = foreignTableName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Table getTable() {
		return table;
	}

	public void setTable(Table table) {
		this.table = table;
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

	public void setForeignTableName(String foreignTableName) {
		this.foreignTableName = foreignTableName;
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
		if (reference != null) {
			references.add(reference);
		}
	}

	public boolean hasLocalColumn(Column column) {
		for (Reference reference : references) {
			if (reference.getLocalColumn().equals(column)) {
				return true;
			}
		}
		return false;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((foreignTableName == null) ? 0 : foreignTableName.hashCode());
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
		if (!(obj instanceof ForeignKey)) {
			return false;
		}
		ForeignKey other = (ForeignKey) obj;
		if (foreignTableName == null) {
			if (other.foreignTableName != null) {
				return false;
			}
		} else if (!foreignTableName.equals(other.foreignTableName)) {
			return false;
		}
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
		return String.format("ForeignKey [name=%s, foreignTable=%s, onUpdate=%s, onDelete=%s, references=%s]", name, foreignTableName, onUpdate, onDelete, references);
	}
}
