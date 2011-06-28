package org.springframework.roo.project.layers;

import java.util.Map;

import org.springframework.roo.model.JavaType;

/**
 * Provides upper-layer code (such as MVC, GWT, and tests) with the
 * {@link MemberTypeAdditions} they need to make to their source code in order
 * to invoke layer-related operations such as <code>persist</code> and
 * <code>find</code> (or any other operations provided by third-party layer
 * providers).
 * 
 * @author Stefan Schmidt
 * @author Andrew Swan
 * @since 1.2
 */
public interface LayerService {
	
	/**
	 * Returns the additions that the caller should make in order to invoke the
	 * given method. This is simply a convenience version of the
	 * {@link #getAdditions(Enum...)} method that returns a {@link Map}.
	 * 
	 * @param <M> the type of method to be invoked
	 * @param metadataId the caller's MID
	 * @param targetEntity the entity to which the method relates
	 * @param layerPosition bigger means higher
	 * @param method the method to be invoked
	 * @return <code>null</code> if that method is not supported
	 * @return
	 */
	<M extends Enum<M>> MemberTypeAdditions getAdditions(String metadataId, JavaType targetEntity, int layerPosition, M method);

	/**
	 * Returns the additions that the caller should make in order to invoke the
	 * given methods.
	 * 
	 * @param <M> the type of method to be invoked
	 * @param metadataId the caller's MID
	 * @param targetEntity the entity to which the method relates
	 * @param layerPosition bigger means higher
	 * @param methods the methods to be invoked
	 * @return a non-<code>null</code> map
	 */
	<M extends Enum<M>> Map<M, MemberTypeAdditions> getAdditions(String metadataId, JavaType targetEntity, int layerPosition, M... methods);

	/**
	 * Returns the additions that the caller should make in order to invoke all
	 * methods of the given type.
	 * 
	 * @param <M> the type of method to be invoked
	 * @param metadataId the caller's MID
	 * @param targetEntity the entity to which the method relates
	 * @param layerPosition bigger means higher
	 * @param methodType the type of method to be invoked
	 * @return a non-<code>null</code> map
	 */
	<M extends Enum<M>> Map<M, MemberTypeAdditions> getAllAdditions(String metadataId, JavaType targetEntity, int layerPosition, Class<M> methodType);
}
