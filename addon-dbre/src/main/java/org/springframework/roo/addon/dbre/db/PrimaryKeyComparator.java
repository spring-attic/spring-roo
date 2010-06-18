package org.springframework.roo.addon.dbre.db;

import java.util.Comparator;

/**
 * {@link Comparator} for {@link PrimaryKey}.
 * 
 * <p>
 * Compares based on the key sequence of the primary key.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class PrimaryKeyComparator implements Comparator<PrimaryKey> {

	public int compare(PrimaryKey o1, PrimaryKey o2) {
		return o1.getKeySeq().compareTo(o2.getKeySeq());
	}
}
