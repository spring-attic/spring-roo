package org.springframework.roo.classpath.operations.jsr303;

import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.EnumAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.operations.DateTime;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * This field can optionally provide the mandatory JSR 220 temporal annotation.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class DateField extends FieldDetails {
	
	/** Whether the JSR 220 @Temporal annotation will be added */
	private DateFieldPersistenceType persistenceType = null;
	
	/** Whether the JSR 303 @Past annotation will be added */
	private boolean past = false;
	
	/** Whether the JSR 303 @Future annotation will be added */
	private boolean future = false;

	private DateTime dateFormat = null;
	
	private DateTime timeFormat = null;

	/** Custom date formatting through a DateTime pattern such as yyyy/mm/dd h:mm:ss a. */
	private String pattern = null;

	public DateField(String physicalTypeIdentifier, JavaType fieldType, JavaSymbolName fieldName) {
		super(physicalTypeIdentifier, fieldType, fieldName);
	}

	public void decorateAnnotationsList(List<AnnotationMetadataBuilder> annotations) {
		super.decorateAnnotationsList(annotations);
		if (past) {
			annotations.add(new AnnotationMetadataBuilder(new JavaType("javax.validation.constraints.Past")));
		}
		if (future) {
			annotations.add(new AnnotationMetadataBuilder(new JavaType("javax.validation.constraints.Future")));
		}
		if (persistenceType != null) {
			// Add JSR 220 @Temporal annotation
			String value = null;
			if (persistenceType == DateFieldPersistenceType.JPA_DATE) {
				value = "DATE";
			} else if (persistenceType == DateFieldPersistenceType.JPA_TIME) {
				value = "TIME";
			} else if (persistenceType == DateFieldPersistenceType.JPA_TIMESTAMP) {
				value = "TIMESTAMP";
			}
			List<AnnotationAttributeValue<?>> attrs = new ArrayList<AnnotationAttributeValue<?>>();
			attrs.add(new EnumAttributeValue(new JavaSymbolName("value"), new EnumDetails(new JavaType("javax.persistence.TemporalType"), new JavaSymbolName(value))));
			annotations.add(new AnnotationMetadataBuilder(new JavaType("javax.persistence.Temporal"), attrs));
		}
		// Always add a DateTimeFormat annotation
		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		if (pattern != null) {
			attributes.add(new StringAttributeValue(new JavaSymbolName("pattern"), pattern));
		} else {
			String dateStyle = null != dateFormat ? String.valueOf(dateFormat.getShortKey()) : "S";
			String timeStyle = null != timeFormat ? String.valueOf(timeFormat.getShortKey()) : "-";
			attributes.add(new StringAttributeValue(new JavaSymbolName("style"), dateStyle + timeStyle));
		}
		annotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.format.annotation.DateTimeFormat"), attributes));
	}

	public boolean isPast() {
		return past;
	}

	public void setPast(boolean past) {
		this.past = past;
	}

	public boolean isFuture() {
		return future;
	}

	public void setFuture(boolean future) {
		this.future = future;
	}

	public DateFieldPersistenceType getPersistenceType() {
		return persistenceType;
	}

	public void setPersistenceType(DateFieldPersistenceType persistenceType) {
		this.persistenceType = persistenceType;
	}

	public DateTime getDateFormat() {
		return dateFormat;
	}

	public void setDateFormat(DateTime dateFormat) {
		this.dateFormat = dateFormat;
	}

	public DateTime getTimeFormat() {
		return timeFormat;
	}

	public void setTimeFormat(DateTime timeFormat) {
		this.timeFormat = timeFormat;
	}

	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

}
