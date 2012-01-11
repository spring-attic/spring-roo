package org.springframework.roo.classpath.customdata.taggers;

import java.util.List;

import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.model.CustomDataAccessor;
import org.springframework.roo.model.CustomDataKey;

/**
 * Matches {@link CustomDataAccessor}s based on a specific criteria and provides
 * the relevant key and value to be applied to a matched
 * {@link CustomDataAccessor}.
 * 
 * @author James Tyrrell
 * @since 1.1.3
 * @param <T> the type of {@link CustomDataAccessor} returned for each match
 */
public interface Matcher<T extends CustomDataAccessor> {

    /**
     * Returns a key indicating the type of custom data returned by
     * {@link #matches(List)}
     * 
     * @return a non-<code>null</code> key
     */
    CustomDataKey<T> getCustomDataKey();

    /**
     * Returns the value associated with the given key that should be applied to
     * the matched instance.
     * 
     * @param key the custom data key
     * @return a value (can be null)
     */
    Object getTagValue(T key);

    /**
     * Returns the {@link CustomDataAccessor}s for any elements of the given
     * list that meet this matcher's inclusion criteria.
     * 
     * @param memberHoldingTypeDetailsList the list to check for matches
     * @return a non-<code>null</code> list
     */
    List<T> matches(List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList);
}
