package org.springframework.roo.project;

import org.springframework.roo.support.util.Assert;

/**
 * Immutable representation of common file path conventions used in Maven projects.
 * 
 * <p>
 * {@link PathResolver} instances provide the ability to resolve these paths
 * to and from physical locations.
 * 
 * <p>
 * A name cannot include the question mark character.
 * 
 * <p>
 * Presented as a class instead of an enumeration to enable extension.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class Path implements Comparable<Path> {
	public static final Path SRC_MAIN_JAVA = new Path("SRC_MAIN_JAVA");
	public static final Path SRC_MAIN_RESOURCES = new Path("SRC_MAIN_RESOURCES");
	public static final Path SRC_TEST_JAVA = new Path("SRC_TEST_JAVA");
	public static final Path SRC_TEST_RESOURCES = new Path("SRC_TEST_RESOURCES");
	public static final Path SRC_MAIN_WEBAPP = new Path("SRC_MAIN_WEBAPP");
	public static final Path ROOT = new Path("ROOT");
	public static final Path SPRING_CONFIG_ROOT = new Path("SPRING_CONFIG_ROOT");
	
	private String name;
	/**
	 * Creates a name with the specified string.
	 * 
	 * <p>
	 * A name cannot contain a question mark character, due to it being a reserved character for metadata
	 * identification string tokenization.
	 * 
	 * @param name the name (required and cannot contain a "?" character)
	 */
	public Path(String name) {
		Assert.hasText(name, "Name required");
		Assert.isTrue(!name.contains("?"), "Name cannot contain a question mark character ('" + name + "')");
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public int hashCode() {
		return this.name.hashCode();
	}

	public boolean equals(Object obj) {
		return obj != null && obj instanceof Path && this.compareTo((Path) obj) == 0;
	}

	public int compareTo(Path o) {
		if (o == null) {
			throw new NullPointerException();
		}
		return name.compareTo(o.name);
	}
	
	public final String toString() {
		return name;
	}
}
