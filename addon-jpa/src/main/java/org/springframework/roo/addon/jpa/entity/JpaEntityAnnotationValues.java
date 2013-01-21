package org.springframework.roo.addon.jpa.entity;

import static org.springframework.roo.addon.jpa.entity.RooJpaEntity.VERSION_FIELD_DEFAULT;

import org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.classpath.itd.MemberHoldingTypeDetailsMetadataItem;
import org.springframework.roo.model.JavaType;

/**
 * The purely JPA-related values of a single {@link RooJpaEntity} or
 * {@link RooJpaActiveRecord} annotation.
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class JpaEntityAnnotationValues extends AbstractAnnotationValues {

    @AutoPopulate private String catalog = "";
    @AutoPopulate private String entityName = "";
    @AutoPopulate private String identifierColumn = "";
    @AutoPopulate private String identifierField = "";
    @AutoPopulate private JavaType identifierType;
    @AutoPopulate private String inheritanceType = "";
    @AutoPopulate private boolean mappedSuperclass;
    @AutoPopulate private String schema = "";
    @AutoPopulate private String sequenceName = null;
    @AutoPopulate private String table = "";
    @AutoPopulate private String versionColumn = "";
    @AutoPopulate private String versionField = VERSION_FIELD_DEFAULT;
    @AutoPopulate private JavaType versionType = JavaType.INT_OBJECT;

    /**
     * Constructor for reading the values of the given annotation
     * 
     * @param annotatedType the type from which to read the values (required)
     * @param triggerAnnotation the type of annotation from which to read the
     *            values (required)
     * @since 1.2.0
     */
    public JpaEntityAnnotationValues(
            final MemberHoldingTypeDetailsMetadataItem<?> annotatedType,
            final JavaType annotationType) {
        super(annotatedType, annotationType);
        // TODO move to superclass for this and all sibling classes?
        AutoPopulationUtils.populate(this, annotationMetadata);
    }

    public String getCatalog() {
        return catalog;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getIdentifierColumn() {
        return identifierColumn;
    }

    public String getIdentifierField() {
        return identifierField;
    }

    public JavaType getIdentifierType() {
        return identifierType;
    }

    public String getInheritanceType() {
        return inheritanceType;
    }

    public String getSchema() {
        return schema;
    }

    public String getSequenceName() {
        return sequenceName;
    }

    public String getTable() {
        return table;
    }

    public String getVersionColumn() {
        return versionColumn;
    }

    public String getVersionField() {
        return versionField;
    }

    public JavaType getVersionType() {
        return versionType;
    }

    public boolean isMappedSuperclass() {
        return mappedSuperclass;
    }
}
