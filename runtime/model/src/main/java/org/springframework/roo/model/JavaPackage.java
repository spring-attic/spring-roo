package org.springframework.roo.model;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * Immutable representation of a Java package.
 * <p>
 * This class is used whenever a formal reference to a Java package is required.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class JavaPackage implements Comparable<JavaPackage> {

    private final String fullyQualifiedPackageName;

    /**
     * Construct a JavaPackage.
     * <p>
     * The fully qualified package name will be enforced as follows:
     * <ul>
     * <li>The rules listed in
     * {@link JavaSymbolName#assertJavaNameLegal(String)}
     * </ul>
     * 
     * @param fullyQualifiedPackageName the name (as per the above rules;
     *            mandatory)
     */
    public JavaPackage(final String fullyQualifiedPackageName) {
        Validate.notNull(fullyQualifiedPackageName,
                "Fully qualified package name required");
        JavaSymbolName.assertJavaNameLegal(fullyQualifiedPackageName);
        this.fullyQualifiedPackageName = fullyQualifiedPackageName;
    }

    public int compareTo(final JavaPackage o) {
        if (o == null) {
            return -1;
        }
        return fullyQualifiedPackageName.compareTo(o
                .getFullyQualifiedPackageName());
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof JavaPackage && compareTo((JavaPackage) obj) == 0;
    }

    /**
     * Returns the elements of this package's fully-qualified name
     * 
     * @return a non-empty list
     */
    public List<String> getElements() {
        return Arrays.asList(StringUtils.split(fullyQualifiedPackageName, "."));
    }

    /**
     * @return the fully qualified package name (complies with the rules
     *         specified in the constructor)
     */
    public String getFullyQualifiedPackageName() {
        return fullyQualifiedPackageName;
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

    @Override
    public int hashCode() {
        return fullyQualifiedPackageName.hashCode();
    }

    /**
     * Indicates whether this package is anywhere within the given package, in
     * other words is the same package or is a sub-package of the given one. For
     * example:
     * <ul>
     * <li>com.foo is within com.foo</li>
     * <li>com.foo.bar is within com.foo</li>
     * <li>com.foo is not within com.foo.bar</li>
     * </ul>
     * 
     * @param otherPackage the package to check against (can be
     *            <code>null</code>)
     * @return <code>false</code> if a <code>null</code> package is given
     */
    public boolean isWithin(final JavaPackage otherPackage) {
        return otherPackage != null
                && fullyQualifiedPackageName.startsWith(otherPackage
                        .getFullyQualifiedPackageName());
    }

    @Override
    public String toString() {
        return fullyQualifiedPackageName;
    }
}
