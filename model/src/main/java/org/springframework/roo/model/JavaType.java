package org.springframework.roo.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * The declaration of a Java type (i.e. contains no details of its members).
 * Instances are immutable.
 * <p>
 * Note that a Java type can be contained within a package, but a package is not
 * a type.
 * <p>
 * This class is used whenever a formal reference to a Java type is required. It
 * provides convenient ways to determine the type's simple name and package
 * name. A related {@link org.springframework.core.convert.converter.Converter}
 * is also offered.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class JavaType implements Comparable<JavaType> {

    public static final JavaType BOOLEAN_OBJECT = new JavaType(
            "java.lang.Boolean");
    public static final JavaType BOOLEAN_PRIMITIVE = new JavaType(
            "java.lang.Boolean", 0, DataType.PRIMITIVE, null, null);
    public static final JavaType BYTE_ARRAY_PRIMITIVE = new JavaType(
            "java.lang.Byte", 1, DataType.PRIMITIVE, null, null);
    public static final JavaType BYTE_OBJECT = new JavaType("java.lang.Byte");
    public static final JavaType BYTE_PRIMITIVE = new JavaType(
            "java.lang.Byte", 0, DataType.PRIMITIVE, null, null);
    public static final JavaType CHAR_OBJECT = new JavaType(
            "java.lang.Character");
    public static final JavaType CHAR_PRIMITIVE = new JavaType(
            "java.lang.Character", 0, DataType.PRIMITIVE, null, null);
    public static final JavaType CLASS = new JavaType("java.lang.Class");
    // The fully-qualified names of common collection types
    private static final Set<String> COMMON_COLLECTION_TYPES = new HashSet<String>();
    private static final String[] CORE_TYPE_PREFIXES = { "java.", "javax." };
    public static final JavaType DOUBLE_OBJECT = new JavaType(
            "java.lang.Double");
    public static final JavaType DOUBLE_PRIMITIVE = new JavaType(
            "java.lang.Double", 0, DataType.PRIMITIVE, null, null);
    public static final JavaType FLOAT_OBJECT = new JavaType("java.lang.Float");
    public static final JavaType FLOAT_PRIMITIVE = new JavaType(
            "java.lang.Float", 0, DataType.PRIMITIVE, null, null);
    public static final JavaType INT_OBJECT = new JavaType("java.lang.Integer");
    public static final JavaType INT_PRIMITIVE = new JavaType(
            "java.lang.Integer", 0, DataType.PRIMITIVE, null, null);
    public static final JavaType LONG_OBJECT = new JavaType("java.lang.Long");
    public static final JavaType LONG_PRIMITIVE = new JavaType(
            "java.lang.Long", 0, DataType.PRIMITIVE, null, null);
    public static final JavaType OBJECT = new JavaType("java.lang.Object");
    public static final JavaType SERIALIZABLE = new JavaType(
            "java.io.Serializable");
    public static final JavaType SHORT_OBJECT = new JavaType("java.lang.Short");
    public static final JavaType SHORT_PRIMITIVE = new JavaType(
            "java.lang.Short", 0, DataType.PRIMITIVE, null, null);
    public static final JavaType STRING = new JavaType("java.lang.String");

    /**
     * @deprecated use {@link #STRING} instead
     */
    @Deprecated public static final JavaType STRING_OBJECT = STRING;

    public static final JavaType VOID_OBJECT = new JavaType("java.lang.Void");
    public static final JavaType VOID_PRIMITIVE = new JavaType(
            "java.lang.Void", 0, DataType.PRIMITIVE, null, null);
    // Used for wildcard type parameters; it must be one or the other
    public static final JavaSymbolName WILDCARD_EXTENDS = new JavaSymbolName(
            "_ROO_WILDCARD_EXTENDS_"); // List<? extends YY>

    public static final JavaSymbolName WILDCARD_NEITHER = new JavaSymbolName(
            "_ROO_WILDCARD_NEITHER_"); // List<?>

    public static final JavaSymbolName WILDCARD_SUPER = new JavaSymbolName(
            "_ROO_WILDCARD_SUPER_"); // List<? super XXXX>

    static {
        COMMON_COLLECTION_TYPES.add(ArrayList.class.getName());
        COMMON_COLLECTION_TYPES.add(Collection.class.getName());
        COMMON_COLLECTION_TYPES.add(HashMap.class.getName());
        COMMON_COLLECTION_TYPES.add(HashSet.class.getName());
        COMMON_COLLECTION_TYPES.add(List.class.getName());
        COMMON_COLLECTION_TYPES.add(Map.class.getName());
        COMMON_COLLECTION_TYPES.add(Set.class.getName());
        COMMON_COLLECTION_TYPES.add(TreeMap.class.getName());
        COMMON_COLLECTION_TYPES.add(Vector.class.getName());
    }

    /**
     * Factory method for a {@link JavaType} with full details. Recall that
     * {@link JavaType} is immutable and therefore this is the only way of
     * setting these non-default values. This is a factory method rather than a
     * constructor so as not to cause ambiguity problems for existing callers of
     * {@link #JavaType(String, int, DataType, JavaSymbolName, List)}
     * 
     * @param fullyQualifiedTypeName the name (as per the rules above)
     * @param arrayDimensions the number of array dimensions (0 = not an array,
     *            1 = one-dimensional array, etc.)
     * @param dataType the {@link DataType} (required)
     * @param argName the type argument name to this particular Java type (can
     *            be null if unassigned)
     * @param parameters the type parameters applicable (can be null if there
     *            aren't any)
     * @return a JavaType instance constructed based on the passed in details
     * @since 1.2.0
     */
    public static JavaType getInstance(final String fullyQualifiedTypeName,
            final int arrayDimensions, final DataType dataType,
            final JavaSymbolName argName, final JavaType... parameters) {
        return new JavaType(fullyQualifiedTypeName, arrayDimensions, dataType,
                argName, Arrays.asList(parameters));
    }

    /**
     * Factory method for a {@link JavaType} with full details. Recall that
     * {@link JavaType} is immutable and therefore this is the only way of
     * setting these non-default values. This is a factory method rather than a
     * constructor so as not to cause ambiguity problems for existing callers of
     * {@link #JavaType(String, int, DataType, JavaSymbolName, List)}
     * 
     * @param fullyQualifiedTypeName the name (as per the rules above)
     * @param enclosingType the type's enclosing type
     * @param arrayDimensions the number of array dimensions (0 = not an array,
     *            1 = one-dimensional array, etc.)
     * @param dataType the {@link DataType} (required)
     * @param argName the type argument name to this particular Java type (can
     *            be null if unassigned)
     * @param parameters the type parameters applicable (can be null if there
     *            aren't any)
     * @return a JavaType instance constructed based on the passed in details
     * @since 1.2.0
     */
    public static JavaType getInstance(final String fullyQualifiedTypeName,
            final JavaType enclosingType, final int arrayDimensions,
            final DataType dataType, final JavaSymbolName argName,
            final JavaType... parameters) {
        return new JavaType(fullyQualifiedTypeName, enclosingType,
                arrayDimensions, dataType, argName, Arrays.asList(parameters));
    }

    /**
     * Returns a {@link JavaType} for a list of the given element type
     * 
     * @param elementType the type of element in the list (required)
     * @return a non-<code>null</code> type
     * @since 1.2.0
     */
    public static JavaType listOf(final JavaType elementType) {
        return new JavaType(List.class.getName(), 0, DataType.TYPE, null,
                Arrays.asList(elementType));
    }

    private final JavaSymbolName argName;
    private final int arrayDimensions;
    private final DataType dataType;
    private final boolean defaultPackage;
    private final JavaType enclosingType;
    private final String fullyQualifiedTypeName;
    private final List<JavaType> parameters;
    private final String simpleTypeName;

    /**
     * Constructor equivalent to {@link #JavaType(String)}, but takes a Class
     * for convenience and type safety.
     * 
     * @param type the class for which to create an instance (required)
     * @since 1.2.0
     */
    public JavaType(final Class<?> type) {
        this(type.getName());
    }

    /**
     * Constructs a {@link JavaType}.
     * <p>
     * The fully qualified type name will be enforced as follows:
     * <ul>
     * <li>The rules listed in
     * {@link JavaSymbolName#assertJavaNameLegal(String)}
     * <li>First letter of simple type name must be upper-case</li>
     * </ul>
     * <p>
     * A fully qualified type name may include or exclude a package designator.
     * 
     * @param fullyQualifiedTypeName the name (as per the above rules;
     *            mandatory)
     */
    public JavaType(final String fullyQualifiedTypeName) {
        this(fullyQualifiedTypeName, 0, DataType.TYPE, null, null);
    }

    /**
     * Construct a {@link JavaType} with full details. Recall that
     * {@link JavaType} is immutable and therefore this is the only way of
     * setting these non-default values.
     * 
     * @param fullyQualifiedTypeName the name (as per the rules above)
     * @param arrayDimensions the number of array dimensions (0 = not an array,
     *            1 = one-dimensional array, etc.)
     * @param dataType the {@link DataType} (required)
     * @param argName the type argument name to this particular Java type (can
     *            be null if unassigned)
     * @param parameters the type parameters applicable (can be null if there
     *            aren't any)
     */
    public JavaType(final String fullyQualifiedTypeName,
            final int arrayDimensions, final DataType dataType,
            final JavaSymbolName argName, final List<JavaType> parameters) {
        this(fullyQualifiedTypeName, null, arrayDimensions, dataType, argName,
                parameters);
    }

    /**
     * Constructs a {@link JavaType}.
     * <p>
     * The fully qualified type name will be enforced as follows:
     * <ul>
     * <li>The rules listed in
     * {@link JavaSymbolName#assertJavaNameLegal(String)}
     * <li>First letter of simple type name must be upper-case</li>
     * </ul>
     * <p>
     * A fully qualified type name may include or exclude a package designator.
     * 
     * @param fullyQualifiedTypeName the name (as per the above rules;
     *            mandatory)
     * @param enclosingType the type's enclosing type
     */
    public JavaType(final String fullyQualifiedTypeName,
            final JavaType enclosingType) {
        this(fullyQualifiedTypeName, enclosingType, 0, DataType.TYPE, null,
                null);
    }

    /**
     * Construct a {@link JavaType} with full details. Recall that
     * {@link JavaType} is immutable and therefore this is the only way of
     * setting these non-default values.
     * 
     * @param fullyQualifiedTypeName the name (as per the rules above)
     * @param enclosingType the type's enclosing type
     * @param arrayDimensions the number of array dimensions (0 = not an array,
     *            1 = one-dimensional array, etc.)
     * @param dataType the {@link DataType} (required)
     * @param argName the type argument name to this particular Java type (can
     *            be null if unassigned)
     * @param parameters the type parameters applicable (can be null if there
     *            aren't any)
     */
    public JavaType(final String fullyQualifiedTypeName,
            final JavaType enclosingType, final int arrayDimensions,
            final DataType dataType, final JavaSymbolName argName,
            final List<JavaType> parameters) {
        Validate.notBlank(fullyQualifiedTypeName,
                "Fully qualified type name required");
        Validate.notNull(dataType, "Data type required");
        JavaSymbolName.assertJavaNameLegal(fullyQualifiedTypeName);
        this.argName = argName;
        this.arrayDimensions = arrayDimensions;
        this.dataType = dataType;
        this.fullyQualifiedTypeName = fullyQualifiedTypeName;
        defaultPackage = !fullyQualifiedTypeName.contains(".");
        if (enclosingType == null) {
            this.enclosingType = determineEnclosingType();
        }
        else {
            this.enclosingType = enclosingType;
        }
        if (defaultPackage) {
            simpleTypeName = fullyQualifiedTypeName;
        }
        else {
            final int offset = fullyQualifiedTypeName.lastIndexOf(".");
            simpleTypeName = fullyQualifiedTypeName.substring(offset + 1);
        }

        this.parameters = new ArrayList<JavaType>();
        if (parameters != null) {
            this.parameters.addAll(parameters);
        }
    }

    public int compareTo(final JavaType o) {
        // NB: If adding more fields to this class ensure the equals(Object)
        // method is updated accordingly
        if (o == null) {
            return -1;
        }
        if (equals(o)) {
            return 0;
        }
        return toString().compareTo(o.toString());
    }

    private JavaType determineEnclosingType() {
        final int offset = fullyQualifiedTypeName.lastIndexOf(".");
        if (offset == -1) {
            // There is no dot in the name, so there's no way there's an
            // enclosing type
            return null;
        }
        final String possibleName = fullyQualifiedTypeName.substring(0, offset);
        final int offset2 = possibleName.lastIndexOf(".");

        // Start by handling if the type name is Foo.Bar (ie an enclosed type
        // within the default package)
        String enclosedWithinPackage = null;
        String enclosedWithinTypeName = possibleName;

        // Handle the probability the type name is within a package like
        // com.alpha.Foo.Bar
        if (offset2 > -1) {
            enclosedWithinPackage = possibleName.substring(0, offset2);
            enclosedWithinTypeName = possibleName.substring(offset2 + 1);
        }

        if (Character.isUpperCase(enclosedWithinTypeName.charAt(0))) {
            // First letter is upper-case, so treat it as a type name for now
            final String preTypeNamePortion = enclosedWithinPackage == null ? ""
                    : enclosedWithinPackage + ".";
            return new JavaType(preTypeNamePortion + enclosedWithinTypeName);
        }

        return null;
    }

    @Override
    public boolean equals(final Object obj) {
        // NB: Not using the normal convention of delegating to compareTo (for
        // efficiency reasons)
        return obj != null
                && obj instanceof JavaType
                && fullyQualifiedTypeName.equals(((JavaType) obj)
                        .getFullyQualifiedTypeName())
                && dataType == ((JavaType) obj).getDataType()
                && arrayDimensions == ((JavaType) obj).getArray()
                && ((JavaType) obj).getParameters().containsAll(parameters);
    }

    public JavaSymbolName getArgName() {
        return argName;
    }

    public int getArray() {
        return arrayDimensions;
    }

    private String getArraySuffix() {
        if (arrayDimensions == 0) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arrayDimensions; i++) {
            sb.append("[]");
        }
        return sb.toString();
    }

    /**
     * Returns this type's base type, being <code>this</code> for single-valued
     * types, otherwise the element type for collection types.
     * 
     * @return <code>null</code> for an untyped collection
     * @since 1.2.1
     */
    public JavaType getBaseType() {
        if (isCommonCollectionType()) {
            if (parameters.isEmpty()) {
                return null;
            }
            return parameters.get(0);
        }
        return this;
    }

    public DataType getDataType() {
        return dataType;
    }

    /**
     * @return the enclosing type, if any (will return null if there is no
     *         enclosing type)
     */
    public JavaType getEnclosingType() {
        return enclosingType;
    }

    /**
     * @return the fully qualified name (complies with the rules specified in
     *         the constructor)
     */
    public String getFullyQualifiedTypeName() {
        return fullyQualifiedTypeName;
    }

    /**
     * Obtains the name of this type, including type parameters. It will be
     * formatted in a manner compatible with non-static use. No type name import
     * resolution will take place. This is a side-effect free method.
     * 
     * @return the type name, including parameters, as legal Java code (never
     *         null or empty)
     */
    public String getNameIncludingTypeParameters() {
        return getNameIncludingTypeParameters(false, null,
                new HashMap<String, String>());
    }

    /**
     * Obtains the name of this type, including type parameters. It will be
     * formatted in a manner compatible with either static or non-static usage,
     * as per the passed argument. Type names will attempt to be resolved (and
     * automatically registered) using the passed resolver. This method will
     * have side-effects on the passed resolver.
     * 
     * @param staticForm true if the output should be compatible with static use
     * @param resolver the resolver to use (may be null in which case no import
     *            resolution will occur)
     * @return the type name, including parameters, as legal Java code (never
     *         null or empty)
     */
    public String getNameIncludingTypeParameters(final boolean staticForm,
            final ImportRegistrationResolver resolver) {
        return getNameIncludingTypeParameters(staticForm, resolver,
                new HashMap<String, String>());
    }

    private String getNameIncludingTypeParameters(final boolean staticForm,
            final ImportRegistrationResolver resolver,
            final Map<String, String> types) {
        if (DataType.PRIMITIVE == dataType) {
            Validate.isTrue(parameters.isEmpty(),
                    "A primitive cannot have parameters");
            if (fullyQualifiedTypeName.equals(Integer.class.getName())) {
                return "int" + getArraySuffix();
            }
            else if (fullyQualifiedTypeName.equals(Character.class.getName())) {
                return "char" + getArraySuffix();
            }
            else if (fullyQualifiedTypeName.equals(Void.class.getName())) {
                return "void";
            }
            return StringUtils.uncapitalize(getSimpleTypeName()
                    + getArraySuffix());
        }

        final StringBuilder sb = new StringBuilder();

        if (WILDCARD_EXTENDS.equals(argName)) {
            sb.append("?");
            if (dataType == DataType.TYPE || !staticForm) {
                sb.append(" extends ");
            }
            else if (types.containsKey(fullyQualifiedTypeName)) {
                sb.append(" extends ")
                        .append(types.get(fullyQualifiedTypeName));
            }
        }
        else if (WILDCARD_SUPER.equals(argName)) {
            sb.append("?");
            if (dataType == DataType.TYPE || !staticForm) {
                sb.append(" super ");
            }
            else if (types.containsKey(fullyQualifiedTypeName)) {
                sb.append(" extends ")
                        .append(types.get(fullyQualifiedTypeName));
            }
        }
        else if (WILDCARD_NEITHER.equals(argName)) {
            sb.append("?");
        }
        else if (argName != null && !staticForm) {
            sb.append(argName);
            if (dataType == DataType.TYPE) {
                if (!fullyQualifiedTypeName.equals(OBJECT
                        .getFullyQualifiedTypeName())) {
                    sb.append(" extends ");
                }
            }
        }

        if (!WILDCARD_NEITHER.equals(argName)) {
            // It wasn't a WILDCARD_NEITHER, so we might need to continue with
            // more details
            if (dataType == DataType.TYPE || !staticForm) {
                if (resolver != null) {
                    if (resolver
                            .isFullyQualifiedFormRequiredAfterAutoImport(this)) {
                        sb.append(fullyQualifiedTypeName);
                    }
                    else {
                        sb.append(getSimpleTypeName());
                    }
                }
                else {
                    if (fullyQualifiedTypeName.equals(OBJECT
                            .getFullyQualifiedTypeName())) {
                        // It's Object, so we need to only append if this isn't
                        // a type arg
                        if (argName == null) {
                            sb.append(fullyQualifiedTypeName);
                        }
                    }
                    else {
                        // It's ok to just append it as it's not Object
                        sb.append(fullyQualifiedTypeName);
                    }
                }
            }

            if (parameters.size() > 0
                    && (dataType == DataType.TYPE || !staticForm)) {
                sb.append("<");
                int counter = 0;
                for (final JavaType param : parameters) {
                    counter++;
                    if (counter > 1) {
                        sb.append(", ");
                    }
                    sb.append(param.getNameIncludingTypeParameters(staticForm,
                            resolver, types));
                    counter++;
                }
                sb.append(">");
            }

            sb.append(getArraySuffix());
        }

        if (argName != null && !argName.equals(WILDCARD_EXTENDS)
                && !argName.equals(WILDCARD_SUPER)
                && !argName.equals(WILDCARD_NEITHER)) {
            types.put(argName.getSymbolName(), sb.toString());
        }

        return sb.toString();
    }

    /**
     * @return the package name (never null)
     */
    public JavaPackage getPackage() {
        if (isDefaultPackage()
                && !Character.isUpperCase(fullyQualifiedTypeName.charAt(0))) {
            return new JavaPackage("");
        }

        if (enclosingType != null) {
            final String enclosingTypeFullyQualifiedTypeName = enclosingType
                    .getFullyQualifiedTypeName();
            final int offset = enclosingTypeFullyQualifiedTypeName
                    .lastIndexOf(".");
            // Handle case where the package name after the last period starts
            // with a capital letter.
            if (offset > -1
                    && Character
                            .isUpperCase(enclosingTypeFullyQualifiedTypeName
                                    .charAt(offset + 1))) {
                return new JavaPackage(enclosingTypeFullyQualifiedTypeName);
            }
            return enclosingType.getPackage();
        }

        final int offset = fullyQualifiedTypeName.lastIndexOf(".");
        return offset == -1 ? new JavaPackage("") : new JavaPackage(
                fullyQualifiedTypeName.substring(0, offset));
    }

    public List<JavaType> getParameters() {
        return Collections.unmodifiableList(parameters);
    }

    /**
     * Returns the name of the source file that contains this type, starting
     * from its base package. For example, for a type called "com.example.Foo",
     * this method returns "com/example/Foo.java", delimited by the platform-
     * specific separator ("/" in this example).
     * 
     * @return a non-blank path
     */
    public String getRelativeFileName() {
        return fullyQualifiedTypeName.replace('.', File.separatorChar)
                + ".java";
    }

    /**
     * @return the name (does not contain any periods; never null or empty)
     */
    public String getSimpleTypeName() {
        return simpleTypeName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime
                * result
                + (fullyQualifiedTypeName == null ? 0 : fullyQualifiedTypeName
                        .hashCode());
        result = prime * result + (dataType == null ? 0 : dataType.hashCode());
        result = prime * result + arrayDimensions;
        return result;
    }

    public boolean isArray() {
        return arrayDimensions > 0;
    }

    /**
     * Indicates whether this type is any kind of boolean.
     * 
     * @return see above
     * @since 1.2.1
     */
    public boolean isBoolean() {
        return equals(BOOLEAN_OBJECT) || equals(BOOLEAN_PRIMITIVE);
    }

    public boolean isCommonCollectionType() {
        return COMMON_COLLECTION_TYPES.contains(fullyQualifiedTypeName);
    }

    /**
     * Indicates whether this type is part of core Java.
     * 
     * @return see above
     */
    public boolean isCoreType() {
        for (final String coreTypePrefix : CORE_TYPE_PREFIXES) {
            if (fullyQualifiedTypeName.startsWith(coreTypePrefix)) {
                return true;
            }
        }
        return false;
    }

    public boolean isDefaultPackage() {
        return defaultPackage;
    }

    /**
     * Indicates whether a field or variable of this type can contain multiple
     * values
     * 
     * @return see above
     * @since 1.2.0
     */
    public boolean isMultiValued() {
        return isCommonCollectionType() || isArray();
    }

    /**
     * Indicates whether this type is a primitive, or in the case of an array,
     * whether its elements are primitive.
     * 
     * @return see above
     */
    public boolean isPrimitive() {
        return DataType.PRIMITIVE == dataType;
    }

    @Override
    public String toString() {
        return getNameIncludingTypeParameters();
    }
}
