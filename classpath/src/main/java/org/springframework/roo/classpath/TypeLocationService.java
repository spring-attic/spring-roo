package org.springframework.roo.classpath;

import java.util.List;
import java.util.Set;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.model.JavaType;

/**
 * Locates types that have the specified annotations.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public interface TypeLocationService {

	/**
	 * Locates types with the specified list of annotations and uses the supplied 
	 * {@link LocatedTypeCallback callback} implementation to process the located types.
	 * 
	 * @param annotationsToDetect the list of annotations to detect on a type.
	 * @param callback the {@link LocatedTypeCallback} to handle the processing of the located type
	 */
	void findTypesWithAnnotation(List<JavaType> annotationsToDetect, LocatedTypeCallback callback);

	/**
	 * Returns a set of {@link JavaType}s that possess the specified list of annotations.
	 * 
	 * @param annotationsToDetect the list of annotations to detect on a type.
	 * @return a set of types that have the specified annotations.
	 */
	Set<JavaType> findTypesWithAnnotation(List<JavaType> annotationsToDetect);

	/**
	 * Returns a set of {@link JavaType}s that possess the specified annotations (specified as a vararg).
	 * 
	 * @param annotationsToDetect the annotations (as a vararg) to detect on a type.
	 * @return a set of types that have the specified annotations.
	 */
	Set<JavaType> findTypesWithAnnotation(JavaType... annotationsToDetect);
	
	/**
	 * Returns a set of {@link ClassOrInterfaceTypeDetails}s that possess the specified annotations (specified as a vararg).
	 * 
	 * @param annotationsToDetect the annotations (as a vararg) to detect on a type.
	 * @return a set of ClassOrInterfaceTypeDetails that have the specified annotations.
	 */
	Set<ClassOrInterfaceTypeDetails> findClassesOrInterfacesWithAnnotation(JavaType... annotationsToDetect);
}
