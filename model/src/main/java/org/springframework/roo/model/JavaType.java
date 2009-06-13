package org.springframework.roo.model;

import java.util.ArrayList;
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
 * Immutable representation of a Java type.
 * 
 * <p>
 * Note that a Java type can be contained within a package, but a package is not a type.
 * 
 * <p>
 * This class is used whenever a formal reference to a Java type is required.
 * It provides convenient ways to determine the type's simple name and package name.
 * A related {@link Converter} is also offered.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public final class JavaType implements Comparable<JavaType>, Cloneable {
	private List<JavaType> parameters = new ArrayList<JavaType>();
	private JavaSymbolName argName = null;
	private int array = 0;
	private boolean primitive = false;
	private String fullyQualifiedTypeName;
	public static final JavaType BOOLEAN_OBJECT = new JavaType("java.lang.Boolean", 0, false, null, null);
	public static final JavaType CHAR_OBJECT = new JavaType("java.lang.Character", 0, false, null, null);
	public static final JavaType BYTE_OBJECT = new JavaType("java.lang.Byte", 0, false, null, null);
	public static final JavaType SHORT_OBJECT = new JavaType("java.lang.Short", 0, false, null, null);
	public static final JavaType INT_OBJECT = new JavaType("java.lang.Integer", 0, false, null, null);
	public static final JavaType LONG_OBJECT = new JavaType("java.lang.Long", 0, false, null, null);
	public static final JavaType FLOAT_OBJECT = new JavaType("java.lang.Float", 0, false, null, null);
	public static final JavaType DOUBLE_OBJECT = new JavaType("java.lang.Double", 0, false, null, null);
	public static final JavaType VOID_OBJECT = new JavaType("java.lang.Void", 0, false, null, null);
	public static final JavaType BOOLEAN_PRIMITIVE = new JavaType("java.lang.Boolean", 0, true, null, null);
	public static final JavaType CHAR_PRIMITIVE = new JavaType("java.lang.Character", 0, true, null, null);
	public static final JavaType BYTE_PRIMITIVE = new JavaType("java.lang.Byte", 0, true, null, null);
	public static final JavaType SHORT_PRIMITIVE = new JavaType("java.lang.Short", 0, true, null, null);
	public static final JavaType INT_PRIMITIVE = new JavaType("java.lang.Integer", 0, true, null, null);
	public static final JavaType LONG_PRIMITIVE = new JavaType("java.lang.Long", 0, true, null, null);
	public static final JavaType FLOAT_PRIMITIVE = new JavaType("java.lang.Float", 0, true, null, null);
	public static final JavaType DOUBLE_PRIMITIVE = new JavaType("java.lang.Double", 0, true, null, null);
	public static final JavaType VOID_PRIMITIVE = new JavaType("java.lang.Void", 0, true, null, null);

	private static final Set<String> commonCollectionTypes = new HashSet<String>();

	static {
		commonCollectionTypes.add(Collection.class.getName());
		commonCollectionTypes.add(List.class.getName());
		commonCollectionTypes.add(Set.class.getName());
		commonCollectionTypes.add(Map.class.getName());
		commonCollectionTypes.add(HashMap.class.getName());
		commonCollectionTypes.add(TreeMap.class.getName());
		commonCollectionTypes.add(ArrayList.class.getName());
		commonCollectionTypes.add(Vector.class.getName());
		commonCollectionTypes.add(HashSet.class.getName());
	}

	/**
	 * Construct a JavaType.
	 * 
	 * <p>
	 * The fully qualified type name will be enforced as follows:
	 * 
	 * <ul>
	 * <li>The rules listed in {link {@link JavaTypeUtils#assertJavaNameLegal(String)}}
	 * <li>First letter of simple type name must be uppercase</li>
	 * </ul>
	 * 
	 * <p>
	 * A fully qualified type name may include or exclude a package designator.
	 * 
	 * @param fullyQualifiedTypeName the name (as per the above rules; mandatory)
	 */
	public JavaType(String fullyQualifiedTypeName) {
		this(fullyQualifiedTypeName, 0, false, null, null);
	}

	/**
	 * Construct a {@link JavaType} with full details. Recall that {@link JavaType} is immutable and therefore this is the only way of
	 * setting these non-default values.
	 * 
	 * @param fullyQualifiedTypeName the name (as per the rules above)
	 * @param array number of array indicies (0 = not an array, 1 = single dimensional array etc)
	 * @param primitive whether this type is representing the equivalent primitive
	 * @param argName the type argument name to this particular Java type (can be null if unassigned)
	 * @param parameters the type parameters applicable (can be null if there aren't any)
	 */
	public JavaType(String fullyQualifiedTypeName, int array, boolean primitive, JavaSymbolName argName, List<JavaType> parameters) {
		if (fullyQualifiedTypeName == null || fullyQualifiedTypeName.length() == 0) {
			throw new IllegalArgumentException("Fully qualified type name required");
		}
		JavaSymbolName.assertJavaNameLegal(fullyQualifiedTypeName);
		this.fullyQualifiedTypeName = fullyQualifiedTypeName;
		if (!Character.isUpperCase(getSimpleTypeName().charAt(0))) {
			throw new IllegalArgumentException("The first letter of the type name portion must be uppercase (attempted '" + fullyQualifiedTypeName + "')");
		}
		
		this.array = array;
		this.primitive = primitive;
		if (parameters != null) {
			this.parameters = parameters;
		}
		this.argName = argName;
	}

	/**
	 * @return the name (does not contain any periods; never null or empty)
	 */
	public String getSimpleTypeName() {
		if (isDefaultPackage()) {
			return fullyQualifiedTypeName;
		}
		int offset = fullyQualifiedTypeName.lastIndexOf(".");
		return fullyQualifiedTypeName.substring(offset+1);
	}

	/**
	 * @return the fully qualified name (complies with the rules specified in the constructor)
	 */
	public String getFullyQualifiedTypeName() {
		return fullyQualifiedTypeName;
	}

	// used for wildcard type parameters; it must be one or the other
	public static final JavaSymbolName WILDCARD_NEITHER = new JavaSymbolName("_ROO_WILDCARD_NEITHER_");
	public static final JavaSymbolName WILDCARD_EXTENDS = new JavaSymbolName("_ROO_WILDCARD_EXTENDS_");
	public static final JavaSymbolName WILDCARD_SUPER = new JavaSymbolName("_ROO_WILDCARD_SUPER_");

	public String getFullyQualifiedTypeNameIncludingTypeParameterNames() {
		StringBuilder sb = new StringBuilder();
		if (primitive) {
			Assert.isTrue(parameters.size() == 0, "A primitive cannot have parameters");
			if (this.fullyQualifiedTypeName.equals(Integer.class.getName())) {
				return "int" + getArraySuffix();
			} else if (this.fullyQualifiedTypeName.equals(Character.class.getName())) {
					return "char" + getArraySuffix();
			} else if (this.fullyQualifiedTypeName.equals(Void.class.getName())) {
				return "void";
			}
			return StringUtils.uncapitalize(this.getSimpleTypeName() + getArraySuffix());
		}
		if (WILDCARD_EXTENDS.equals(argName)) {
			sb.append("? extends ").append(fullyQualifiedTypeName);
		} else if (WILDCARD_SUPER.equals(argName)) {
			sb.append("? super ").append(fullyQualifiedTypeName);
		} else if (WILDCARD_NEITHER.equals(argName)) {
			sb.append("?");
		} else {
			if (argName == null) {
				sb.append(fullyQualifiedTypeName);
			} else {
				sb.append(argName);
			}
		}
		if (this.parameters.size() > 0 && argName == null) {
			sb.append("<");
			int counter = 0;
			for (JavaType param : this.parameters) {
				counter++;
				if (counter > 1) {
					sb.append(", ");
				}
				sb.append(param.getFullyQualifiedTypeNameIncludingTypeParameterNames());
				counter++;
			}
			sb.append(">");
		}
		sb.append(getArraySuffix());
		return sb.toString();
	}
	
	/**
	 * @return the fully qualified name, including fully-qualified name of
	 * each type parameter
	 */
	public String getFullyQualifiedTypeNameIncludingTypeParameters() {
		StringBuilder sb = new StringBuilder();
		if (primitive) {
			Assert.isTrue(parameters.size() == 0, "A primitive cannot have parameters");
			if (this.fullyQualifiedTypeName.equals(Integer.class.getName())) {
				return "int" + getArraySuffix();
			} else if (this.fullyQualifiedTypeName.equals(Character.class.getName())) {
					return "char" + getArraySuffix();
			} else if (this.fullyQualifiedTypeName.equals(Void.class.getName())) {
				return "void";
			}
			return StringUtils.uncapitalize(this.getSimpleTypeName() + getArraySuffix());
		}
		if (argName != null) {
			if (WILDCARD_EXTENDS.equals(argName)) {
				sb.append("? extends ");
			} else if (WILDCARD_SUPER.equals(argName)) {
				sb.append("? super ");
			} else if (WILDCARD_NEITHER.equals(argName)) {
				sb.append("?");
			} else {
				sb.append(argName).append(" extends ");
			}
		}
		if (!WILDCARD_NEITHER.equals(argName)) {
			sb.append(fullyQualifiedTypeName);
			if (this.parameters.size() > 0) {
				sb.append("<");
				int counter = 0;
				for (JavaType param : this.parameters) {
					counter++;
					if (counter > 1) {
						sb.append(", ");
					}
					sb.append(param.getFullyQualifiedTypeNameIncludingTypeParameters());
					counter++;
				}
				sb.append(">");
			}
			sb.append(getArraySuffix());
		}
		return sb.toString();
	}
	
	/**
	 * @return the package name (never null)
	 */
	public JavaPackage getPackage() {
		if (isDefaultPackage()) {
			return new JavaPackage("");
		}
		int offset = fullyQualifiedTypeName.lastIndexOf(".");
		return new JavaPackage(fullyQualifiedTypeName.substring(0, offset));
	}
	
	public boolean isDefaultPackage() {
		return !fullyQualifiedTypeName.contains(".");
	}
	
	public final int hashCode() {
		return this.fullyQualifiedTypeName.hashCode();
	}

	public final boolean equals(Object obj) {
		return obj != null && obj instanceof JavaType && this.compareTo((JavaType)obj) == 0;
	}
	
	public boolean isPrimitive() {
		return primitive;
	}

	public boolean isCommonCollectionType() {
		return commonCollectionTypes.contains(this.fullyQualifiedTypeName);
	}

	public List<JavaType> getParameters() {
		return Collections.unmodifiableList(this.parameters);
	}
	
	public boolean isArray() {
		return array > 0;
	}
	
	public int getArray() {
		return array;
	}

	private String getArraySuffix() {
		if (array == 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < array; i++) {
			sb.append("[]");
		}
		return sb.toString();
	}

	public final int compareTo(JavaType o) {
		if (o == null) return -1;
		return this.fullyQualifiedTypeName.compareTo(o.fullyQualifiedTypeName);
	}
	
	public final String toString() {
		return getFullyQualifiedTypeNameIncludingTypeParameters();
	}

	public JavaSymbolName getArgName() {
		return argName;
	}

// Shouldn't be required given JavaType is immutable!
//	@Override
//	public JavaType clone() throws CloneNotSupportedException {
//		return new JavaType(this.fullyQualifiedTypeName, this.array, this.primitive);
//	}
	
}
