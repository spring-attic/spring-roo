package org.springframework.roo.obr;

/**
 * Performs an add-on search using a given {@link AddOnFinder}, displaying the results to the user.
 *
 * @author Ben Alex
 * @since 1.1
 */
public interface AddOnSearchManager {

	/**
	 * Performs the search, displaying matching add-ons to the user along with installation instructions.
	 * 
	 * @param criteria to search (the exact meaning varies as per {@link AddOnFinder}; required)
	 * @param finder the finder that will perform the search (required)
	 * @return the number of matches
	 */
	int completeAddOnSearch(String criteria, AddOnFinder finder);
}