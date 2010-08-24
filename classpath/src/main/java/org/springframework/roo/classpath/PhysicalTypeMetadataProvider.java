package org.springframework.roo.classpath;

import org.springframework.roo.metadata.MetadataProvider;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.ClasspathProvidingProjectMetadata;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;

/**
 * Provides a classpath-related finder for {@link PhysicalTypeMetadata} instances.
 * 
 * <p>
 * Advanced implementations may also monitor the {@link ClasspathProvidingProjectMetadata} locations if desired,
 * which will result in {@link PollingFileMonitorService} being created for all types related to the project classpath.
 * 
 * <p>
 * Add-ons can rely on there being only one {@link PhysicalTypeMetadataProvider} active at a time. Initially
 * this will be because there will only be one implementation that uses source code AST parsing, although it
 * is intended that a bytecode-based parser may also be added in the future. If more than one implementation
 * is eventually developed, they will be hidden below a single visible delegating implementation. As such
 * add-ons do not need to consult a list of different implementations.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public interface PhysicalTypeMetadataProvider extends MetadataProvider {

	/**
	 * Attempts to locate the specified {@link JavaType} by searching the physical disk (does not
	 * search for existing {@link PhysicalTypeMetadata}).
	 * 
	 * <p>
	 * This method resolves the issue that a {@link JavaType} is location independent, yet {@link PhysicalTypeIdentifier}
	 * instances are location dependent (ie a {@link PhysicalTypeIdentifier} relates to a given physical file, whereas a
	 * {@link JavaType} simply assumes the type is available from the classpath). This resolution is achieved by
	 * first scanning the {@link PathResolver#getSourcePaths()} locations, and then scanning any locations provided by the
	 * {@link ClasspathProvidingProjectMetadata} (if the {@link ProjectMetadata} implements this extended interface).
	 * 
	 * <p>
	 * Due to the "best effort" basis of classpath resolution, callers should not rely on complex classpath
	 * resolution outcomes. However, callers can rely on robust determination of types defined in {@link Path}s from
	 * {@link PathResolver#getSourcePaths()}, using the {@link Path} order returned by that method.
	 * 
	 * @param javaType the type to locate (required)
	 * @return the string (in {@link PhysicalTypeIdentifier} format) if found, or null if not found
	 */
	String findIdentifier(JavaType javaType);
}
