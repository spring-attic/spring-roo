package org.springframework.roo.classpath.itd;

import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.MetadataItem;

/**
 * Indicates a {@link MetadataItem} capable of returning
 * {@link MemberHoldingTypeDetails}.
 * <p>
 * Most Roo metadata builds types, and therefore can provide member information
 * via the {@link MemberHoldingTypeDetails} interface. This interface offers a
 * convenient way for discovering available member information for iteration
 * etc. See for example {@link MemberDetailsScanner} for a common usage pattern.
 * 
 * @author Ben Alex
 * @since 1.1.1
 * @param <T> the type of {@link MemberHoldingTypeDetails} that will be provided
 */
public interface MemberHoldingTypeDetailsMetadataItem<T extends MemberHoldingTypeDetails>
        extends MetadataItem {

    /**
     * Obtains the {@link MemberHoldingTypeDetails}, if available.
     * <p>
     * An {@link MemberHoldingTypeDetails} should be returned even if no members
     * should be introduced. Only return null if there was a failure during
     * parsing or other unexpected condition.
     * 
     * @return the details, or null if the details are unavailable or no member
     *         details are required
     */
    T getMemberHoldingTypeDetails();
}
