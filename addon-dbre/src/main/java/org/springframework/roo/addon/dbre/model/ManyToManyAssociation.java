package org.springframework.roo.addon.dbre.model;

import java.util.Iterator;

import org.springframework.roo.support.util.Assert;

/**
 * Represents a many-valued association with many-to-many multiplicity.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class ManyToManyAssociation {
	private Table joinTable;
	private Table owningSideTable;
	private Table inverseSideTable;

	ManyToManyAssociation(Table joinTable) {
		Assert.isTrue(joinTable.getColumnCount() == 2 && joinTable.getPrimaryKeyCount() == 2 && joinTable.getForeignKeyCount() == 2 && joinTable.getPrimaryKeyCount() == joinTable.getForeignKeyCount(), "Table must have have exactly two primary keys and have exactly two foreign-keys pointing to other entity tables and have no other columns");
		this.joinTable = joinTable;
		
		Iterator<ForeignKey> iter = this.joinTable.getForeignKeys().iterator();
		this.owningSideTable = iter.next().getForeignTable(); // First table in set
		this.inverseSideTable = iter.next().getForeignTable(); // Second table in set
	}

	public Table getJoinTable() {
		return joinTable;
	}

	public Table getOwningSideTable() {
		return owningSideTable;
	}

	public String getPrimaryKeyOfOwningSideTable() {
		return joinTable.getForeignKeys().iterator().next().getReferences().iterator().next().getForeignColumnName();
	}

	public Table getInverseSideTable() {
		return inverseSideTable; 
	}

	public String getPrimaryKeyOfInverseSideTable() {
		Iterator<ForeignKey> iter = joinTable.getForeignKeys().iterator();
		iter.next(); // Skip owning side
		return iter.next().getReferences().iterator().next().getForeignColumnName();
	}
	
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((joinTable == null) ? 0 : joinTable.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof ManyToManyAssociation)) {
			return false;
		}
		ManyToManyAssociation other = (ManyToManyAssociation) obj;
		if (joinTable == null) {
			if (other.joinTable != null) {
				return false;
			}
		} else if (!joinTable.equals(other.joinTable)) {
			return false;
		}
		return true;
	}

	public String toString() {
		return String.format("ManyToManyAssociation [joinTable=%s, owningSideTable=%s, inverseSideTable=%s]", joinTable.getName(), owningSideTable.getName(), inverseSideTable.getName());
	}
}
