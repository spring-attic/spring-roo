package org.springframework.roo.classpath.details;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Provides utility methods for querying JavaBeans.
 * 
 * @author Ben Alex
 * @since 1.1.1
 */
public final class BeanInfoUtils {

    /**
     * Returns the accessor name for the given field.
     * 
     * @param field the field to determine the accessor name
     * @return the accessor method name
     */
    public static JavaSymbolName getAccessorMethodName(final FieldMetadata field) {
        Validate.notNull(field, "Field required");
        return getAccessorMethodName(field.getFieldName(), field.getFieldType());
    }

    /**
     * Returns the accessor name for the given field name and field type.
     * 
     * @param fieldName the field name used to determine the accessor name
     * @param fieldType the field type
     * @return the accessor method name
     */
    public static JavaSymbolName getAccessorMethodName(
            final JavaSymbolName fieldName, final JavaType fieldType) {
        Validate.notNull(fieldName, "Field name required");
        Validate.notNull(fieldType, "Field type required");
        final String capitalizedFieldName = StringUtils.capitalize(fieldName
                .getSymbolName());
        return fieldType.equals(JavaType.BOOLEAN_PRIMITIVE) ? new JavaSymbolName(
                "is" + capitalizedFieldName) : new JavaSymbolName("get"
                + capitalizedFieldName);
    }

    /**
     * Attempts to locate the field which is represented by the presented java
     * bean method.
     * <p>
     * Not every JavaBean getter or setter actually backs to a field with an
     * identical name. In such cases, null will be returned.
     * 
     * @param memberDetails the member holders to scan (required)
     * @param method the method name (required)
     * @return the field if found, or null if it could not be found
     */
    public static FieldMetadata getFieldForJavaBeanMethod(
            final MemberDetails memberDetails, final MethodMetadata method) {
        Validate.notNull(memberDetails, "Member details required");
        Validate.notNull(method, "Method is required");
        final JavaSymbolName propertyName = getPropertyNameForJavaBeanMethod(method);
        return getFieldForPropertyName(memberDetails, propertyName);
    }

    /**
     * Attempts to locate the field which is represented by the presented
     * property name.
     * <p>
     * Not every JavaBean getter or setter actually backs to a field with an
     * identical name. In such cases, null will be returned.
     * 
     * @param memberDetails the member holders to scan (required)
     * @param propertyName the property name (required)
     * @return the field if found, or null if it could not be found
     */
    public static FieldMetadata getFieldForPropertyName(
            final MemberDetails memberDetails, final JavaSymbolName propertyName) {
        Validate.notNull(memberDetails, "Member details required");
        Validate.notNull(propertyName, "Property name required");
        for (final MemberHoldingTypeDetails holder : memberDetails.getDetails()) {
            FieldMetadata result = holder.getDeclaredField(propertyName);
            if (result != null) {
                return result;
            }
            // To get here means we couldn't find the property using the exact
            // same case;
            // try to scan with a lowercase first character (see ROO-203)
            result = holder.getDeclaredField(new JavaSymbolName(StringUtils
                    .uncapitalize(propertyName.getSymbolName())));
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    /**
     * Returns the mutator name for the given field.
     * 
     * @param field the field used to determine the accessor name
     * @return the mutator method name
     */
    public static JavaSymbolName getMutatorMethodName(final FieldMetadata field) {
        Validate.notNull(field, "Field metadata required");
        return getMutatorMethodName(field.getFieldName());
    }

    /**
     * Returns the mutator name for the given field name.
     * 
     * @param fieldName the field name used to determine the accessor name
     * @return the mutator method name
     */
    public static JavaSymbolName getMutatorMethodName(
            final JavaSymbolName fieldName) {
        Validate.notNull(fieldName, "Field name required");
        return new JavaSymbolName("set"
                + StringUtils.capitalize(fieldName.getSymbolName()));
    }

    /**
     * Obtains the property name for the specified JavaBean accessor or mutator
     * method. This is determined by discarding the first 2 or 3 letters of the
     * method name (depending whether it is a "get", "set" or "is" method).
     * There is no special searching back to the actual field name.
     * 
     * @param method to search (required, and must be a "get", "set" or "is"
     *            method)
     * @return the name of the property (never returned null)
     */
    public static JavaSymbolName getPropertyNameForJavaBeanMethod(
            final MethodMetadata method) {
        Validate.notNull(method, "Method is required");
        final String name = method.getMethodName().getSymbolName();
        if (name.startsWith("set") || name.startsWith("get")) {
            return new JavaSymbolName(name.substring(3));
        }
        if (name.startsWith("is")) {
            return new JavaSymbolName(name.substring(2));
        }
        throw new IllegalStateException("Method name '" + name
                + "' does not observe JavaBean method naming conventions");
    }

    /**
     * Attempts to locate an accessor and a mutator method for a given field.
     * <p>
     * Not every JavaBean getter or setter actually backs to a field with an
     * identical name. In such cases, false will be returned.
     * 
     * @param field the member holders to scan (required)
     * @param memberDetails the member details to scan
     * @return true if an accessor and a mutator are present, or false otherwise
     */
    public static boolean hasAccessorAndMutator(final FieldMetadata field,
            final MemberDetails memberDetails) {
        Validate.notNull(field, "Field required");
        Validate.notNull(memberDetails, "Member details required");

        if (memberDetails.getMethod(getAccessorMethodName(field),
                new ArrayList<JavaType>()) != null
                && memberDetails.getMethod(getMutatorMethodName(field),
                        Arrays.asList(field.getFieldType())) != null) {
            return true;
        }

        return false;
    }

    /**
     * Indicates if the presented method compiles with the JavaBean conventions
     * around accessor methods (public, "set" or "is", 0 args etc).
     * 
     * @param method to evaluate (required)
     * @return true if the presented method is an accessor, otherwise false
     */
    public static boolean isAccessorMethod(final MethodMetadata method) {
        Validate.notNull(method, "Method is required");
        return (method.getMethodName().getSymbolName().startsWith("get") || method
                .getMethodName().getSymbolName().startsWith("is"))
                && method.getParameterTypes().isEmpty()
                && Modifier.isPublic(method.getModifier());
    }

    /**
     * Determines whether the presented entity is a test class or not.
     * 
     * @param entity the type to test
     * @return true if the entity is likely not a test class, otherwise false
     */
    public static boolean isEntityReasonablyNamed(final JavaType entity) {
        Validate.notNull(entity, "Entity required");
        return !entity.getSimpleTypeName().startsWith("Test")
                && !entity.getSimpleTypeName().endsWith("TestCase")
                && !entity.getSimpleTypeName().endsWith("Test");
    }

    /**
     * Indicates if the presented method compiles with the JavaBean conventions
     * around mutator methods (public, "set", 1 arg etc).
     * 
     * @param method to evaluate (required)
     * @return true if the presented method is a mutator, otherwise false
     */
    public static boolean isMutatorMethod(final MethodMetadata method) {
        Validate.notNull(method, "Method is required");
        return method.getMethodName().getSymbolName().startsWith("set")
                && method.getParameterTypes().size() == 1
                && Modifier.isPublic(method.getModifier());
    }

    /**
     * Constructor is private to prevent instantiation
     * 
     * @since 1.2.0
     */
    private BeanInfoUtils() {
    }
}
