package org.springframework.roo.classpath.customdata;

import org.springframework.roo.model.CustomData;

/**
 * {@link CustomData} tag definitions for persistence-related functionality.
 * 
 * @author Stefan Schmidt
 * @since 1.1.3
 */
public enum CustomDataPersistenceTags {
	
	/**
	 * Persistence type definitions
	 */
	IDENTIFIER_TYPE,
	PERSISTENT_TYPE,
	
	NO_ARG_CONSTRUCTOR,
	
	/**
	 * Persistence field definitions
	 */
	IDENTIFIER_FIELD,
	ROO_IDENTIFIER_FIELD,
	VERSION_FIELD,
	TRANSIENT_FIELD,
	EMBEDDED_FIELD,
	EMBEDDED_ID_FIELD,
	ENUMERATED_FIELD,
	MANY_TO_MANY_FIELD,
	ONE_TO_MANY_FIELD,
	MANY_TO_ONE_FIELD,
	ONE_TO_ONE_FIELD,
	LOB_FIELD,
	COLUMN_FIELD, 
	
	/**
	 * Persistence method definitions
	 */
	// Identifier accessor method (method must have no parameters, and return ID data type)
	IDENTIFIER_ACCESSOR_METHOD,
	
	// Identifier mutator method (method must have one parameter, and return void data type)
	IDENTIFIER_MUTATOR_METHOD,

	// Version accessor method (method must have no parameters, and return version data type)
	VERSION_ACCESSOR_METHOD,
	
	// Version mutator method (method must have one parameter, and return void data type)
	VERSION_MUTATOR_METHOD,
	
	// Persist method (TODO)
	PERSIST_METHOD,
	
	// Merge method (TODO)
	MERGE_METHOD,
	
	// Remove method (TODO)
	REMOVE_METHOD,
	
	// Flush method (TODO)
	FLUSH_METHOD,
	
	// Clear method (TODO)
	CLEAR_METHOD,
	
	// Count all method (TODO)
	COUNT_ALL_METHOD,
	
	// Find all method (TODO)
	FIND_ALL_METHOD,
	
	// Find method (TODO)
	FIND_METHOD,
	
	// Find entries method (TODO)
	FIND_ENTRIES_METHOD,
	
	// Dynamic finder method names; CustomData value expected to be a java.util.List<String> of finder names
	DYNAMIC_FINDER_NAMES;
}
