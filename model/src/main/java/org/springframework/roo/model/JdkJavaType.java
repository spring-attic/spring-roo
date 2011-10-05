package org.springframework.roo.model;

import static org.springframework.roo.model.JavaType.DOUBLE_OBJECT;
import static org.springframework.roo.model.JavaType.DOUBLE_PRIMITIVE;
import static org.springframework.roo.model.JavaType.FLOAT_OBJECT;
import static org.springframework.roo.model.JavaType.FLOAT_PRIMITIVE;
import static org.springframework.roo.model.JavaType.INT_OBJECT;
import static org.springframework.roo.model.JavaType.INT_PRIMITIVE;
import static org.springframework.roo.model.JavaType.LONG_OBJECT;
import static org.springframework.roo.model.JavaType.LONG_PRIMITIVE;
import static org.springframework.roo.model.JavaType.SHORT_OBJECT;
import static org.springframework.roo.model.JavaType.SHORT_PRIMITIVE;

import java.beans.PropertyEditorSupport;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Ref;
import java.sql.Struct;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.annotation.PostConstruct;

/**
 * Constants for JDK {@link JavaType}s.
 *
 * Use them in preference to creating new instances of these types.
 *
 * @author Alan Stewart
 * @since 1.2.0
 */
public final class JdkJavaType {
	
	// java.beans
	public static final JavaType PROPERTY_EDITOR_SUPPORT = new JavaType(PropertyEditorSupport.class);
	
	// java.io
	public static final JavaType SERIALIZABLE = new JavaType(Serializable.class);

	// java.lang
	public static final JavaType SUPPRESS_WARNINGS = new JavaType(SuppressWarnings.class);
	
	// java.math
	public static final JavaType BIG_DECIMAL = new JavaType(BigDecimal.class);
	public static final JavaType BIG_INTEGER = new JavaType(BigInteger.class);

	// java.security
	public static final JavaType SECURE_RANDOM = new JavaType(SecureRandom.class);

	// java.sql
	public static final JavaType ARRAY = new JavaType(Array.class);
	public static final JavaType BLOB = new JavaType(Blob.class);
	public static final JavaType CLOB = new JavaType(Clob.class);
	public static final JavaType REF = new JavaType(Ref.class);
	public static final JavaType STRUCT = new JavaType(Struct.class);
	
	// java.text
	public static final JavaType DATE_FORMAT = new JavaType(DateFormat.class);

	// java.util
	public static final JavaType ARRAY_LIST = new JavaType(ArrayList.class);
	public static final JavaType ARRAYS = new JavaType(Arrays.class);
	public static final JavaType CALENDAR = new JavaType(Calendar.class);
	public static final JavaType COLLECTION = new JavaType(Collection.class);
	public static final JavaType DATE = new JavaType(Date.class);
	public static final JavaType GREGORIAN_CALENDAR = new JavaType(GregorianCalendar.class);
	public static final JavaType HASH_SET = new JavaType(HashSet.class);
	public static final JavaType ITERATOR = new JavaType(Iterator.class);
	public static final JavaType LIST = new JavaType(List.class);
	public static final JavaType MAP = new JavaType(Map.class);
	public static final JavaType RANDOM = new JavaType(Random.class);
	public static final JavaType SET = new JavaType(Set.class);

	// javax.annotation
	public static final JavaType POST_CONSTRUCT = new JavaType(PostConstruct.class);

	// Static methods

	public static boolean isIntegerType(final JavaType javaType) {
		return javaType.equals(BIG_INTEGER) || javaType.equals(INT_PRIMITIVE) || javaType.equals(INT_OBJECT) || javaType.equals(LONG_PRIMITIVE) || javaType.equals(LONG_OBJECT) || javaType.equals(SHORT_PRIMITIVE) || javaType.equals(SHORT_OBJECT);
	}

	public static boolean isDecimalType(final JavaType javaType) {
		return javaType.equals(BIG_DECIMAL) || isDoubleOrFloat(javaType);
	}

	public static boolean isDoubleOrFloat(final JavaType javaType) {
		return javaType.equals(DOUBLE_OBJECT) || javaType.equals(DOUBLE_PRIMITIVE) || javaType.equals(FLOAT_OBJECT) || javaType.equals(FLOAT_PRIMITIVE);
	}

	public static boolean isDateField(final JavaType javaType) {
		return javaType.equals(DATE) || javaType.equals(CALENDAR);
	}

	/**
	 * Constructor is private to prevent instantiation
	 */
	private JdkJavaType() {
	}
}