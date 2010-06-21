package org.springframework.roo.addon.dbre.db;

import java.util.Comparator;

/**
 * {@link Comparator} for {@link ForeignKey}.
 * 
 * <p>
 * Used to sort foreign keys on the key sequence.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class ForeignKeyComparator implements Comparator<ForeignKey> {

	public int compare(ForeignKey o1, ForeignKey o2) {
		return o1.getKeySeq().compareTo(o2.getKeySeq());
	}
}
