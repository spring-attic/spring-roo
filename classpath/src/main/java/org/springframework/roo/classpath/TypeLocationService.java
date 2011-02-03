package org.springframework.roo.classpath;

import java.util.List;
import java.util.Set;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;

/**
 * Locates types.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public interface TypeLocationService {
		
	String getPhysicalLocationCanonicalPath(JavaType javaType, Path path);

	String getPhysicalLocationCanonicalPath(String physicalTypeIdentifier);

	/**
	 * Obtains the requested {@link JavaType}, assuming it is a class or interface that exists at this time and can be parsed.
	 * If these assumption are not met, an exception will be thrown.
	 * 
	 * @param requiredClassOrInterface that should be parsed (required)
	 * @return the details (never returns null)
	 */
	ClassOrInterfaceTypeDetails getClassOrInterface(JavaType requiredClassOrInterface);

	/**
	 * Processes types with the specified list of annotations and uses the supplied 
	 * {@link LocatedTypeCallback callback} implementation to process the located types.
	 * 
	 * @param annotationsToDetect the list of annotations to detect on a type.
	 * @param callback the {@link LocatedTypeCallback} to handle the processing of the located type
	 */
	void processTypesWithAnnotation(List<JavaType> annotationsToDetect, LocatedTypeCallback callback);

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
	Set<ClassOrInterfaceTypeDetails> findClassesOrInterfaceDetailsWithAnnotation(JavaType... annotationsToDetect);
}
