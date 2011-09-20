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

	// java.math 
	public static final JavaType BIG_DECIMAL = new JavaType("java.math.BigDecimal");
	public static final JavaType BIG_INTEGER = new JavaType("java.math.BigInteger");
	
	// java.security
	public static final JavaType SECURE_RANDOM = new JavaType("java.security.SecureRandom");

	// java.util constants
	public static final JavaType ARRAY_LIST = new JavaType("java.util.ArrayList");
	public static final JavaType ARRAYS = new JavaType("java.util.Arrays");
	public static final JavaType CALENDAR = new JavaType("java.util.Calendar");
	public static final JavaType COLLECTION = new JavaType("java.util.Collection");
	public static final JavaType DATE = new JavaType("java.util.Date");
	public static final JavaType GREGORIAN_CALENDAR = new JavaType("java.util.GregorianCalendar");
	public static final JavaType HASH_SET = new JavaType("java.util.HashSet");
	public static final JavaType LIST = new JavaType("java.util.List");
	public static final JavaType RANDOM = new JavaType("java.util.Random");
	public static final JavaType SET = new JavaType("java.util.Set");
	public static final JavaType SUPPRESS_WARNINGS = new JavaType("java.lang.SuppressWarnings");
	
	// javax.annotation 
	public static final JavaType POST_CONSTRUCT = new JavaType("javax.annotation.PostConstruct");

	/**
	 * Constructor is private to prevent instantiation
	 */
	private JdkJavaType() {
	}
}