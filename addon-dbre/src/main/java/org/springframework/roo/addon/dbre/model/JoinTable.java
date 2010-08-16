package org.springframework.roo.addon.dbre.model;

import java.util.Iterator;

import org.springframework.roo.support.util.Assert;

/**
 * Represents a join table for a many-valued association with many-to-many multiplicity.
 * 
 * <p>
 * A join table must have have exactly two primary keys and have exactly two foreign-keys 
 * pointing to other entity tables and have no other columns.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class JoinTable {
	private Table table;
	private Table owningSideTable;
	private Table inverseSideTable;

	JoinTable(Table table) {
		Assert.isTrue(table.getColumnCount() == 2 && table.getPrimaryKeyCount() == 2 && table.getForeignKeyCount() == 2 && table.getPrimaryKeyCount() == table.getForeignKeyCount(), "Table must have have exactly two primary keys and have exactly two foreign-keys pointing to other entity tables and have no other columns");
		this.table = table;

		Iterator<ForeignKey> iter = this.table.getForeignKeys().iterator();
		this.owningSideTable = iter.next().getForeignTable(); // First table in set
		this.inverseSideTable = iter.next().getForeignTable(); // Second table in set
	}

	public Table getTable() {
		return table;
	}

	public Table getOwningSideTable() {
		return owningSideTable;
	}

	public Table getInverseSideTable() {
		return inverseSideTable;
	}

	public String getPrimaryKeyOfOwningSideTable() {
		return table.getForeignKeys().iterator().next().getReferences().iterator().next().getForeignColumnName();
	}

	public String getPrimaryKeyOfInverseSideTable() {
		Iterator<ForeignKey> iter = table.getForeignKeys().iterator();
		iter.next(); // Skip owning side
		return iter.next().getReferences().iterator().next().getForeignColumnName();
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((table == null) ? 0 : table.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof JoinTable)) {
			return false;
		}
		JoinTable other = (JoinTable) obj;
		if (table == null) {
			if (other.table != null) {
				return false;
			}
		} else if (!table.equals(other.table)) {
			return false;
		}
		return true;
	}

	public String toString() {
		return String.format("JoinTable [table=%s, owningSideTable=%s, inverseSideTable=%s]", table.getName(), owningSideTable.getName(), inverseSideTable.getName());
	}
}
