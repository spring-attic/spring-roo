package org.springframework.roo.model;

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

import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * The declaration of a Java type (i.e. contains no details of its members).
 * Instances are immutable.
 *
 * <p>
 * Note that a Java type can be contained within a package, but a package is not a type.
 *
 * <p>
 * This class is used whenever a formal reference to a Java type is required.
 * It provides convenient ways to determine the type's simple name and package name.
 * A related {@link org.springframework.core.convert.converter.Converter} is also offered.
 *
 * @author Ben Alex
 * @since 1.0
 */
public class JavaType implements Comparable<JavaType> {

	// Constants
	public static final JavaType CLASS = new JavaType("java.lang.Class");
	public static final JavaType OBJECT = new JavaType("java.lang.Object");
	public static final JavaType STRING = new JavaType("java.lang.String");
	public static final JavaType BOOLEAN_OBJECT = new JavaType("java.lang.Boolean");
	public static final JavaType CHAR_OBJECT = new JavaType("java.lang.Character");
	public static final JavaType BYTE_OBJECT = new JavaType("java.lang.Byte");
	public static final JavaType SHORT_OBJECT = new JavaType("java.lang.Short");
	public static final JavaType INT_OBJECT = new JavaType("java.lang.Integer");
	public static final JavaType LONG_OBJECT = new JavaType("java.lang.Long");
	public static final JavaType FLOAT_OBJECT = new JavaType("java.lang.Float");
	public static final JavaType DOUBLE_OBJECT = new JavaType("java.lang.Double");
	public static final JavaType VOID_OBJECT = new JavaType("java.lang.Void");
	public static final JavaType BOOLEAN_PRIMITIVE = new JavaType("java.lang.Boolean", 0, DataType.PRIMITIVE, null, null);
	public static final JavaType CHAR_PRIMITIVE = new JavaType("java.lang.Character", 0, DataType.PRIMITIVE, null, null);
	public static final JavaType BYTE_PRIMITIVE = new JavaType("java.lang.Byte", 0, DataType.PRIMITIVE, null, null);
	public static final JavaType BYTE_ARRAY_PRIMITIVE = new JavaType("java.lang.Byte", 1, DataType.PRIMITIVE, null, null);
	public static final JavaType SHORT_PRIMITIVE = new JavaType("java.lang.Short", 0, DataType.PRIMITIVE, null, null);
	public static final JavaType INT_PRIMITIVE = new JavaType("java.lang.Integer", 0, DataType.PRIMITIVE, null, null);
	public static final JavaType LONG_PRIMITIVE = new JavaType("java.lang.Long", 0, DataType.PRIMITIVE, null, null);
	public static final JavaType FLOAT_PRIMITIVE = new JavaType("java.lang.Float", 0, DataType.PRIMITIVE, null, null);
	public static final JavaType DOUBLE_PRIMITIVE = new JavaType("java.lang.Double", 0, DataType.PRIMITIVE, null, null);
	public static final JavaType VOID_PRIMITIVE = new JavaType("java.lang.Void", 0, DataType.PRIMITIVE, null, null);

	/**
	 * @deprecated use {@link #STRING} instead
	 */
	@Deprecated
	public static final JavaType STRING_OBJECT = STRING;

	// Used for wildcard type parameters; it must be one or the other
	public static final JavaSymbolName WILDCARD_EXTENDS = new JavaSymbolName("_ROO_WILDCARD_EXTENDS_"); // List<? extends YY>
	public static final JavaSymbolName WILDCARD_SUPER = new JavaSymbolName("_ROO_WILDCARD_SUPER_"); // List<? super XXXX>
	public static final JavaSymbolName WILDCARD_NEITHER = new JavaSymbolName("_ROO_WILDCARD_NEITHER_"); // List<?>

