package org.springframework.roo.classpath.details;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;

import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Provides utility methods for querying JavaBeans.
 * 
 * @author Ben Alex
 * @since 1.1.1
 */
public abstract class BeanInfoUtils {

	/**
	 * Obtains the property name for the specified JavaBean accessor or mutator method. This is determined by discarding the first 2 or 3 letters of the method name (depending whether it is a "get",
	 * "set" or "is" method). There is no special searching back to the actual field name.
	 * 
	 * @param methodMetadata to search (required, and must be a "get", "set" or "is" method)
	 * @return the name of the property (never returned null)
	 */
	public static JavaSymbolName getPropertyNameForJavaBeanMethod(MethodMetadata methodMetadata) {
		Assert.notNull(methodMetadata, "Method metadata is required");
		String name = methodMetadata.getMethodName().getSymbolName();
		if (name.startsWith("set") || name.startsWith("get")) {
			return new JavaSymbolName(name.substring(3));
		}
		if (name.startsWith("is")) {
			return new JavaSymbolName(name.substring(2));
		}
		throw new IllegalStateException("Method name '" + name + "' does not observe JavaBean method naming conventions");
	}
	
	/**
	 * Attempts to locate the field which is represented by the presented property name.
	 * 
	 * <p>
	 * Not every JavaBean getter or setter actually backs to a field with an identical name. In such cases, null will be returned.
	 * 
	 * @param memberDetails the member holders to scan (required)
	 * @param propertyName the property name (required)
	 * @return the field if found, or null if it could not be found
	 */
	public static FieldMetadata getFieldForPropertyName(MemberDetails memberDetails, JavaSymbolName propertyName) {
		Assert.notNull(propertyName, "Property name required");
		for (MemberHoldingTypeDetails holder : memberDetails.getDetails()) {
			FieldMetadata result = MemberFindingUtils.getDeclaredField(holder, propertyName);
			if (result != null) {
				return result;
			}
			// To get here means we couldn't find the property using the exact same case;
			// try to scan with a lowercase first character (see ROO-203)
			result = MemberFindingUtils.getDeclaredField(holder, new JavaSymbolName(StringUtils.uncapitalize(propertyName.getSymbolName())));
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Indicates if the presented method compiles with the JavaBean conventions around mutator methods (public, "set", 1 arg etc).
	 * 
	 * @param method to evaluate (required)
	 * @return true if the presented method is a mutator, otherwise false
	 */
	public static boolean isMutatorMethod(MethodMetadata method) {
		return method.getMethodName().getSymbolName().startsWith("set") && method.getParameterTypes().size() == 1 && Modifier.isPublic(method.getModifier());
	}

	/**
	 * Indicates if the presented method compiles with the JavaBean conventions around accessor methods (public, "set" or "is", 0 args etc).
	 * 
	 * @param method to evaluate (required)
	 * @return true if the presented method is an accessor, otherwise false
	 */
	public static boolean isAccessorMethod(MethodMetadata method) {
		return (method.getMethodName().getSymbolName().startsWith("get") || method.getMethodName().getSymbolName().startsWith("is")) && method.getParameterTypes().size() == 0 && Modifier.isPublic(method.getModifier());
	}
	
	/**
	 * Attempts to locate an accessor and a mutator method for a given field.
	 * 
	 * <p>
	 * Not every JavaBean getter or setter actually backs to a field with an identical name. In such cases, false will be returned.
	 * 
	 * @param field the member holders to scan (required)
	 * @param memberDetails the member details to scan
	 * @return true if an accessor and a mutator are present, or false otherwise
	 */
	public static boolean hasAccessorAndMutator(FieldMetadata field, MemberDetails memberDetails) {
		Assert.notNull(field, "Field metadata required");
		Assert.notNull(memberDetails, "Member details required");
		String capitalizedFieldName = StringUtils.capitalize(field.getFieldName().getSymbolName());

		if ((MemberFindingUtils.getMethod(memberDetails, new JavaSymbolName("get" + capitalizedFieldName), new ArrayList<JavaType>()) != null
				|| MemberFindingUtils.getMethod(memberDetails, new JavaSymbolName("is" + capitalizedFieldName), new ArrayList<JavaType>()) != null)
				&& MemberFindingUtils.getMethod(memberDetails, new JavaSymbolName("set" + capitalizedFieldName), Arrays.asList(field.getFieldType())) != null) {
			return true;
		}
	
		return false;
	}

	/**
	 * Determines whether the presented entity is a test class or not.
	 * 
	 * @param entity the type to test
	 * @return true if the entity is likely not a test class, otherwise false
	 */
	public static boolean isEntityReasonablyNamed(JavaType entity) {
		return !entity.getSimpleTypeName().startsWith("Test") && !entity.getSimpleTypeName().endsWith("TestCase") && !entity.getSimpleTypeName().endsWith("Test");
	}
}
