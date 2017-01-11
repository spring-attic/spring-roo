package org.springframework.roo.addon.jpa.addon.entity;

import org.springframework.roo.addon.jpa.annotations.entity.RooJpaEntity;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.classpath.itd.MemberHoldingTypeDetailsMetadataItem;
import org.springframework.roo.model.JavaType;

/**
 * The purely JPA-related values of a single {@link RooJpaEntity} annotation.
 * 
 * @author Andrew Swan
 * @author Juan Carlos García
 * @author Sergio Clares
 * @since 1.2.0
 */
public class JpaEntityAnnotationValues extends AbstractAnnotationValues {

  @AutoPopulate
  private String catalog = "";
  @AutoPopulate
  private String entityName = "";
  @AutoPopulate
  private String inheritanceType = "";
  @AutoPopulate
  private boolean mappedSuperclass;
  @AutoPopulate
  private String schema = "";
  @AutoPopulate
  private String table = "";
  @AutoPopulate
  private boolean readOnly;
  @AutoPopulate
  private String entityFormatMessage = "";
  @AutoPopulate
  private String entityFormatExpression = "";

  /**
   * Constructor for reading the values of the given annotation
   * 
   * @param annotatedType the type from which to read the values (required)
   * @param triggerAnnotation the type of annotation from which to read the
   *            values (required)
   * @since 1.2.0
   */
  public JpaEntityAnnotationValues(final MemberHoldingTypeDetailsMetadataItem<?> annotatedType,
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

  public String getInheritanceType() {
    return inheritanceType;
  }

  public String getSchema() {
    return schema;
  }

  public String getTable() {
    return table;
  }

  public boolean isMappedSuperclass() {
    return mappedSuperclass;
  }

  public boolean isReadOnly() {
    return readOnly;
  }

  public String getEntityFormatExpression() {
    return entityFormatExpression;
  }

  public String getEntityFormatMessage() {
    return entityFormatMessage;
  }

}
