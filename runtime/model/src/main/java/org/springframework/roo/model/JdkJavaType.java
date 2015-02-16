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
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Ref;
import java.sql.Struct;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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

import org.apache.commons.lang3.Validate;

/**
 * Constants for JDK {@link JavaType}s. Use them in preference to creating new
 * instances of these types.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
public final class JdkJavaType {

    // java.sql
    public static final JavaType ARRAY = new JavaType(Array.class);
    // java.util
    public static final JavaType ARRAY_LIST = new JavaType(ArrayList.class);

    public static final JavaType ARRAYS = new JavaType(Arrays.class);

    // java.math
    public static final JavaType BIG_DECIMAL = new JavaType(BigDecimal.class);
    public static final JavaType BIG_INTEGER = new JavaType(BigInteger.class);
    public static final JavaType BLOB = new JavaType(Blob.class);

    // java.io
    public static final JavaType BYTE_ARRAY_INPUT_STREAM = new JavaType(
            ByteArrayInputStream.class);
    public static final JavaType CALENDAR = new JavaType(Calendar.class);

    public static final JavaType CLOB = new JavaType(Clob.class);
    public static final JavaType COLLECTION = new JavaType(Collection.class);

    public static final JavaType DATE = new JavaType(Date.class);

    // java.text
    public static final JavaType DATE_FORMAT = new JavaType(DateFormat.class);
    // java.lang
    public static final JavaType EXCEPTION = new JavaType(Exception.class);
    public static final JavaType GREGORIAN_CALENDAR = new JavaType(
            GregorianCalendar.class);
    public static final JavaType HASH_SET = new JavaType(HashSet.class);
    public static final JavaType ITERATOR = new JavaType(Iterator.class);

    private static final List<String> javaLangSimpleTypeNames = new ArrayList<String>();
    private static final List<String> javaLangTypes = new ArrayList<String>();

    public static final JavaType LIST = new JavaType(List.class);
    public static final JavaType MAP = new JavaType(Map.class);
    // javax.annotation
    public static final JavaType POST_CONSTRUCT = new JavaType(
            PostConstruct.class);
    // java.beans
    public static final JavaType PROPERTY_EDITOR_SUPPORT = new JavaType(
            PropertyEditorSupport.class);
    public static final JavaType RANDOM = new JavaType(Random.class);
    public static final JavaType REF = new JavaType(Ref.class);
    // java.security
    public static final JavaType SECURE_RANDOM = new JavaType(
            SecureRandom.class);
    public static final JavaType SERIALIZABLE = new JavaType(Serializable.class);
    public static final JavaType SET = new JavaType(Set.class);
    public static final JavaType SIMPLE_DATE_FORMAT = new JavaType(
            SimpleDateFormat.class);
    public static final JavaType STRUCT = new JavaType(Struct.class);
    public static final JavaType SUPPRESS_WARNINGS = new JavaType(
            SuppressWarnings.class);
    // java.sql
    public static final JavaType TIMESTAMP = new JavaType(Timestamp.class);

    public static final JavaType UNSUPPORTED_ENCODING_EXCEPTION = new JavaType(
            UnsupportedEncodingException.class);

    // Static methods

    static {
        javaLangSimpleTypeNames.add("Appendable");
        javaLangSimpleTypeNames.add("CharSequence");
        javaLangSimpleTypeNames.add("Cloneable");
        javaLangSimpleTypeNames.add("Comparable");
        javaLangSimpleTypeNames.add("Iterable");
        javaLangSimpleTypeNames.add("Readable");
        javaLangSimpleTypeNames.add("Runnable");
        javaLangSimpleTypeNames.add("Boolean");
        javaLangSimpleTypeNames.add("Byte");
        javaLangSimpleTypeNames.add("Character");
        javaLangSimpleTypeNames.add("Class");
        javaLangSimpleTypeNames.add("ClassLoader");
        javaLangSimpleTypeNames.add("Compiler");
        javaLangSimpleTypeNames.add("Double");
        javaLangSimpleTypeNames.add("Enum");
        javaLangSimpleTypeNames.add("Float");
        javaLangSimpleTypeNames.add("InheritableThreadLocal");
        javaLangSimpleTypeNames.add("Integer");
        javaLangSimpleTypeNames.add("Long");
        javaLangSimpleTypeNames.add("Math");
        javaLangSimpleTypeNames.add("Number");
        javaLangSimpleTypeNames.add("Object");
        javaLangSimpleTypeNames.add("Package");
        javaLangSimpleTypeNames.add("Process");
        javaLangSimpleTypeNames.add("ProcessBuilder");
        javaLangSimpleTypeNames.add("Runtime");
        javaLangSimpleTypeNames.add("RuntimePermission");
        javaLangSimpleTypeNames.add("SecurityManager");
        javaLangSimpleTypeNames.add("Short");
        javaLangSimpleTypeNames.add("StackTraceElement");
        javaLangSimpleTypeNames.add("StrictMath");
        javaLangSimpleTypeNames.add("String");
        javaLangSimpleTypeNames.add("StringBuilder");
        javaLangSimpleTypeNames.add("StringBuilder");
        javaLangSimpleTypeNames.add("System");
        javaLangSimpleTypeNames.add("Thread");
        javaLangSimpleTypeNames.add("ThreadGroup");
        javaLangSimpleTypeNames.add("ThreadLocal");
        javaLangSimpleTypeNames.add("Throwable");
        javaLangSimpleTypeNames.add("Void");
        javaLangSimpleTypeNames.add("ArithmeticException");
        javaLangSimpleTypeNames.add("ArrayIndexOutOfBoundsException");
        javaLangSimpleTypeNames.add("ArrayStoreException");
        javaLangSimpleTypeNames.add("ClassCastException");
        javaLangSimpleTypeNames.add("ClassNotFoundException");
        javaLangSimpleTypeNames.add("CloneNotSupportedException");
        javaLangSimpleTypeNames.add("EnumConstantNotPresentException");
        javaLangSimpleTypeNames.add("Exception");
        javaLangSimpleTypeNames.add("IllegalAccessException");
        javaLangSimpleTypeNames.add("IllegalArgumentException");
        javaLangSimpleTypeNames.add("IllegalMonitorStateException");
        javaLangSimpleTypeNames.add("IllegalStateException");
        javaLangSimpleTypeNames.add("IllegalThreadStateException");
        javaLangSimpleTypeNames.add("IndexOutOfBoundsException");
        javaLangSimpleTypeNames.add("InstantiationException");
        javaLangSimpleTypeNames.add("InterruptedException");
        javaLangSimpleTypeNames.add("NegativeArraySizeException");
        javaLangSimpleTypeNames.add("NoSuchFieldException");
        javaLangSimpleTypeNames.add("NoSuchMethodException");
        javaLangSimpleTypeNames.add("NullPointerException");
        javaLangSimpleTypeNames.add("NumberFormatException");
        javaLangSimpleTypeNames.add("RuntimeException");
        javaLangSimpleTypeNames.add("SecurityException");
        javaLangSimpleTypeNames.add("StringIndexOutOfBoundsException");
        javaLangSimpleTypeNames.add("TypeNotPresentException");
        javaLangSimpleTypeNames.add("UnsupportedOperationException");
        javaLangSimpleTypeNames.add("AbstractMethodError");
        javaLangSimpleTypeNames.add("AssertionError");
        javaLangSimpleTypeNames.add("ClassCircularityError");
        javaLangSimpleTypeNames.add("ClassFormatError");
        javaLangSimpleTypeNames.add("Error");
        javaLangSimpleTypeNames.add("ExceptionInInitializerError");
        javaLangSimpleTypeNames.add("IllegalAccessError");
        javaLangSimpleTypeNames.add("IncompatibleClassChangeError");
        javaLangSimpleTypeNames.add("InstantiationError");
        javaLangSimpleTypeNames.add("InternalError");
        javaLangSimpleTypeNames.add("LinkageError");
        javaLangSimpleTypeNames.add("NoClassDefFoundError");
        javaLangSimpleTypeNames.add("NoSuchFieldError");
        javaLangSimpleTypeNames.add("NoSuchMethodError");
        javaLangSimpleTypeNames.add("OutOfMemoryError");
        javaLangSimpleTypeNames.add("StackOverflowError");
        javaLangSimpleTypeNames.add("ThreadDeath");
        javaLangSimpleTypeNames.add("UnknownError");
        javaLangSimpleTypeNames.add("UnsatisfiedLinkError");
        javaLangSimpleTypeNames.add("UnsupportedClassVersionError");
        javaLangSimpleTypeNames.add("VerifyError");
        javaLangSimpleTypeNames.add("VirtualMachineError");
    }

    public static boolean isDateField(final JavaType javaType) {
        return javaType.equals(DATE) || javaType.equals(CALENDAR);
    }

    public static boolean isDecimalType(final JavaType javaType) {
        return javaType.equals(BIG_DECIMAL) || isDoubleOrFloat(javaType);
    }

    public static boolean isDoubleOrFloat(final JavaType javaType) {
        return javaType.equals(DOUBLE_OBJECT)
                || javaType.equals(DOUBLE_PRIMITIVE)
                || javaType.equals(FLOAT_OBJECT)
                || javaType.equals(FLOAT_PRIMITIVE);
    }

    public static boolean isIntegerType(final JavaType javaType) {
        return javaType.equals(BIG_INTEGER) || javaType.equals(INT_PRIMITIVE)
                || javaType.equals(INT_OBJECT)
                || javaType.equals(LONG_PRIMITIVE)
                || javaType.equals(LONG_OBJECT)
                || javaType.equals(SHORT_PRIMITIVE)
                || javaType.equals(SHORT_OBJECT);
    }

    /**
     * Determines whether the presented java type is in the java.lang package or
     * not.
     * 
     * @param javaType the Java type (required)
     * @return whether the type is declared as part of java.lang
     */
    public static boolean isPartOfJavaLang(final JavaType javaType) {
        Validate.notNull(javaType, "Java type required");
        if (javaLangTypes.isEmpty()) {
            for (final String javaLangSimpleTypeName : javaLangSimpleTypeNames) {
                javaLangTypes.add("java.lang." + javaLangSimpleTypeName);
            }
        }
        return javaLangTypes.contains(javaType.getFullyQualifiedTypeName());
    }

    /**
     * Determines whether the presented simple type name is part of java.lang or
     * not.
     * 
     * @param simpleTypeName the simple type name (required)
     * @return whether the type is declared as part of java.lang
     */
    public static boolean isPartOfJavaLang(final String simpleTypeName) {
        Validate.notBlank(simpleTypeName, "Simple type name required");
        return javaLangSimpleTypeNames.contains(simpleTypeName);
    }

    /**
     * Constructor is private to prevent instantiation
     */
    private JdkJavaType() {
    }
}