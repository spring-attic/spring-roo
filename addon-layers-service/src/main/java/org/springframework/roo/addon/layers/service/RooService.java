package org.springframework.roo.addon.layers.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation indicating a service interface in a user project
 * 
 * @author Stefan Schmidt
 * @author Andrew Swan
 * @since 1.2.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface RooService {

    /**
     * The default prefix of the "count all" method
     */
    String COUNT_ALL_METHOD = "countAll";

    /**
     * The default name of the "delete" method
     */
    String DELETE_METHOD = "delete";

    /**
     * The name of this annotation's "domain types" attribute
     */
    String DOMAIN_TYPES_ATTRIBUTE = "domainTypes";

    /**
     * The default prefix of the "find all" method
     */
    String FIND_ALL_METHOD = "findAll";

    /**
     * The default prefix of the "find entries" method
     */
    String FIND_ENTRIES_METHOD = "find";

    /**
     * The default prefix of the "find" method
     */
    String FIND_METHOD = "find";

    /**
     * The default name of the "save" method
     */
    String SAVE_METHOD = "save";

    /**
     * The default name of the "update" method
     */
    String UPDATE_METHOD = "update";

    /**
     * Returns the prefix of the "count all" method
     * 
     * @return a blank string if the annotated type doesn't support this method
     */
    String countAllMethod() default COUNT_ALL_METHOD;

    /**
     * Returns the name of the "delete" method
     * 
     * @return a blank string if the annotated type doesn't support this method
     */
    String deleteMethod() default DELETE_METHOD;

    /**
     * Returns the domain type(s) managed by this service
     * 
     * @return a non-<code>null</code> array
     */
    Class<?>[] domainTypes();

    /**
     * Returns the name of the "find all" method
     * 
     * @return a blank string if the annotated type doesn't support this method
     */
    String findAllMethod() default FIND_ALL_METHOD;

    /**
     * Returns the prefix of the "findFooEntries" method
     * 
     * @return a blank string if the annotated type doesn't support this method
     */
    String findEntriesMethod() default FIND_ENTRIES_METHOD;

    /**
     * Returns the name of the "find" method
     * 
     * @return a blank string if the annotated type doesn't support this method
     */
    String findMethod() default FIND_METHOD;

    /**
     * Returns the name of the "save" method
     * 
     * @return a blank string if the annotated type doesn't support this method
     */
    String saveMethod() default SAVE_METHOD;

    /**
     * Indicates whether the annotated service should be transactional
     * 
     * @return see above
     */
    boolean transactional() default true;

    /**
     * Returns the name of the "update" method
     * 
     * @return a blank string if the annotated type doesn't support this method
     */
    String updateMethod() default UPDATE_METHOD;

    /**
     * Annotates services methods with @PreAuthorize(isAuthenticated())
     * 
     * @return a blank string if the annotated type doesn't support this method
     */
    boolean requireAuthentication() default false;

    /**
     * Annotates services methods with @PreAuthorize(hasPermission())
     * 
     * @return a blank string if the annotated type doesn't support this method
     */
    boolean usePermissionEvaluator() default false;

    /**
     * Annotates update service methods with @PreAuthorize(hasRole())
     * 
     * @return a blank string if the annotated type doesn't support this method
     */
    String[] authorizedCreateOrUpdateRoles() default { };

    /**
     * Annotates delete service methods with @PreAuthorize(hasRole())
     * 
     * @return a blank string if the annotated type doesn't support this method
     */
    String[] authorizedDeleteRoles() default { };

    /**
     * Annotates find service methods with @PreAuthorize(hasRole())
     * 
     * @return a blank string if the annotated type doesn't support this method
     */
    String[] authorizedReadRoles() default { };

    /**
     * Indicates whether the annotated service should be instantiated using XML
     * configuration
     * 
     * @return see above
     */
    boolean useXmlConfiguration() default false;

}
