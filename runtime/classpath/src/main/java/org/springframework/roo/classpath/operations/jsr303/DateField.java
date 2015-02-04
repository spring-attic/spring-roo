package org.springframework.roo.classpath.operations.jsr303;

import static org.springframework.roo.model.JpaJavaType.TEMPORAL;
import static org.springframework.roo.model.JpaJavaType.TEMPORAL_TYPE;
import static org.springframework.roo.model.Jsr303JavaType.FUTURE;
import static org.springframework.roo.model.Jsr303JavaType.PAST;
import static org.springframework.roo.model.SpringJavaType.DATE_TIME_FORMAT;

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

    private DateTime dateFormat;

    /** Whether the JSR 303 @Future annotation will be added */
    private boolean future;

    /** Whether the JSR 303 @Past annotation will be added */
    private boolean past;

    /**
     * Custom date formatting through a DateTime pattern such as yyyy/mm/dd
     * h:mm:ss a.
     */
    private String pattern;

    /** Whether the JSR 220 @Temporal annotation will be added */
    private DateFieldPersistenceType persistenceType;

    private DateTime timeFormat;

    public DateField(final String physicalTypeIdentifier,
            final JavaType fieldType, final JavaSymbolName fieldName) {
        super(physicalTypeIdentifier, fieldType, fieldName);
    }

    @Override
    public void decorateAnnotationsList(
            final List<AnnotationMetadataBuilder> annotations) {
        super.decorateAnnotationsList(annotations);
        if (past) {
            annotations.add(new AnnotationMetadataBuilder(PAST));
        }
        if (future) {
            annotations.add(new AnnotationMetadataBuilder(FUTURE));
        }
        if (persistenceType != null) {
            // Add JSR 220 @Temporal annotation
            String value = null;
            if (persistenceType == DateFieldPersistenceType.JPA_DATE) {
                value = "DATE";
            }
            else if (persistenceType == DateFieldPersistenceType.JPA_TIME) {
                value = "TIME";
            }
            else if (persistenceType == DateFieldPersistenceType.JPA_TIMESTAMP) {
                value = "TIMESTAMP";
            }
            final List<AnnotationAttributeValue<?>> attrs = new ArrayList<AnnotationAttributeValue<?>>();
            attrs.add(new EnumAttributeValue(new JavaSymbolName("value"),
                    new EnumDetails(TEMPORAL_TYPE, new JavaSymbolName(value))));
            annotations.add(new AnnotationMetadataBuilder(TEMPORAL, attrs));
        }
        // Always add a DateTimeFormat annotation
        final List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
        if (pattern != null) {
            attributes.add(new StringAttributeValue(new JavaSymbolName(
                    "pattern"), pattern));
        }
        else {
            final String dateStyle = null != dateFormat ? String
                    .valueOf(dateFormat.getShortKey()) : "M";
            final String timeStyle = null != timeFormat ? String
                    .valueOf(timeFormat.getShortKey()) : "-";
            attributes.add(new StringAttributeValue(
                    new JavaSymbolName("style"), dateStyle + timeStyle));
        }
        annotations.add(new AnnotationMetadataBuilder(DATE_TIME_FORMAT,
                attributes));
    }

    public DateTime getDateFormat() {
        return dateFormat;
    }

    public String getPattern() {
        return pattern;
    }

    public DateFieldPersistenceType getPersistenceType() {
        return persistenceType;
    }

    public DateTime getTimeFormat() {
        return timeFormat;
    }

    public boolean isFuture() {
        return future;
    }

    public boolean isPast() {
        return past;
    }

    public void setDateFormat(final DateTime dateFormat) {
        this.dateFormat = dateFormat;
    }

    public void setFuture(final boolean future) {
        this.future = future;
    }

    public void setPast(final boolean past) {
        this.past = past;
    }

    public void setPattern(final String pattern) {
        this.pattern = pattern;
    }

    public void setPersistenceType(
            final DateFieldPersistenceType persistenceType) {
        this.persistenceType = persistenceType;
    }

    public void setTimeFormat(final DateTime timeFormat) {
        this.timeFormat = timeFormat;
    }
}
