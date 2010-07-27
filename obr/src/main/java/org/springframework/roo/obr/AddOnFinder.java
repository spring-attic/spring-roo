package org.springframework.roo.obr;

import java.util.SortedMap;

/**
 * Provides a mechanism to locate add-ons which meet a specific criteria.
 *
 * @author Ben Alex
 * @since 1.1
 */
public interface AddOnFinder {

	/**
	 * Performs a search with the given criteria.
	 * 
	 * <p>
	 * The implementation must avoid performing a search if the OBR information is not available
	 * according to {@link ObrResourceFinder}.
	 * 
	 * @param criteria to search (required; the exact meaning of criteria depends on the implementation)
	 * @return a map where keys are the bundle symbolic names and the values are the presentation names of the bundles (a null result means no search was performed)
	 */
	SortedMap<String, String> findAddOnsOffering(String criteria);
	
	/**
	 * Indicates the target of this implementation. Generally indicates what the criteria targets (eg command, JDBC driver etc).
	 * 
	 * @return the target in singular form (never null or in plural form)
	 */
	String getFinderTargetSingular();

	/**
	 * Indicates the target of this implementation. Generally indicates what the criteria targets (eg commands, JDBC drivers etc).
	 * 
	 * @return the target in plural form (never null or in singular form, although may be in singular form if the plural is the same)
	 */
	String getFinderTargetPlural();
}