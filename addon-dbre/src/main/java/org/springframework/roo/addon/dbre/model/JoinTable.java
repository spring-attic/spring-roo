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
	private ForeignKey foreignKey1;
	private ForeignKey foreignKey2;

	JoinTable(Table table) {
		Assert.isTrue(table != null && table.getColumnCount() == 2 && table.getPrimaryKeyCount() == 2 && table.getImportedKeyCount() == 2 && table.getPrimaryKeyCount() == table.getImportedKeyCount(), "Table must have have exactly two primary keys and have exactly two foreign-keys pointing to other entity tables and have no other columns");
		this.table = table;
		Iterator<ForeignKey> iter = table.getImportedKeys().iterator();
		this.foreignKey1 = iter.next(); // First foreign key in set
		this.foreignKey2 = iter.next(); // Second and last foreign key in set
	}

	public Table getTable() {
		return table;
	}

	public String getTableName() {
		return table.getName();
	}

	public String getOwningSideTableName() {
		return foreignKey1.getForeignTableName();
	}

	public Table getOwningSideTable() {
		return foreignKey1.getForeignTable();
	}

	public String getInverseSideTableName() {
		return foreignKey2.getForeignTableName();
	}

	public Table getInverseSideTable() {
		return foreignKey2.getForeignTable();
	}

	public String getLocalColumnReferenceOfOwningSideTable() {
		return foreignKey1.getReferences().iterator().next().getLocalColumnName();
	}

	public String getLocalColumnReferenceOfInverseSideTable() {
		return foreignKey2.getReferences().iterator().next().getLocalColumnName();
	}

	public boolean isOwningSideSameAsInverseSide() {
		return getOwningSideTableName().equals(getInverseSideTableName());
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
		return String.format("JoinTable [table=%s, owningSideTable=%s, inverseSideTable=%s]", table.getName(), getOwningSideTableName(), getInverseSideTableName());
	}
}
