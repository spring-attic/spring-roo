package org.springframework.roo.addon.dbre.model;

import java.io.Serializable;

/**
 * Represents a column of an index in the database model.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class IndexColumn implements Serializable {
	private static final long serialVersionUID = 4206711649555220093L;
	private String name;
	private int ordinalPosition;
	private Column column;
	private int size;

	public IndexColumn(String name) {
		this.name = name;
	}

	IndexColumn() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getOrdinalPosition() {
		return ordinalPosition;
	}

	public void setOrdinalPosition(int ordinalPosition) {
		this.ordinalPosition = ordinalPosition;
	}

	public Column getColumn() {
		return column;
	}

	public void setColumn(Column column) {
		this.column = column;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public String toString() {
		return String.format("IndexColumn [name=%s, ordinalPosition=%s, column=%s, size=%s]", name, ordinalPosition, column, size);
	}
}
