package org.springframework.roo.classpath;

import java.util.List;
import java.util.Set;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.ContextualPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.maven.Pom;

/**
 * Locates types.
 *
 * @author Alan Stewart
 * @author James Tyrrell
 * @since 1.1
 */
public interface TypeLocationService {

	/**
	 * Attempts to find the {@link ClassOrInterfaceTypeDetails} for  the requested {@link JavaType}, assuming
	 * it is a class or interface that exists at this time and can be parsed.
	 * If these assumption are not met, an exception will be thrown.
	 *
	 * @param requiredClassOrInterface that should be parsed (required)
	 * @return the ClassOrInterfaceTypeDetails (never returns null)
	 */
	ClassOrInterfaceTypeDetails getTypeDetails(JavaType requiredClassOrInterface);

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
	String getPhysicalTypeIdentifier(JavaType javaType);

	/**
	 * Resolves the physical type identifier for the provided canonical path. If the path doesn't correspond with a type
	 * or the file doesn't exist null is returned.
	 *
	 * @param fileIdentifier the path to the physical type (required)
	 * @return the physical type identifier if found, otherwise null if not found
	 */
	String getPhysicalTypeIdentifier(String fileIdentifier);

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
	ClassOrInterfaceTypeDetails getTypeDetails(String physicalTypeIdentifier);

	/**
	 * Resolves the canonical file path to for the provided {@link JavaType} and {@link Path}.
	 *
	 * @param javaType the type's {@link JavaType} (required)
	 * @param path the type type's path
	 * @return the resolved path
	 */
	String getPhysicalTypeCanonicalPath(JavaType javaType, ContextualPath path);

	/**
	 * Indicates whether the passed in type has changed since last invocation by the requesting class.
	 *
	 * @param requestingClass the class requesting the changed types
	 * @param javaType the type to lookup to see if a change has occurred
	 * @return a collection of MIDs which represent changed types
	 */
	boolean hasTypeChanged(String requestingClass, JavaType javaType);

	/**
	 *
	 * @param type
	 * @param path
	 * @return
	 */
	String getPhysicalTypeIdentifier(JavaType type, ContextualPath path);

	/**
	 *
	 * @param modulePath
	 * @return
	 */
	Set<String> getTypesForModule(String modulePath);

	/**
	 *
	 * @param module
	 * @return
	 */
	String getTopLevelPackageForModule(Pom module);

	/**
	 *
	 * @param module
	 * @return
	 */
	List<String> getPotentialTopLevelPackagesForModule(Pom module);

	/**
	 *
	 * @param javaType
	 * @return
	 */
	ContextualPath getTypePath(JavaType javaType);
}
