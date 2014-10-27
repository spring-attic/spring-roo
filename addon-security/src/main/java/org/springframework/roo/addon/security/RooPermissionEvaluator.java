package org.springframework.roo.addon.security;

public @interface RooPermissionEvaluator {
	/**
     * The default prefix of the "count all" permission
     */
    String COUNT_ALL_PERMISSION = null;

    /**
     * The default name of the "delete" permission
     */
    String DELETE_PERMISSION = "delete";

    /**
     * The default prefix of the "find all" permission
     */
    String FIND_ALL_PERMISSION = null;

    /**
     * The default prefix of the "find entries" permission
     */
    String FIND_ENTRIES_PERMISSION = null;

    /**
     * The default prefix of the "find" permission
     */
    String FIND_PERMISSION = "find";

    /**
     * The default name of the "save" permission
     */
    String SAVE_PERMISSION = "save";

    /**
     * The default name of the "update" permission
     */
    String UPDATE_PERMISSION = "update";
    
    /**
     * Indicates the default return value for the permission evaluator
     * 
     * @return see above
     */
    boolean defaultReturnValue() default false;
}
