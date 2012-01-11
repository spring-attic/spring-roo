package org.springframework.roo.addon.web.mvc.controller.scaffold;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates a type that requires ROO controller support.
 * <p>
 * This annotation will cause ROO to produce code that would typically appear in
 * MVC controllers. Importantly, such code does NOT depend on any singletons and
 * is intended to safely serialise. In the current release this code will be
 * emitted to an ITD.
 * <p>
 * The following functionality will be introduced in the ITD:
 * <ul>
 * <li>The Spring MVC org.springframework.stereotype.Controller annotation will
 * be declared on the controller type if not exists</li>
 * <li>Setting this annotation will also generate JSP view pages corresponding
 * to the functionalities included</li>
 * <li>The {@link RooWebScaffold#formBackingObject()} property defines the
 * persistent type which is exposed through this controller</li>
 * </ul>
 * <p>
 * There are two cases in which ROO will not emit one or more of the above
 * artifacts:
 * <ul>
 * <li>The user provides the equivalent methods on the controller object itself</li>
 * <li>A specific {@link RooWebScaffold} annotation value indicates the desired
 * output type should not be emitted (all emit by default)</li>
 * </ul>
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RooWebScaffold {

    /**
     * Creates a create() method which allows the creation of a new entity.
     * 
     * @return indicates if the create() method should be provided (defaults to
     *         "true"; optional)
     */
    boolean create() default true;

    /**
     * Creates a delete() method which deletes an entity for a given id.
     * 
     * @return indicates if the delete() method should be provided (defaults to
     *         "true"; optional)
     */
    boolean delete() default true;

    /**
     * This flag is not used any more as of Roo 1.2.0. Please annotate
     * controller types with
     * {@link org.springframework.roo.addon.web.mvc.controller.finder.RooWebFinder}
     * instead. (Was: Will scan the formBackingObjects for installed finder
     * methods and expose them when configured.)
     * 
     * @return indicates if the finders methods should be provided (defaults to
     *         "true"; optional)
     */
    @Deprecated
    boolean exposeFinders() default true;

    /**
     * This flag is not used any more as of Roo 1.2.0. Please annotate
     * controller types with
     * {@link org.springframework.roo.addon.web.mvc.controller.json.RooWebJson}
     * instead. (Was: Will scan the formBackingObjects for
     * org.springframework.roo.addon.json.RooJson annotation and expose json
     * when configured.)
     * 
     * @return indicates if the json methods should be provided (defaults to
     *         "true"; optional)
     */
    @Deprecated
    boolean exposeJson() default true;

    /**
     * Every controller is responsible for a single form backing object. The
     * form backing object defined here class will be exposed in a RESTful way.
     */
    Class<?> formBackingObject();

    /**
     * All view-related artifacts for a specific controller are stored in a
     * sub-directory under WEB-INF/views/<em>path</em>. The path parameter
     * defines the name of this sub-directory or path. This path is also used to
     * define the restful resource in the URL to which the controller is mapped.
     * 
     * @return The view path.
     */
    String path();

    /**
     * Indicate if Roo should create data population methods used for model
     * attributes required for the Spring MVC forms. If this flag is set to
     * false the developer is expected to manage the population of the model
     * attributes by himself.
     * 
     * @return indicates if the populateXXX() methods should be provided
     *         (defaults to "true"; optional)
     */
    boolean populateMethods() default true;

    /**
     * Creates an update() method which allows alteration of an existing entity.
     * 
     * @return indicates if the update() method should be provided (defaults to
     *         "true"; optional)
     */
    boolean update() default true;
}
