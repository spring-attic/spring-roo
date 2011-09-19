package org.springframework.roo.model;

/**
 * Constants for JDK {@link JavaType}s.
 * 
 * Use them in preference to creating new instances of these types.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
public final class JdkJavaType {

	// Constants
	public static final JavaType BIG_DECIMAL = new JavaType("java.math.BigDecimal");
	public static final JavaType BIG_INTEGER = new JavaType("java.math.BigInteger");

	/**
	 * Constructor is private to prevent instantiation
	 */
	private JdkJavaType() {
	}
}