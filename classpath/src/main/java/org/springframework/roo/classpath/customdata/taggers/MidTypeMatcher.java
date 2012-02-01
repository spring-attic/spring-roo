package org.springframework.roo.classpath.customdata.taggers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.model.CustomDataKey;

/**
 * {@link MemberHoldingTypeDetails}-specific implementation of {@link Matcher}.
 * Matches are based on the the type's MID.
 * 
 * @author James Tyrrell
 * @since 1.1.3
 */
public class MidTypeMatcher extends TypeMatcher {

    private final CustomDataKey<MemberHoldingTypeDetails> customDataKey;
    private final String declaredBy;

    /**
     * Constructor
     * 
     * @param customDataKey
     * @param declaredBy the declaring class (required)
     * @since 1.2
     */
    public MidTypeMatcher(
            final CustomDataKey<MemberHoldingTypeDetails> customDataKey,
            final Class<?> declaredBy) {
        this(customDataKey, declaredBy.getName());
    }

    /**
     * Constructor
     * 
     * @param customDataKey
     * @param declaredBy (required)
     */
    public MidTypeMatcher(
            final CustomDataKey<MemberHoldingTypeDetails> customDataKey,
            final String declaredBy) {
        Validate.notBlank(declaredBy, "declaredBy is required");
        this.customDataKey = customDataKey;
        this.declaredBy = declaredBy;
    }

    public CustomDataKey<MemberHoldingTypeDetails> getCustomDataKey() {
        return customDataKey;
    }

    public Object getTagValue(final MemberHoldingTypeDetails key) {
        return null;
    }

    public List<MemberHoldingTypeDetails> matches(
            final List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList) {
        final List<MemberHoldingTypeDetails> types = new ArrayList<MemberHoldingTypeDetails>();
        for (final MemberHoldingTypeDetails memberHoldingTypeDetails : memberHoldingTypeDetailsList) {
            if (memberHoldingTypeDetails.getDeclaredByMetadataId().startsWith(
                    "MID:" + declaredBy)) {
                types.add(memberHoldingTypeDetails);
            }
        }
        return types;
    }
}
