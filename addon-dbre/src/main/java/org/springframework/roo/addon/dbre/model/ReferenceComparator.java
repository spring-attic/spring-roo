package org.springframework.roo.addon.dbre.model;

import java.util.Comparator;

/**
 * Sorts {@link Reference reference}s by the sequence number.
 * 
 * @author Alan Stewart.
 * @since 1.1
 */
public class ReferenceComparator implements Comparator<Reference> {

	public int compare(Reference o1, Reference o2) {
		return new Integer(o1.getSequenceNumber()).compareTo(new Integer(o2.getSequenceNumber()));
	}
}