	// The fully-qualified names of common collection types
	private static final Set<String> COMMON_COLLECTION_TYPES = new HashSet<String>();

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
	 * Returns a {@link JavaType} for a list of the given element type
	 *
	 * @param elementType the type of element in the list (required)
	 * @return a non-<code>null</code> type
	 * @since 1.2.0
	 */
	public static JavaType listOf(final JavaType elementType) {
		return new JavaType(List.class.getName(), 0, DataType.TYPE, null, Arrays.asList(elementType));
	}

	/**
	 * Factory method for a {@link JavaType} with full details. Recall that
	 * {@link JavaType} is immutable and therefore this is the only way of
	 * setting these non-default values.
	 *
	 * This is a factory method rather than a constructor so as not to cause
	 * ambiguity problems for existing callers of {@link #JavaType(String, int, DataType, JavaSymbolName, List)}
	 *
	 * @param fullyQualifiedTypeName the name (as per the rules above)
	 * @param arrayDimensions the number of array dimensions (0 = not an array, 1 = one-dimensional array, etc.)
	 * @param dataType the {@link DataType} (required)
	 * @param argName the type argument name to this particular Java type (can be null if unassigned)
	 * @param parameters the type parameters applicable (can be null if there aren't any)
	 * @return a JavaType instance constructed based on the passed in details
	 * @since 1.2.0
	 */
	public static JavaType getInstance(final String fullyQualifiedTypeName, final int arrayDimensions, final DataType dataType, final JavaSymbolName argName, final JavaType... parameters) {
		return new JavaType(fullyQualifiedTypeName, arrayDimensions, dataType, argName, Arrays.asList(parameters));
	}

	/**
	 * Factory method for a {@link JavaType} with full details. Recall that
	 * {@link JavaType} is immutable and therefore this is the only way of
	 * setting these non-default values.
	 *
	 * This is a factory method rather than a constructor so as not to cause
	 * ambiguity problems for existing callers of {@link #JavaType(String, int, DataType, JavaSymbolName, List)}
	 *
	 * @param fullyQualifiedTypeName the name (as per the rules above)
	 * @param enclosingType the type's enclosing type
	 * @param arrayDimensions the number of array dimensions (0 = not an array, 1 = one-dimensional array, etc.)
	 * @param dataType the {@link DataType} (required)
	 * @param argName the type argument name to this particular Java type (can be null if unassigned)
	 * @param parameters the type parameters applicable (can be null if there aren't any)
	 * @return a JavaType instance constructed based on the passed in details
	 * @since 1.2.0
	 */
	public static JavaType getInstance(final String fullyQualifiedTypeName, final JavaType enclosingType, final int arrayDimensions, final DataType dataType, final JavaSymbolName argName, final JavaType... parameters) {
		return new JavaType(fullyQualifiedTypeName, enclosingType, arrayDimensions, dataType, argName, Arrays.asList(parameters));
	}

	// Fields
	private final boolean defaultPackage;
	private final int arrayDimensions;
	private final DataType dataType;
	private final JavaSymbolName argName;
	private final List<JavaType> parameters;
	private final String fullyQualifiedTypeName;
	private final String simpleTypeName;
	private final JavaType enclosingType;

	/**
	 * Constructs a {@link JavaType}.
	 * <p>
	 * The fully qualified type name will be enforced as follows:
	 * <ul>
	 * <li>The rules listed in {@link JavaSymbolName#assertJavaNameLegal(String)}
	 * <li>First letter of simple type name must be upper-case</li>
	 * </ul>
	 * <p>
	 * A fully qualified type name may include or exclude a package designator.
	 *
	 * @param fullyQualifiedTypeName the name (as per the above rules; mandatory)
	 */
	public JavaType(final String fullyQualifiedTypeName) {
		this(fullyQualifiedTypeName, 0, DataType.TYPE, null, null);
	}

