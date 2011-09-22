package org.springframework.roo.model;

import static org.springframework.roo.model.JavaType.DOUBLE_OBJECT;
import static org.springframework.roo.model.JavaType.DOUBLE_PRIMITIVE;
import static org.springframework.roo.model.JavaType.FLOAT_OBJECT;
import static org.springframework.roo.model.JavaType.FLOAT_PRIMITIVE;

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

	// java.sql
	public static final JavaType ARRAY = new JavaType("java.sql.Array");
	public static final JavaType BLOB = new JavaType("java.sql.Blob");
	public static final JavaType CLOB = new JavaType("java.sql.Clob");
	public static final JavaType REF = new JavaType("java.sql.Ref");
	public static final JavaType STRUCT = new JavaType("java.sql.Struct");
	
	// java.util
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
	
	// Static methods
	
	public static boolean isIntegerType(final JavaType javaType) {
		return javaType.equals(BIG_INTEGER) || javaType.equals(JavaType.INT_PRIMITIVE) || javaType.equals(JavaType.INT_OBJECT) || javaType.equals(JavaType.LONG_PRIMITIVE) || javaType.equals(JavaType.LONG_OBJECT) || javaType.equals(JavaType.SHORT_PRIMITIVE) || javaType.equals(JavaType.SHORT_OBJECT);
	}
	
	public static boolean isDecimalType(final JavaType javaType) {
		return javaType.equals(BIG_DECIMAL) || isDoubleOrFloat(javaType);
	}

	public static boolean isDoubleOrFloat(final JavaType javaType) {
		return javaType.equals(DOUBLE_OBJECT) || javaType.equals(DOUBLE_PRIMITIVE) || javaType.equals(FLOAT_OBJECT) || javaType.equals(FLOAT_PRIMITIVE);
	}
	
	public static boolean isDateField(final JavaType javaType) {
		return javaType.equals(DATE) || javaType.equals(CALENDAR) || javaType.equals(GREGORIAN_CALENDAR);
	}

	/**
	 * Constructor is private to prevent instantiation
	 */
	private JdkJavaType() {
	}
}