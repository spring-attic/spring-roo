package org.springframework.roo.project.layers;

/**
 * Persistence-related methods that layers might want to provide
 *
 * @author Andrew Swan
 */
public enum PersistenceMethod {

	FIND_ALL,
	
	FIND_BY_ID,
	
	FIND_BY_RANGE;
	
	// TODO add others: persist, remove, etc.
}
