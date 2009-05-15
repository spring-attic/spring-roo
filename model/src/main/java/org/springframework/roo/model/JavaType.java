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
	private boolean array = false;
	private boolean primitive = false;
	private String fullyQualifiedTypeName;
	public static final JavaType BOOLEAN_PRIMITIVE = new JavaType("java.lang.Boolean", false, true, null);
	public static final JavaType CHAR_PRIMITIVE = new JavaType("java.lang.Character", false, true, null);
	public static final JavaType BYTE_PRIMITIVE = new JavaType("java.lang.Byte", false, true, null);
	public static final JavaType SHORT_PRIMITIVE = new JavaType("java.lang.Short", false, true, null);
	public static final JavaType INT_PRIMITIVE = new JavaType("java.lang.Integer", false, true, null);
	public static final JavaType LONG_PRIMITIVE = new JavaType("java.lang.Long", false, true, null);
	public static final JavaType FLOAT_PRIMITIVE = new JavaType("java.lang.Float", false, true, null);
	public static final JavaType DOUBLE_PRIMITIVE = new JavaType("java.lang.Double", false, true, null);
	public static final JavaType VOID_PRIMITIVE = new JavaType("java.lang.Void", false, true, null);

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
		Assert.hasText(fullyQualifiedTypeName, "Fully qualified type name required");
		JavaSymbolName.assertJavaNameLegal(fullyQualifiedTypeName);
		this.fullyQualifiedTypeName = fullyQualifiedTypeName;
		String firstChar = getSimpleTypeName().substring(0,1);
		Assert.isTrue(firstChar.toUpperCase().equals(firstChar), "The first letter of the type name portion must be uppercase (attempted '" + fullyQualifiedTypeName + "')");
	}

	/**
	 * Construct a {@link JavaType} with full details. Recall that {@link JavaType} is immutable and therefore this is the only way of
	 * setting these non-default values.
	 * 
	 * @param fullyQualifiedTypeName the name (as per the rules above)
	 * @param array whether this type is an array
	 * @param primitive whether this type is representing the equivalent primitive
	 * @param parameters the type parameters applicable (can be null if there aren't any)
	 */
	public JavaType(String fullyQualifiedTypeName, boolean array, boolean primitive, List<JavaType> parameters) {
		Assert.hasText(fullyQualifiedTypeName, "Fully qualified type name required");
		JavaSymbolName.assertJavaNameLegal(fullyQualifiedTypeName);
		this.fullyQualifiedTypeName = fullyQualifiedTypeName;
		String firstChar = getSimpleTypeName().substring(0,1);
		Assert.isTrue(firstChar.toUpperCase().equals(firstChar), "The first letter of the type name portion must be uppercase");
		this.array = array;
		this.primitive = primitive;
		if (parameters != null) {
			this.parameters = parameters;
		}
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

	/**
	 * @return the fully qualified name, including fully-qualified name of
	 * each type parameter
	 */
	public String getFullyQualifiedTypeNameIncludingTypeParameters() {
		StringBuilder sb = new StringBuilder();
		if (primitive) {
			Assert.isTrue(parameters.size() == 0, "A primitive cannot have parameters");
			if (this.fullyQualifiedTypeName.equals(Integer.class.getName())) {
				return "int";
			} else if (this.fullyQualifiedTypeName.equals(Character.class.getName())) {
					return "char";
			} else if (this.fullyQualifiedTypeName.equals(Void.class.getName())) {
				return "void";
			}
			return StringUtils.uncapitalize(this.getSimpleTypeName());
		}
		sb.append(fullyQualifiedTypeName);
		if (array) {
			sb.append("[]");
		}
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
		return array;
	}

	public final int compareTo(JavaType o) {
		if (o == null) return -1;
		return this.fullyQualifiedTypeName.compareTo(o.fullyQualifiedTypeName);
	}
	
	public final String toString() {
		return getFullyQualifiedTypeNameIncludingTypeParameters();
	}

// Shouldn't be required given JavaType is immutable!
//	@Override
//	public JavaType clone() throws CloneNotSupportedException {
//		return new JavaType(this.fullyQualifiedTypeName, this.array, this.primitive);
//	}
	
}