	/**
	 * Constructs a {@link JavaType}.
	 * <p>
	 * The fully qualified type name will be enforced as follows:
	 * <ul>
	 * <li>The rules listed in {@link JavaSymbolName#assertJavaNameLegal(String)}
	 * <li>First letter of simple type name must be upper-case</li>
	 * </ul>
	 * <p>
	 * A fully qualified type name may include or exclude a package designator.
	 *
	 * @param fullyQualifiedTypeName the name (as per the above rules; mandatory)
	 * @param enclosingType the type's enclosing type
	 */
	public JavaType(final String fullyQualifiedTypeName, final JavaType enclosingType) {
		this(fullyQualifiedTypeName, enclosingType, 0, DataType.TYPE, null, null);
	}

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
	 * Construct a {@link JavaType} with full details. Recall that {@link JavaType} is immutable and therefore this is the only way of
	 * setting these non-default values.
	 *
	 * @param fullyQualifiedTypeName the name (as per the rules above)
	 * @param arrayDimensions the number of array dimensions (0 = not an array, 1 = one-dimensional array, etc.)
	 * @param dataType the {@link DataType} (required)
	 * @param argName the type argument name to this particular Java type (can be null if unassigned)
	 * @param parameters the type parameters applicable (can be null if there aren't any)
	 */
	public JavaType(final String fullyQualifiedTypeName, final int arrayDimensions, final DataType dataType, final JavaSymbolName argName, final List<JavaType> parameters) {
		this(fullyQualifiedTypeName, null, arrayDimensions, dataType, argName, parameters);
	}

	/**
	 * Construct a {@link JavaType} with full details. Recall that {@link JavaType} is immutable and therefore this is the only way of
	 * setting these non-default values.
	 *
	 * @param fullyQualifiedTypeName the name (as per the rules above)
	 * @param enclosingType the type's enclosing type
	 * @param arrayDimensions the number of array dimensions (0 = not an array, 1 = one-dimensional array, etc.)
	 * @param dataType the {@link DataType} (required)
	 * @param argName the type argument name to this particular Java type (can be null if unassigned)
	 * @param parameters the type parameters applicable (can be null if there aren't any)
	 */
	public JavaType(final String fullyQualifiedTypeName, final JavaType enclosingType, final int arrayDimensions, final DataType dataType, final JavaSymbolName argName, final List<JavaType> parameters) {
		Assert.hasText(fullyQualifiedTypeName, "Fully qualified type name required");
		Assert.notNull(dataType, "Data type required");
		JavaSymbolName.assertJavaNameLegal(fullyQualifiedTypeName);
		this.argName = argName;
		this.arrayDimensions = arrayDimensions;
		this.dataType = dataType;
		this.fullyQualifiedTypeName = fullyQualifiedTypeName;
		this.defaultPackage = !fullyQualifiedTypeName.contains(".");
		if (enclosingType == null) {
			this.enclosingType = determineEnclosingType();
		} else {
			this.enclosingType = enclosingType;
		}
		if (defaultPackage) {
			this.simpleTypeName = fullyQualifiedTypeName;
		} else {
			final int offset = fullyQualifiedTypeName.lastIndexOf(".");
			this.simpleTypeName = fullyQualifiedTypeName.substring(offset + 1);
		}

		this.parameters = new ArrayList<JavaType>();
		if (parameters != null) {
			this.parameters.addAll(parameters);
		}
	}

	/**
	 * @return the name (does not contain any periods; never null or empty)
	 */
	public String getSimpleTypeName() {
		return simpleTypeName;
	}

	/**
	 * @return the fully qualified name (complies with the rules specified in the constructor)
	 */
	public String getFullyQualifiedTypeName() {
		return fullyQualifiedTypeName;
	}

	/**
	 * Obtains the name of this type, including type parameters. It will be formatted in a manner compatible with non-static use.
	 * No type name import resolution will take place. This is a side-effect free method.
	 *
	 * @return the type name, including parameters, as legal Java code (never null or empty)
	 */
	public String getNameIncludingTypeParameters() {
		return getNameIncludingTypeParameters(false, null, new HashMap<String, String>());
	}

	/**
	 * Obtains the name of this type, including type parameters. It will be formatted in a manner compatible with either static
	 * or non-static usage, as per the passed argument. Type names will attempt to be resolved (and automatically registered)
	 * using the passed resolver. This method will have side-effects on the passed resolver.
	 *
	 * @param staticForm true if the output should be compatible with static use
	 * @param resolver the resolver to use (may be null in which case no import resolution will occur)
	 * @return the type name, including parameters, as legal Java code (never null or empty)
	 */
	public String getNameIncludingTypeParameters(final boolean staticForm, final ImportRegistrationResolver resolver) {
		return getNameIncludingTypeParameters(staticForm, resolver, new HashMap<String, String>());
	}

	private String getNameIncludingTypeParameters(final boolean staticForm, final ImportRegistrationResolver resolver, final Map<String, String> types) {
		if (DataType.PRIMITIVE == dataType) {
			Assert.isTrue(parameters.isEmpty(), "A primitive cannot have parameters");
			if (this.fullyQualifiedTypeName.equals(Integer.class.getName())) {
				return "int" + getArraySuffix();
			} else if (this.fullyQualifiedTypeName.equals(Character.class.getName())) {
				return "char" + getArraySuffix();
			} else if (this.fullyQualifiedTypeName.equals(Void.class.getName())) {
				return "void";
			}
			return StringUtils.uncapitalize(this.getSimpleTypeName() + getArraySuffix());
		}

		StringBuilder sb = new StringBuilder();

		if (WILDCARD_EXTENDS.equals(argName)) {
			sb.append("?");
			if (dataType == DataType.TYPE || !staticForm) {
				sb.append(" extends ");
			} else if (types.containsKey(fullyQualifiedTypeName)) {
				sb.append(" extends ").append(types.get(fullyQualifiedTypeName));
			}
		} else if (WILDCARD_SUPER.equals(argName)) {
			sb.append("?");
			if (dataType == DataType.TYPE || !staticForm) {
				sb.append(" super ");
			} else if (types.containsKey(fullyQualifiedTypeName)) {
				sb.append(" extends ").append(types.get(fullyQualifiedTypeName));
			}
		} else if (WILDCARD_NEITHER.equals(argName)) {
			sb.append("?");
		} else if (argName != null && !staticForm) {
			sb.append(argName);
			if (dataType == DataType.TYPE) {
				if (!fullyQualifiedTypeName.equals(OBJECT.getFullyQualifiedTypeName())) {
					sb.append(" extends ");
				}
			}
		}

		if (!WILDCARD_NEITHER.equals(argName)) {
			// It wasn't a WILDCARD_NEITHER, so we might need to continue with more details
			if (dataType == DataType.TYPE || !staticForm) {
				if (resolver != null) {
					if (resolver.isFullyQualifiedFormRequiredAfterAutoImport(this)) {
						sb.append(fullyQualifiedTypeName);
					} else {
						sb.append(getSimpleTypeName());
					}
				} else {
					if (fullyQualifiedTypeName.equals(OBJECT.getFullyQualifiedTypeName())) {
						// It's Object, so we need to only append if this isn't a type arg
						if (argName == null) {
							sb.append(fullyQualifiedTypeName);
						}
					} else {
						// It's ok to just append it as it's not Object
						sb.append(fullyQualifiedTypeName);
					}
				}
			}

			if (this.parameters.size() > 0 && (dataType == DataType.TYPE || !staticForm)) {
				sb.append("<");
				int counter = 0;
				for (JavaType param : this.parameters) {
					counter++;
					if (counter > 1) {
						sb.append(", ");
					}
					sb.append(param.getNameIncludingTypeParameters(staticForm, resolver, types));
					counter++;
				}
				sb.append(">");
			}

			sb.append(getArraySuffix());
		}

		if (argName != null && !argName.equals(WILDCARD_EXTENDS) && !argName.equals(WILDCARD_SUPER) && !argName.equals(WILDCARD_NEITHER)) {
			types.put(this.argName.getSymbolName(), sb.toString());
		}

		return sb.toString();
	}

	/**
	 * @return the package name (never null)
	 */
	public JavaPackage getPackage() {
		if (isDefaultPackage() && !Character.isUpperCase(fullyQualifiedTypeName.charAt(0))) {
			return new JavaPackage("");
		}

		if (enclosingType != null) {
			String enclosingTypeFullyQualifiedTypeName = enclosingType.getFullyQualifiedTypeName();
			int offset = enclosingTypeFullyQualifiedTypeName.lastIndexOf(".");
			// Handle case where the package name after the last period starts with a capital letter.
			if (offset > -1 && Character.isUpperCase(enclosingTypeFullyQualifiedTypeName.charAt(offset + 1))) {
				return new JavaPackage(enclosingTypeFullyQualifiedTypeName);
			}
			return enclosingType.getPackage();
		}

		int offset = fullyQualifiedTypeName.lastIndexOf(".");
		return offset == -1 ? new JavaPackage("") : new JavaPackage(fullyQualifiedTypeName.substring(0, offset));
	}

	/**
	 * @return the enclosing type, if any (will return null if there is no enclosing type)
	 */
	public JavaType getEnclosingType() {
		return enclosingType;
	}

	public boolean isDefaultPackage() {
		return defaultPackage;
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

	public boolean isCommonCollectionType() {
		return COMMON_COLLECTION_TYPES.contains(this.fullyQualifiedTypeName);
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

	public List<JavaType> getParameters() {
		return Collections.unmodifiableList(this.parameters);
	}

	public boolean isArray() {
		return arrayDimensions > 0;
	}

	public int getArray() {
		return arrayDimensions;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fullyQualifiedTypeName == null) ? 0 : fullyQualifiedTypeName.hashCode());
		result = prime * result + ((dataType == null) ? 0 : dataType.hashCode());
		result = prime * result + arrayDimensions;
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		// NB: Not using the normal convention of delegating to compareTo (for efficiency reasons)
		return obj != null
			&& obj instanceof JavaType
			&& this.fullyQualifiedTypeName.equals(((JavaType) obj).getFullyQualifiedTypeName())
			&& this.dataType == ((JavaType) obj).getDataType()
			&& this.arrayDimensions == ((JavaType) obj).getArray()
			&& ((JavaType) obj).getParameters().containsAll(this.parameters);
	}

	public int compareTo(final JavaType o) {
		// NB: If adding more fields to this class ensure the equals(Object) method is updated accordingly
		if (o == null) return -1;
		if (equals(o)) return 0;
		return toString().compareTo(o.toString());
	}

	@Override
	public String toString() {
		return getNameIncludingTypeParameters();
	}

	public JavaSymbolName getArgName() {
		return argName;
	}

	public DataType getDataType() {
		return dataType;
	}

	private String getArraySuffix() {
		if (arrayDimensions == 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < arrayDimensions; i++) {
			sb.append("[]");
		}
		return sb.toString();
	}

	private JavaType determineEnclosingType() {
		int offset = fullyQualifiedTypeName.lastIndexOf(".");
		if (offset == -1) {
			// There is no dot in the name, so there's no way there's an enclosing type
			return null;
		}
		String possibleName = fullyQualifiedTypeName.substring(0, offset);
		int offset2 = possibleName.lastIndexOf(".");

		// Start by handling if the type name is Foo.Bar (ie an enclosed type within the default package)
		String enclosedWithinPackage = null;
		String enclosedWithinTypeName = possibleName;

		// Handle the probability the type name is within a package like com.alpha.Foo.Bar
		if (offset2 > -1) {
			enclosedWithinPackage = possibleName.substring(0, offset2);
			enclosedWithinTypeName = possibleName.substring(offset2 + 1);
		}

		if (Character.isUpperCase(enclosedWithinTypeName.charAt(0))) {
			// First letter is upper-case, so treat it as a type name for now
			String preTypeNamePortion = enclosedWithinPackage == null ? "" : (enclosedWithinPackage + ".");
			return new JavaType(preTypeNamePortion + enclosedWithinTypeName);
		}

		return null;
	}
}
