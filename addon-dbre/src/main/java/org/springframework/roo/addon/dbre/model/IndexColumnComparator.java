package org.springframework.roo.addon.dbre.model;

import java.util.Comparator;

/**
 * Sorts {@link IndexColumn}s by the ordinal position.
 * 
 * @author Alan Stewart.
 * @since 1.1
 */
public class IndexColumnComparator implements Comparator<IndexColumn> {

	public int compare(IndexColumn o1, IndexColumn o2) {
		return new Integer(o1.getOrdinalPosition()).compareTo(new Integer(o2.getOrdinalPosition()));
	}
}
