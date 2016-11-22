package org.springframework.roo.addon.web.mvc.controller.addon;

import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata.RelationInfo;

/**
 * Extends {@link RelationInfo} to include {@link JpaEntityMetadata} of every parent and child
 *
 * @author Jose Manuel Vivó
 * @since 2.0
 */
public class RelationInfoExtended extends RelationInfo {

  public final JpaEntityMetadata entityMetadata;
  public final JpaEntityMetadata childEntityMetadata;

  protected RelationInfoExtended(RelationInfo info, JpaEntityMetadata entityMetadata,
      JpaEntityMetadata childEntityMetadata) {
    super(info.entityType, info.fieldName, info.addMethod, info.removeMethod, info.cardinality,
        info.childType, info.fieldMetadata, info.mappedBy, info.type);
    this.entityMetadata = entityMetadata;
    this.childEntityMetadata = childEntityMetadata;
  }

}
