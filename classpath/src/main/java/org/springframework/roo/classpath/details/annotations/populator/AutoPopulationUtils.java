package org.springframework.roo.classpath.details.annotations.populator;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.ArrayAttributeValue;
import org.springframework.roo.classpath.details.annotations.BooleanAttributeValue;
import org.springframework.roo.classpath.details.annotations.CharAttributeValue;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.details.annotations.DoubleAttributeValue;
import org.springframework.roo.classpath.details.annotations.EnumAttributeValue;
import org.springframework.roo.classpath.details.annotations.IntegerAttributeValue;
import org.springframework.roo.classpath.details.annotations.LongAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;

/**
 * Automatically populates the {@link AutoPopulate} annotated fields on a 
 * given {@link Object}.
 *  
 * @author Ben Alex
 * @since 1.0
 */
public abstract class AutoPopulationUtils {
	private static final Map<Class<?>,List<Field>> cachedIntrospections = new HashMap<Class<?>, List<Field>>();
	private static final Map<Field, JavaSymbolName> attributeNameForEachField = new HashMap<Field, JavaSymbolName>();
	
	/**
	 * Introspects the target {@link Object} for its declared fields, locating all
	 * {@link AutoPopulate} annotated fields. For each field, an attempt will be made to
	 * locate the value from the passed {@link AnnotationMetadata}. The annotation value
	 * will be converted into the required field type, or silently skipped if this is not
	 * possible (eg the user edited source code and made a formatting error). As such it 
	 * is important that the caller.
	 * 
	 * @param target to put values into (mandatory, cannot be null)
	 * @param annotation to obtain values from (can be null, for convenience of the caller)
	 */
	public static final void populate(Object target, AnnotationMetadata annotation) {
		Assert.notNull(target, "Target required");
		
		if (annotation == null) {
			return;
		}
		
		List<Field> fields = cachedIntrospections.get(target.getClass());
		if (fields == null) {
			// Go and cache them
			fields = new ArrayList<Field>();

			for (Field field : target.getClass().getDeclaredFields()) {
				// Determine if this field even contains the necessary annotation
				AutoPopulate ap = field.getAnnotation(AutoPopulate.class);
				if (ap == null) {
					continue;
				}
				
				// Determine attribute name we should be looking for in the annotation
				String attribute = ap.value();
				if ("".equals(ap.value())) {
					attribute = field.getName();
				}
				
				// Ensure field is accessible
				if (!field.isAccessible()) {
					field.setAccessible(true);
				}

				JavaSymbolName attributeName = new JavaSymbolName(attribute);
				
				// Store the info
				fields.add(field);
				attributeNameForEachField.put(field, attributeName);

			}
			
			cachedIntrospections.put(target.getClass(), fields);
		}
		
		for (Field field : fields) {
			// Lookup whether this attribute name was provided
			JavaSymbolName attributeName = attributeNameForEachField.get(field);
			if (attributeName == null) {
				throw new IllegalStateException("Expected cached attribute name lookup");
			}
			
			if (annotation.getAttributeNames().contains(attributeName)) {
				// Get the value
				AnnotationAttributeValue<?> value = annotation.getAttribute(attributeName);
				
				// Assign the value to the target object
				try {
					Class<?> fieldType = field.getType();
					if (value instanceof BooleanAttributeValue && (fieldType.equals(Boolean.class) || fieldType.equals(Boolean.TYPE))) {
						field.set(target, value.getValue());
					} else if (value instanceof CharAttributeValue && (fieldType.equals(Character.class) || fieldType.equals(Character.TYPE))) {
						field.set(target, value.getValue());
					} else if (value instanceof ClassAttributeValue && fieldType.equals(JavaType.class)) {
						field.set(target, value.getValue());
					} else if (value instanceof DoubleAttributeValue && (fieldType.equals(Double.class) || fieldType.equals(Double.TYPE))) {
						field.set(target, value.getValue());
					} else if (value instanceof EnumAttributeValue && Enum.class.isAssignableFrom(fieldType)) {
						field.set(target, ((EnumAttributeValue) value).getAsEnum());
					} else if (value instanceof IntegerAttributeValue && (fieldType.equals(Integer.class) || fieldType.equals(Integer.TYPE))) {
						field.set(target, value.getValue());
					} else if (value instanceof LongAttributeValue && (fieldType.equals(Long.class) || fieldType.equals(Long.TYPE))) {
						field.set(target, value.getValue());
					} else if (value instanceof StringAttributeValue && fieldType.equals(String.class)) {
						field.set(target, value.getValue());
					} else if (value instanceof StringAttributeValue && fieldType.getComponentType() != null && fieldType.getComponentType().equals(String.class)) {
						// ROO-618
						Object newValue = Array.newInstance(String.class, 1);
						Array.set(newValue, 0, value.getValue());
						field.set(target, newValue);
					} else if (value instanceof ArrayAttributeValue<?> && fieldType.isArray()) {
						// The field is a string array, the attribute is an array, so let's hope it's a string array
						ArrayAttributeValue<?> castValue = (ArrayAttributeValue<?>) value;
						List<String> result = new ArrayList<String>();
						for (AnnotationAttributeValue<?> val : castValue.getValue()) {
							// For now we'll only support arrays of strings
							if (fieldType.getComponentType().equals(String.class) && val instanceof StringAttributeValue) {
								StringAttributeValue stringValue = (StringAttributeValue) val;
								result.add(stringValue.getValue());
							}
						}
						if (result.size() > 0) {
							// We had at least one string array, so we change the field
							field.set(target, result.toArray(new String[] {}));
						}
					}
					// If not in the above list, it's unsupported so we silently skip
				} catch (Throwable ignoreFailures) {}
			}
		}
	}
}
