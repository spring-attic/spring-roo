package org.springframework.roo.addon.dbre.model;

import java.util.Comparator;

/**
 * Sorts {@link Column column}s by the ordinal position.
 * 
 * <p>
 * Note: this comparator imposes orderings that are inconsistent with equals.
 * 
 * @author Alan Stewart.
 * @since 1.1
 */
public class ColumnComparator implements Comparator<Column> {

	public int compare(Column o1, Column o2) {
		return new Integer(o1.getOrdinalPosition()).compareTo(new Integer(o2.getOrdinalPosition()));
	}
}
