package org.springframework.roo.classpath;

import org.springframework.roo.project.ClasspathProvidingProjectMetadata;
import org.springframework.roo.project.Path;

/**
 * Extends the notion of a {@link Path} to include resources acquired from a
 * {@link ClasspathProvidingProjectMetadata}. This is important for {@link PhysicalTypeIdentifierNamingUtils}s.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class ClassloaderInclusivePath extends Path {
	private String classpath;
	
	/**
	 * Creates an {@link ClassloaderInclusivePath} instance.
	 * 
	 * @param classpath the root of the classpath location (cannot be null)
	 */
	public ClassloaderInclusivePath(String classpath) {
		super("CLASSPATH_" + classpath);
		this.classpath = classpath;
	}

	/**
	 * @return the root of the classpath location (never null)
	 */
	public String getClasspath() {
		return classpath;
	}
}
