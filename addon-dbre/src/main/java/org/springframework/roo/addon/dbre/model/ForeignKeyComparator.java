package org.springframework.roo.addon.dbre.model;

import java.util.Comparator;

/**
 * Sorts {@link ForeignKey foreign key}s by the number of column {@link Reference reference}s.
 * 
 * <p>
 * Note: this comparator imposes orderings that are inconsistent with equals.
 * 
 * @author Alan Stewart.
 * @since 1.1
 */
public class ForeignKeyComparator implements Comparator<ForeignKey> {

	public int compare(ForeignKey o1, ForeignKey o2) {
		if (o1 == o2) return 0;
		int cmp = new Integer(o1.getReferenceCount()).compareTo(new Integer(o2.getReferenceCount()));
		if (cmp != 0) {
			return cmp;
		}
		return o1.getName().compareTo(o2.getName());
	}
}