package org.springframework.roo.project;

import java.util.List;

public interface ClasspathProvidingProjectMetadata {

	/**
	 * Indicates the classpath locations that should be scanned when resolving types not defined
	 * in a {@link PathResolver} location.
	 * 
	 * <p>
	 * This method need not return a complete classpath, due to the complexity of determining transitive
	 * relationships. The method may also return {@link String}s that reflect non-existent paths (as the
	 * build system has yet to download them). The classpath computation is therefore on a "best effort" basis only.
	 * An implementation must guarantee to send a notification event should there be a change to the calculated classpath.
	 * An implementation must also ensure that no {@link String} presented is nested within any
	 * {@link PathResolver#getSourcePaths()} location.
	 * 
	 * <p>
	 * The {@link String} elements are in canonical file format.
	 * 
	 * @return an unmodifiable representation of classpath locations to be used, excluding locations
	 * that are included by {@link PathResolver#getSourcePaths()} (never null, but may be empty)
	 */
	List<String> getClasspath();
}
