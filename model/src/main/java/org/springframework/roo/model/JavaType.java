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
	private boolean defaultPackage;
	private DataType dataType;
	private String fullyQualifiedTypeName;
	private String simpleTypeName;
	public static final JavaType BOOLEAN_OBJECT = new JavaType("java.lang.Boolean", 0, DataType.TYPE, null, null);
	public static final JavaType CHAR_OBJECT = new JavaType("java.lang.Character", 0, DataType.TYPE, null, null);
	public static final JavaType STRING_OBJECT = new JavaType("java.lang.String", 0, DataType.TYPE, null, null);
	public static final JavaType BYTE_OBJECT = new JavaType("java.lang.Byte", 0, DataType.TYPE, null, null);
	public static final JavaType SHORT_OBJECT = new JavaType("java.lang.Short", 0, DataType.TYPE, null, null);
	public static final JavaType INT_OBJECT = new JavaType("java.lang.Integer", 0, DataType.TYPE, null, null);
	public static final JavaType LONG_OBJECT = new JavaType("java.lang.Long", 0, DataType.TYPE, null, null);
	public static final JavaType FLOAT_OBJECT = new JavaType("java.lang.Float", 0, DataType.TYPE, null, null);
	public static final JavaType DOUBLE_OBJECT = new JavaType("java.lang.Double", 0, DataType.TYPE, null, null);
	public static final JavaType VOID_OBJECT = new JavaType("java.lang.Void", 0, DataType.TYPE, null, null);
	public static final JavaType BOOLEAN_PRIMITIVE = new JavaType("java.lang.Boolean", 0, DataType.PRIMITIVE, null, null);
	public static final JavaType CHAR_PRIMITIVE = new JavaType("java.lang.Character", 0, DataType.PRIMITIVE, null, null);
	public static final JavaType BYTE_PRIMITIVE = new JavaType("java.lang.Byte", 0, DataType.PRIMITIVE, null, null);
	public static final JavaType SHORT_PRIMITIVE = new JavaType("java.lang.Short", 0, DataType.PRIMITIVE, null, null);
	public static final JavaType INT_PRIMITIVE = new JavaType("java.lang.Integer", 0, DataType.PRIMITIVE, null, null);
	public static final JavaType LONG_PRIMITIVE = new JavaType("java.lang.Long", 0, DataType.PRIMITIVE, null, null);
	public static final JavaType FLOAT_PRIMITIVE = new JavaType("java.lang.Float", 0, DataType.PRIMITIVE, null, null);
	public static final JavaType DOUBLE_PRIMITIVE = new JavaType("java.lang.Double", 0, DataType.PRIMITIVE, null, null);
	public static final JavaType VOID_PRIMITIVE = new JavaType("java.lang.Void", 0, DataType.PRIMITIVE, null, null);

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
		this(fullyQualifiedTypeName, 0, DataType.TYPE, null, null);
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
	public JavaType(String fullyQualifiedTypeName, int array, DataType primitive, JavaSymbolName argName, List<JavaType> parameters) {
		if (fullyQualifiedTypeName == null || fullyQualifiedTypeName.length() == 0) {
			throw new IllegalArgumentException("Fully qualified type name required");
		}
		JavaSymbolName.assertJavaNameLegal(fullyQualifiedTypeName);
		this.fullyQualifiedTypeName = fullyQualifiedTypeName;
		this.defaultPackage = !fullyQualifiedTypeName.contains(".");
		if (defaultPackage) {
			simpleTypeName = fullyQualifiedTypeName;
		} else {
			int offset = fullyQualifiedTypeName.lastIndexOf(".");
			simpleTypeName = fullyQualifiedTypeName.substring(offset+1);
		}
		if (!Character.isUpperCase(simpleTypeName.charAt(0))) {
			throw new IllegalArgumentException("The first letter of the type name portion must be uppercase (attempted '" + fullyQualifiedTypeName + "')");
		}
		
		this.array = array;
		this.dataType = primitive;
		if (parameters != null) {
			this.parameters = parameters;
		}
		this.argName = argName;
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

	// used for wildcard type parameters; it must be one or the other
	public static final JavaSymbolName WILDCARD_EXTENDS = new JavaSymbolName("_ROO_WILDCARD_EXTENDS_");  // List<? extends YY>
	public static final JavaSymbolName WILDCARD_SUPER = new JavaSymbolName("_ROO_WILDCARD_SUPER_");      // List<? super XXXX>
	public static final JavaSymbolName WILDCARD_NEITHER = new JavaSymbolName("_ROO_WILDCARD_NEITHER_");  // List<?>

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
	public String getNameIncludingTypeParameters(boolean staticForm, ImportRegistrationResolver resolver) {
		return getNameIncludingTypeParameters(staticForm, resolver, new HashMap<String, String>());
	}
	
	private String getNameIncludingTypeParameters(boolean staticForm, ImportRegistrationResolver resolver, Map<String, String> types) {
		if (DataType.PRIMITIVE == dataType) {
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
				sb.append(" extends ");
			}
		}
		
		if (!WILDCARD_NEITHER.equals(argName)) {
			// It wasn't a WILDCARD_NEITHER, so we might need to continue with more details
			
			if (dataType == DataType.TYPE || !staticForm) {
				// TODO: Use the import registration resolver
				if (resolver != null) {
					if (resolver.isFullyQualifiedFormRequiredAfterAutoImport(this)) {
						sb.append(fullyQualifiedTypeName);
					} else {
						sb.append(getSimpleTypeName());
					}
				} else {
					sb.append(fullyQualifiedTypeName);
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
		if (isDefaultPackage()) {
			return new JavaPackage("");
		}
		int offset = fullyQualifiedTypeName.lastIndexOf(".");
		return new JavaPackage(fullyQualifiedTypeName.substring(0, offset));
	}
	
	public boolean isDefaultPackage() {
		return defaultPackage;
	}
	
	public final int hashCode() {
		return this.fullyQualifiedTypeName.hashCode();
	}

	public boolean isPrimitive() {
		return DataType.PRIMITIVE == dataType;
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

	public final boolean equals(Object obj) {
		// NB: Not using the normal convention of delegating to compareTo (for efficiency reasons)
		return obj != null && obj instanceof JavaType && this.fullyQualifiedTypeName.equals(((JavaType)obj).fullyQualifiedTypeName) && this.dataType == ((JavaType)obj).dataType;
	}

	public final int compareTo(JavaType o) {
		// NB: If adding more fields to this class ensure the equals(Object) method is updated accordingly 
		if (o == null) return -1;
		return this.fullyQualifiedTypeName.compareTo(o.fullyQualifiedTypeName);
	}
	
	public final String toString() {
		return getNameIncludingTypeParameters();
	}

	public JavaSymbolName getArgName() {
		return argName;
	}

	public DataType getDataType() {
		return dataType;
	}

	
// Shouldn't be required given JavaType is immutable!
//	@Override
//	public JavaType clone() throws CloneNotSupportedException {
//		return new JavaType(this.fullyQualifiedTypeName, this.array, this.primitive);
//	}
	
}
