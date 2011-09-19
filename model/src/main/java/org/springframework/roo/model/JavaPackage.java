package org.springframework.roo.model;

import java.util.Arrays;
import java.util.List;

import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Immutable representation of a Java package.
 * 
 * <p>
 * This class is used whenever a formal reference to a Java package is required.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class JavaPackage implements Comparable<JavaPackage> {
	
	// Fields
	private final String fullyQualifiedPackageName;

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

	public int hashCode() {
		return this.fullyQualifiedPackageName.hashCode();
	}

	public boolean equals(Object obj) {
		return obj instanceof JavaPackage && this.compareTo((JavaPackage) obj) == 0;
	}

	public int compareTo(JavaPackage o) {
		if (o == null) return -1;
		return this.fullyQualifiedPackageName.compareTo(o.getFullyQualifiedPackageName());
	}
	
	public String toString() {
		return fullyQualifiedPackageName;
	}

	/**
	 * Returns the elements of this package's fully-qualified name
	 * 
	 * @return a non-empty list
	 */
	public List<String> getElements() {
		return Arrays.asList(StringUtils.delimitedListToStringArray(fullyQualifiedPackageName, "."));
	}
	
	/**
	 * Returns the last element of the fully-qualified package name
	 * 
	 * @return a non-blank element
	 */
	public String getLastElement() {
		final List<String> elements = getElements();
		return elements.get(elements.size() - 1);
	}
}
