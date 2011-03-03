package org.springframework.roo.classpath.customdata;

import org.springframework.roo.model.CustomData;

/**
 * {@link CustomData} tag definitions for persistence-related functionality.
 * 
 * @author Stefan Schmidt
 * @since 1.1.3
 *
 */
public abstract class CustomDataPersistenceTags {
	
	/**
	 * Persistence field definitions
	 */
	public static final String NO_ARG_CONSTRUCTOR = "NO_ARG_CONSTRUCTOR";
	public static final String IDENTIFIER_FIELD = "IDENTIFIER_FIELD";
	public static final String VERSION_FIELD = "VERSION_FIELD";
	public static final String TRANSIENT_FIELD = "TRANSIENT";
	public static final String EMBEDDED_FIELD = "EMBEDDED";
	public static final String ENUMERATED_FIELD = "ENUMERATED";
	
	/**
	 * Persistence method definitions
	 */
	// Identifier accessor method (method must have no parameters, and return ID data type)
	public static final String IDENTIFIER_ACCESSOR_METHOD = "IDENTIFIER_ACCESSOR_METHOD";
	
	// Version accessor method (method must have no parameters, and return version data type)
	public static final String VERSION_ACCESSOR_METHOD = "VERSION_ACCESSOR_METHOD";
	
	// Persist method (TODO)
	public static final String PERSIST_METHOD = "PERSIST_METHOD";
	
	// Merge method (TODO)
	public static final String MERGE_METHOD = "MERGE_METHOD";
	
	// Remove method (TODO)
	public static final String REMOVE_METHOD = "REMOVE_METHOD";
	
	// Flush method (TODO)
	public static final String FLUSH_METHOD = "FLUSH_METHOD";
	
	// Clear method (TODO)
	public static final String CLEAR_METHOD = "CLEAR_METHOD";
	
	// Count all method (TODO)
	public static final String COUNT_ALL_METHOD = "COUNT_ALL_METHOD";
	
	// Find all method (TODO)
	public static final String FIND_ALL_METHOD = "FIND_ALL_METHOD";
	
	// Find method (TODO)
	public static final String FIND_METHOD = "FIND_METHOD";
	
	// Find entries method (TODO)
	public static final String FIND_ENTRIES_METHOD = "FIND_ENTRIES_METHOD";
	
	// Dynamic finder method names; CustomData value expected to be a java.util.List<String> of finder names
	public static final String DYNAMIC_FINDER_NAMES = "DYNAMIC_FINDER_NAMES";

}
