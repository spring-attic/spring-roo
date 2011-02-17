package org.springframework.roo.model;

import org.springframework.roo.support.util.Assert;

/**
 * Immutable representation of a Java package.
 * 
 * <p>
 * This class is used whenever a formal reference to a Java package is required.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public final class JavaPackage implements Comparable<JavaPackage> {
	private String fullyQualifiedPackageName;

	/**
	 * Construct a JavaPackage.
	 * 
	 * <p>
	 * The fully qualified package name will be enforced as follows:
	 * 
	 * <ul>
	 * <li>The rules listed in {@link JavaSymbolName#assertJavaNameLegal(String)}
	 * </ul>
	 * 
	 * @param fullyQualifiedPackageName the name (as per the above rules; mandatory)
	 */
	public JavaPackage(String fullyQualifiedPackageName) {
		Assert.notNull(fullyQualifiedPackageName, "Fully qualified package name required");
		JavaSymbolName.assertJavaNameLegal(fullyQualifiedPackageName);
		this.fullyQualifiedPackageName = fullyQualifiedPackageName;
	}
	
	/**
	 * @return the fully qualified package name (complies with the rules specified in the constructor)
	 */
	public String getFullyQualifiedPackageName() {
		return fullyQualifiedPackageName;
	}

	public final int hashCode() {
		return this.fullyQualifiedPackageName.hashCode();
	}

	public final boolean equals(Object obj) {
		return obj != null && obj instanceof JavaPackage && this.compareTo((JavaPackage) obj) == 0;
	}

	public final int compareTo(JavaPackage o) {
		if (o == null) return -1;
		return this.fullyQualifiedPackageName.compareTo(o.fullyQualifiedPackageName);
	}
	
	public final String toString() {
		return fullyQualifiedPackageName;
	}
}
