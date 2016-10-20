package org.springframework.roo.classpath.details;

import org.springframework.roo.classpath.details.comments.CommentedJavaStructure;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

import java.util.Comparator;

/**
 * Metadata concerning a particular field.
 *
 * @author Ben Alex
 * @since 1.0
 */
public interface FieldMetadata extends IdentifiableAnnotatedJavaStructure, CommentedJavaStructure {

  public static final Comparator<FieldMetadata> COMPARATOR_BY_NAME =
      new Comparator<FieldMetadata>() {
        @Override
        public int compare(FieldMetadata field1, FieldMetadata field2) {
          return field1.getFieldName().getSymbolName()
              .compareTo(field2.getFieldName().getSymbolName());
        }
      };

  /**
   * @return the field initializer, if known (may be null if there is no
   *         initializer)
   */
  String getFieldInitializer();

  /**
   * @return the name of the field (never null)
   */
  JavaSymbolName getFieldName();

  /**
   * @return the type of field (never null)
   */
  JavaType getFieldType();

}
