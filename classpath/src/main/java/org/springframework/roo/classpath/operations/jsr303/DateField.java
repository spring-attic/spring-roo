package org.springframework.roo.classpath.operations.jsr303;

import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.DefaultAnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.EnumAttributeValue;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * <p>
 * This field can optionally provide the mandatory JSR 220 temporal annotation.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public class DateField extends FieldDetails {
	
	/** Whether the JSR 220 @Temporal annotation will be added */
	private DateFieldPersistenceType persistenceType = null;
	
	/** Whether the JSR 303 @Past annotation will be added */
	private boolean past = false;
	
	/** Whether the JSR 303 @Future annotation will be added */
	private boolean future = false;

	public DateField(String physicalTypeIdentifier, JavaType fieldType, JavaSymbolName fieldName) {
		super(physicalTypeIdentifier, fieldType, fieldName);
	}

	public void decorateAnnotationsList(List<AnnotationMetadata> annotations) {
		super.decorateAnnotationsList(annotations);
		if (past) {
			annotations.add(new DefaultAnnotationMetadata(new JavaType("javax.validation.constraints.Past"), new ArrayList<AnnotationAttributeValue<?>>()));
		}
		if (future) {
			annotations.add(new DefaultAnnotationMetadata(new JavaType("javax.validation.constraints.Future"), new ArrayList<AnnotationAttributeValue<?>>()));
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
			annotations.add(new DefaultAnnotationMetadata(new JavaType("javax.persistence.Temporal"), attrs));
		}
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
	
}
