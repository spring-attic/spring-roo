package org.springframework.roo.addon.web.mvc.controller.json;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates a type that requires ROO controller support.
 * <p>
 * This annotation will cause ROO to produce code that would typically appear in
 * MVC JSON-enabled controllers.
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RooWebJson {

    /**
     * The default prefix of the "create" method
     */
    String CREATE_FROM_JSON = "createFromJson";

    /**
     * The default prefix of the "create from array" method
     */
    String CREATE_FROM_JSON_ARRAY = "createFromJsonArray";

    /**
     * The default prefix of the "delete" method
     */
    String DELETE_FROM_JSON_ARRAY = "deleteFromJson";

    /**
     * Expose finders by default
     */
    boolean EXPOSE_FINDERS = true;

    /**
     * The default prefix of the "find all" method
     */
    String LIST_JSON = "listJson";

    /**
     * The default prefix of the "show" method
     */
    String SHOW_JSON = "showJson";

    /**
     * The default prefix of the "update" method
     */
    String UPDATE_FROM_JSON = "updateFromJson";

    /**
     * The default prefix of the "update from array" method
     */
    String UPDATE_FROM_JSON_ARRAY = "updateFromJsonArray";

    /**
     * Creates a createFromJsonArray() method which finds all objects. Set
     * methodName to "" to prevent its generation.
     * 
     * @return indicates the method name for the createFromJsonArray() method
     *         (optional)
     */
    String createFromJsonArrayMethod() default CREATE_FROM_JSON_ARRAY;

    /**
     * Creates a createFromJson() method which finds all objects. Set methodName
     * to "" to prevent its generation.
     * 
     * @return indicates the method name for the createFromJson() method
     *         (optional)
     */
    String createFromJsonMethod() default CREATE_FROM_JSON;

    /**
     * Creates a deleteFromJson() method which finds all objects. Set methodName
     * to "" to prevent its generation.
     * 
     * @return indicates the method name for the deleteFromJson() method
     *         (optional)
     */
    String deleteFromJsonMethod() default DELETE_FROM_JSON_ARRAY;

    /**
     * Will scan the formBackingObjects for installed finder methods and expose
     * them when configured.
     * 
     * @return indicates if the finders methods should be provided (defaults to
     *         "true"; optional)
     */
    boolean exposeFinders() default EXPOSE_FINDERS;

    /**
     * Every controller is responsible for a single JSON-enabled object. The
     * backing object defined here class will be exposed in a RESTful way.
     */
    Class<?> jsonObject();

    /**
     * Creates a listJson() method which finds all objects. Set methodName to ""
     * to prevent its generation.
     * 
     * @return indicates the method name for the listJson() method (optional)
     */
    String listJsonMethod() default LIST_JSON;

    /**
     * Creates a showJson() method which finds an object for a given id. Set
     * methodName to "" to prevent its generation.
     * 
     * @return indicates the method name for the showJson() method (optional)
     */
    String showJsonMethod() default SHOW_JSON;

    /**
     * Creates a updateFromJsonArray() method which finds all objects. Set
     * methodName to "" to prevent its generation.
     * 
     * @return indicates the method name for the updateFromJsonArray() method
     *         (optional)
     */
    String updateFromJsonArrayMethod() default UPDATE_FROM_JSON_ARRAY;

    /**
     * Creates a updateFromJson() method which finds all objects. Set methodName
     * to "" to prevent its generation.
     * 
     * @return indicates the method name for the updateFromJson() method
     *         (optional)
     */
    String updateFromJsonMethod() default UPDATE_FROM_JSON;
}
