package org.springframework.roo.addon.dbre.model;

import java.io.Serializable;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.roo.support.util.Assert;

/**
 * Represents an index definition for a table which may be either unique or non-unique.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class Index implements Serializable {
	private static final long serialVersionUID = 3248243308098445623L;
	private String name;
	private Table table;
	private boolean unique;
	private SortedSet<IndexColumn> columns = new TreeSet<IndexColumn>(new IndexColumnComparator());

	Index(String name) {
		this.name = name;
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

	public boolean isUnique() {
		return unique;
	}

	public void setUnique(boolean unique) {
		this.unique = unique;
	}

	public SortedSet<IndexColumn> getColumns() {
		return columns;
	}

	public boolean addColumn(IndexColumn indexColumn) {
		Assert.notNull(indexColumn, "Column required");
		return columns.add(indexColumn);
	}

	public boolean addColumns(List<IndexColumn> indexColumns) {
		Assert.notNull(indexColumns, "Columns required");
		return columns.addAll(indexColumns);
	}

	public boolean removeColumn(IndexColumn indexColumn) {
		Assert.notNull(indexColumn, "Column required");
		return columns.remove(indexColumn);
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
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
		if (!(obj instanceof Index)) {
			return false;
		}
		Index other = (Index) obj;
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
		return String.format("Index [name=%s, unique=%s, columns=%s]", name, unique, columns);
	}
}
