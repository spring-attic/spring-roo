package org.springframework.roo.classpath.details;

import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.BooleanAttributeValue;
import org.springframework.roo.classpath.details.annotations.CharAttributeValue;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.details.annotations.DoubleAttributeValue;
import org.springframework.roo.classpath.details.annotations.EnumAttributeValue;
import org.springframework.roo.classpath.details.annotations.IntegerAttributeValue;
import org.springframework.roo.classpath.details.annotations.LongAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;

/**
 * Utilities to use with {@link AnnotationMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public abstract class AnnotationMetadataUtils {

	public static final String toSourceForm(AnnotationMetadata annotation) {
		StringBuilder sb = new StringBuilder();
		sb.append("@");
		sb.append(annotation.getAnnotationType().getFullyQualifiedTypeName());
		
		if (annotation.getAttributeNames().size() == 0) {
			return sb.toString();
		}
		
		sb.append("(");
		boolean requireComma = false;
		for (JavaSymbolName attributeName : annotation.getAttributeNames()) {
			// Add a comma, to separate the last annotation attribute
			if (requireComma) {
				sb.append(", ");
				requireComma = false;
			}
			
			// Compute the value
			String attributeValue = null;
			AnnotationAttributeValue<? extends Object> value = annotation.getAttribute(attributeName);
			
			if (value instanceof BooleanAttributeValue) {
				attributeValue = ((BooleanAttributeValue)value).getValue().toString();
			} else if (value instanceof CharAttributeValue) {
				attributeValue = "'" + ((CharAttributeValue)value).getValue().toString() + "'";
			} else if (value instanceof ClassAttributeValue) {
				attributeValue = ((ClassAttributeValue)value).getValue().getFullyQualifiedTypeName() + ".class";
			} else if (value instanceof DoubleAttributeValue) {
				DoubleAttributeValue dbl = (DoubleAttributeValue) value;
				if (dbl.isFloatingPrecisionOnly()) {
					attributeValue = dbl.getValue().toString() + "F";
				} else {
					attributeValue = dbl.getValue().toString() + "D";
				}
			} else if (value instanceof EnumAttributeValue) {
				EnumDetails enumDetails = ((EnumAttributeValue)value).getValue();
				attributeValue = enumDetails.getType().getFullyQualifiedTypeName() + "." + enumDetails.getField().getSymbolName();
			} else if (value instanceof IntegerAttributeValue) {
				attributeValue = ((IntegerAttributeValue)value).getValue().toString();
			} else if (value instanceof LongAttributeValue) {
				attributeValue = ((LongAttributeValue)value).getValue().toString() + "L";
			} else if (value instanceof StringAttributeValue) {
				attributeValue = "\"" + ((StringAttributeValue)value).getValue() + "\"";
			}
			
			if (attributeValue != null) {
				// We have a supported attribute
				if (!"value".equals(attributeName.getSymbolName()) || annotation.getAttributeNames().size() > 1) {
					sb.append(attributeName.getSymbolName());
					sb.append(" = ");
				}
				sb.append(attributeValue);
				requireComma = true;
			}
		}
		sb.append(")");
		return sb.toString();
	}
}
