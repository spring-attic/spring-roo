package org.springframework.roo.classpath.itd;

import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.metadata.MetadataItem;

/**
 * Indicates a {@link MetadataItem} implementation that can provide
 * {@link ItdTypeDetails}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public interface ItdTypeDetailsProvidingMetadataItem extends
        MemberHoldingTypeDetailsMetadataItem<ItdTypeDetails> {
}
