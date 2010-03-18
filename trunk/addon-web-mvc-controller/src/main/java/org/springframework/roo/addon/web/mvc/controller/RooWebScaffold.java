package org.springframework.roo.addon.web.mvc.controller;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.roo.addon.entity.RooEntity;

/**
 * Indicates a type that requires ROO controller support.
 * 
 * <p>
 * This annotation will cause ROO to produce code that would typically appear in MVC controllers. Importantly, such code
 * does NOT depend on any singletons and is intended to safely serialise. In the current release this code will
 * be emitted to an ITD.
 * 
 * <p>
 * The following functionality will be introduced in the ITD:
 * 
 * <ul>
 * <li>The Spring MVC {@link org.springframework.stereotype.Controller} annotation will be declared on the controller type if not exists</li>
 * <li>Setting this annotation will also generate JSP view pages corresponding to the functionalities included</li>
 * <li>The {@link RooWebScaffold#formBackingObject()} property defines the {@link RooEntity} which is exposed through this
 * controller</li>
 * </ul>
 * 
 * <p>
 * There are two cases in which ROO will not emit one or more of the above artifacts:
 * 
 * <ul>
 * <li>The user provides the equivalent methods on the controller object itself</li>
 * <li>A specific {@link RooWebScaffold} annotation value indicates the desired output type should not be emitted (all emit by default)</li>
 * </ul>
 * 
 * @author Stefan Schmidt
 * @since 1.0
 *
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RooWebScaffold {
	
	
	/**
	 * All view-related artifacts for a specific controller are stored in a sub-directory under 
	 * WEB-INF/views/<em>path</em>. The path parameter defines the name of this sub-directory or path. 
	 * This path is also used to define the restful resource in the URL to which the controller is 
	 * mapped. 
	 * 
	 * @return The view path.
	 */
	String path();
	
	/**
	 * Every controller is responsible for a single form backing object. The form backing object defined 
	 * here class will be exposed in a RESTful way.
	 */
	Class<?> formBackingObject();
	
	/**
	 * Creates a delete() method which deletes an entity for a given id. 
	 * 
	 * @return indicates if the delete() method should be provided (defaults to "true"; optional)
	 */
	boolean delete() default true;
	
	/**
	 * Creates a create() method which allows the creation of a new entity. 
	 * 
	 * @return indicates if the create() method should be provided (defaults to "true"; optional)
	 */
	boolean create() default true;
	
	/**
	 * Creates an update() method which allows alteration of an existing entity.
	 * 
	 * @return indicates if the update() method should be provided (defaults to "true"; optional)
	 */
	boolean update() default true;
	
	
	/**
	 * Will scan the formBackingObjects for installed finder methods and expose them when configured. 
	 * 
	 * @return indicates if the finders methods should be provided (defaults to "true"; optional)
	 */
	boolean exposeFinders() default true;
	
	/**
	 * Registers an @InitBinder method to provide converters for String presentation of objects. Useful
	 * for adjusting the label of select boxes in the UI
	 * 
	 * @return indicates if the registerConverters method is provided (defaults to 'true'; optional)
	 */
	boolean registerConverters() default true;
}
