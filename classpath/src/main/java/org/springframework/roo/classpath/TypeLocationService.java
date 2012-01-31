package org.springframework.roo.classpath;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;
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
     * Returns a set of {@link ClassOrInterfaceTypeDetails}s that possess the
     * specified annotations (specified as a vararg).
     * 
     * @param annotationsToDetect the annotations (as a vararg) to detect on a
     *            type.
     * @return a set of ClassOrInterfaceTypeDetails that have the specified
     *         annotations.
     */
    Set<ClassOrInterfaceTypeDetails> findClassesOrInterfaceDetailsWithAnnotation(
            JavaType... annotationsToDetect);

    /**
     * Returns a set of {@link ClassOrInterfaceTypeDetails}s that possess the
     * specified tag.
     * 
     * @param tag the tag to detect on a type.
     * @return a set of ClassOrInterfaceTypeDetails that have the specified tag.
     */
    Set<ClassOrInterfaceTypeDetails> findClassesOrInterfaceDetailsWithTag(
            Object tag);

    /**
     * Returns a set of {@link JavaType}s that possess the specified annotations
     * (specified as a vararg).
     * 
     * @param annotationsToDetect the annotations (as a vararg) to detect on a
     *            type.
     * @return a set of types that have the specified annotations.
     */
    Set<JavaType> findTypesWithAnnotation(JavaType... annotationsToDetect);

    /**
     * Returns a set of {@link JavaType}s that possess the specified list of
     * annotations.
     * 
     * @param annotationsToDetect the list of annotations to detect on a type.
     * @return a set of types that have the specified annotations.
     */
    Set<JavaType> findTypesWithAnnotation(List<JavaType> annotationsToDetect);

    /**
     * Returns the canonical path that the given {@link JavaType} would have
     * within the given {@link LogicalPath}; this type need not exist.
     * <p>
     * Equivalent to constructing the physical type id from the given arguments
     * and calling {@link #getPhysicalTypeCanonicalPath(String)}.
     * 
     * @param javaType the type's {@link JavaType} (required)
     * @param path the type's logical path
     * @return the canonical path (never blank, but might not exist)
     */
    String getPhysicalTypeCanonicalPath(JavaType javaType, LogicalPath path);

    /**
     * Returns the canonical path that a type with the given physical type id
     * would have; this type need not exist.
     * 
     * @param physicalTypeId the physical type's metadata id (required)
     * @return the canonical path (never blank, but might not exist)
     */
    String getPhysicalTypeCanonicalPath(String physicalTypeId);

    /**
     * Looks for the given {@link JavaType} within the user project, and if
     * found, returns the id for its {@link PhysicalTypeMetadata}. Use this
     * method if you know that the {@link JavaType} exists but don't know its
     * {@link LogicalPath}.
     * <p>
     * This method resolves the issue that a {@link JavaType} is location
     * independent, yet {@link PhysicalTypeIdentifier} instances are location
     * dependent (i.e. a {@link PhysicalTypeIdentifier} relates to a given
     * physical file, whereas a {@link JavaType} simply represents a type on the
     * classpath).
     * 
     * @param javaType the type to locate (required)
     * @return the string (in {@link PhysicalTypeIdentifier} format) if found,
     *         or <code>null</code> if not found
     */
    String getPhysicalTypeIdentifier(JavaType javaType);

    /**
     * Returns the physical type identifier for the Java source file with the
     * given canonical path.
     * 
     * @param fileIdentifier the path to the physical type (required)
     * @return the physical type identifier if the given path matches an
     *         existing Java source file, otherwise <code>null</code>
     */
    String getPhysicalTypeIdentifier(String fileIdentifier);

    /**
     * @param module
     * @return
     */
    List<String> getPotentialTopLevelPackagesForModule(Pom module);

    /**
     * @param module
     * @return
     */
    String getTopLevelPackageForModule(Pom module);

    /**
     * Returns the details of the given Java type from within the user project.
     * 
     * @param javaType the type to look for (required)
     * @return <code>null</code> if the type doesn't exist in the project
     */
    ClassOrInterfaceTypeDetails getTypeDetails(JavaType javaType);

    /**
     * Resolves the {@link ClassOrInterfaceTypeDetails} to for the provided
     * physical type identifier. If the physical type identifier doesn't
     * represent a valid type an exception is thrown. This method will return
     * null if the {@link ClassOrInterfaceTypeDetails} can't be found.
     * 
     * @param physicalTypeId the physical type metadata id (can be blank)
     * @return the resolved {@link ClassOrInterfaceTypeDetails}, or
     *         <code>null</code> if the details can't be found (e.g. the given
     *         ID is blank)
     */
    ClassOrInterfaceTypeDetails getTypeDetails(String physicalTypeId);

    /**
     * Returns the {@link LogicalPath} containing the given {@link JavaType}.
     * 
     * @param javaType the {@link JavaType} for which to return the
     *            {@link LogicalPath}
     * @return <code>null</code> if that type doesn't exist in the project
     */
    LogicalPath getTypePath(JavaType javaType);

    /**
     * Returns the Java types that belong to the given module.
     * 
     * @param module
     * @return a non-<code>null</code> collection
     * @since 1.2.1
     */
    Collection<JavaType> getTypesForModule(Pom module);

    /**
     * Returns the Java types that belong to the given module.
     * 
     * @param modulePath
     * @return a non-<code>null</code> collection of fully-qualified type names
     * @deprecated use {@link #getTypesForModule(Pom)} instead; more strongly
     *             typed and also ignores any types found in pom-packaged
     *             modules
     */
    @Deprecated
    Collection<String> getTypesForModule(String modulePath);

    /**
     * Indicates whether the passed in type has changed since last invocation by
     * the requesting class.
     * 
     * @param requestingClass the class requesting the changed types
     * @param javaType the type to lookup to see if a change has occurred
     * @return a collection of MIDs which represent changed types
     */
    boolean hasTypeChanged(String requestingClass, JavaType javaType);

    /**
     * Indicates whether the given type exists anywhere in the user project
     * 
     * @param javaType the type to check for (can be <code>null</code>)
     * @return <code>false</code> if a <code>null</code> type is given
     */
    boolean isInProject(JavaType javaType);

    /**
     * Processes types with the specified list of annotations and uses the
     * supplied {@link LocatedTypeCallback callback} implementation to process
     * the located types.
     * 
     * @param annotationsToDetect the list of annotations to detect on a type.
     * @param callback the {@link LocatedTypeCallback} to handle the processing
     *            of the located type
     */
    void processTypesWithAnnotation(List<JavaType> annotationsToDetect,
            LocatedTypeCallback callback);
}
