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
 * <li>The Spring MVC @Controller annotation will be declared on the controller type if not exists</li>
 * <li>Setting this annotation will also generate JSP view pages corresponding to the functionalities included</li>
 * <li>Existing JSP pages will be overwritten unless automaticallyMaintainView is disabled</li>
 * <li>The {@link RooWebScaffold#entity()} property defines the {@link RooEntity} which is exposed through this
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
	 * Defines the handling of JSP pages which are using this controller. If this flag is set to true 
	 * ROO will maintain the view artefacts. Existing JSP pages will be overwritten unless 
	 * automaticallyMaintainView is disabled
	 * 
	 * @return indicator if ROO should maintain view artifacts
	 */
	boolean automaticallyMaintainView() default true;
	
	
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
	
//	/**
//	 * Creates a list() method which exposes all entities this controller is responsible for. If enabled this 
//	 * list() method will return a {@link List} of entities.
//	 * 
//	 * @return indicates if the list() method should be provided (defaults to "true"; optional)
//	 */
//	boolean list() default true;
//	
//	/**
//	 * Creates a show() method which exposes the entity with a given id of the type this controller is responsible for. 
//	 * If enabled this show() method will return a single entity or null if an entity with this id cannot be found.
//	 * 
//	 * @return indicates if the show() method should be provided (defaults to "true"; optional)
//	 */
//	boolean show() default true;
	
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
}
