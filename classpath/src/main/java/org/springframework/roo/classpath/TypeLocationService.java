package org.springframework.roo.classpath;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Locates types.
 * 
 * @author Alan Stewart
 * @author James Tyrrell
 * @since 1.1
 */
public interface TypeLocationService {

	/**
	 * Obtains the requested {@link JavaType}, assuming it is a class or interface that exists at this time and can be parsed.
	 * If these assumption are not met, an exception will be thrown.
	 * 
	 * @param requiredClassOrInterface that should be parsed (required)
	 * @return the ClassOrInterfaceTypeDetails (never returns null)
	 */
	ClassOrInterfaceTypeDetails getClassOrInterface(JavaType requiredClassOrInterface);
	
	/**
	 * Attempts to find the requested {@link JavaType}, assuming it is a class or interface.
	 * 
	 * @param requiredClassOrInterface that should be parsed (required)
	 * @return the ClassOrInterfaceTypeDetails if found, otherwise null if not found
	 */
	ClassOrInterfaceTypeDetails findClassOrInterface(JavaType requiredClassOrInterface);

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
	
	/**
	 * Returns a set of {@link ClassOrInterfaceTypeDetails}s that possess the specified tag.
	 * 
	 * @param tag the tag to detect on a type.
	 * @return a set of ClassOrInterfaceTypeDetails that have the specified tag.
	 */
	Set<ClassOrInterfaceTypeDetails> findClassesOrInterfaceDetailsWithTag(Object tag);

	/**
	 * Returns a list of {@link ClassOrInterfaceTypeDetails} that are contained within the specified {@link Path}.
	 *
	 * @param path
	 * @return a list of ClassOrInterfaceTypeDetails contained in path.
	 */
	List<ClassOrInterfaceTypeDetails> getProjectJavaTypes(Path path);

	/**
	 * Attempts to locate the specified {@link JavaType} by searching the physical disk (does not
	 * search for existing {@link PhysicalTypeMetadata}).
	 *
	 * <p>
	 * This method resolves the issue that a {@link JavaType} is location independent, yet {@link PhysicalTypeIdentifier}
	 * instances are location dependent (ie a {@link PhysicalTypeIdentifier} relates to a given physical file, whereas a
	 * {@link JavaType} simply assumes the type is available from the classpath). This resolution is achieved by
	 * first scanning the {@link org.springframework.roo.project.PathResolver#getSourcePaths()} locations, and then scanning any locations provided by the
	 * {@link org.springframework.roo.project.ClasspathProvidingProjectMetadata} (if the {@link org.springframework.roo.project.ProjectMetadata} implements this extended interface).
	 *
	 * <p>
	 * Due to the "best effort" basis of classpath resolution, callers should not rely on complex classpath
	 * resolution outcomes. However, callers can rely on robust determination of types defined in {@link Path}s from
	 * {@link org.springframework.roo.project.PathResolver#getSourcePaths()}, using the {@link Path} order returned by that method.
	 *
	 * @param javaType the type to locate (required)
	 * @return the string (in {@link PhysicalTypeIdentifier} format) if found, or null if not found
	 */
	String findIdentifier(JavaType javaType);

	/**
	 * Resolves the physical type identifier for the provided canonical path. If the path doesn't correspond with a type
	 * or the file doesn't exist null is returned.
	 *
	 * @param fileIdentifier the path to the physical type (required)
	 * @return the physical type identifier if found, otherwise null if not found
	 */
	String findIdentifier(String fileIdentifier);

	/**
	 * Resolves the canonical file path to for the provided physical type identifier. If the physical type identifier doesn't
	 * represent a valid type an exception is thrown.
	 *
	 * @param physicalTypeIdentifier the physical type identifier (required)
	 * @return the resolved path
	 */
	String getPhysicalTypeCanonicalPath(String physicalTypeIdentifier);

	/**
	 * Resolves the {@link ClassOrInterfaceTypeDetails} to for the provided physical type identifier. If the physical
	 * type identifier doesn't represent a valid type an exception is thrown. This method will return null if the
	 * {@link ClassOrInterfaceTypeDetails} can't be found.
	 *
	 * @param physicalTypeIdentifier the physical type identifier (required)
	 * @return the resolved {@link ClassOrInterfaceTypeDetails}
	 */
	ClassOrInterfaceTypeDetails getTypeForIdentifier(String physicalTypeIdentifier);

	/**
	 * Resolves the canonical file path to for the provided {@link JavaType} and {@link Path}.
	 *
	 * @param javaType the type's {@link JavaType} (required)
	 * @param path the type type's path
	 * @return the resolved path
	 */
	String getPhysicalTypeCanonicalPath(JavaType javaType, Path path);


	/**
	 * Returns a collection of MIDs representing types changed since last invocation.
	 *
	 * @param requestingClass the class requesting the changed types
	 * @return a collection of MIDs which represent changed types
	 */
	LinkedHashSet<String> getWhatsDirty(String requestingClass);

}
