package org.springframework.roo.classpath.customdata.taggers;

import org.springframework.roo.model.CustomDataKey;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.model.CustomDataAccessor;

import java.util.List;

/**
 * Matches {@link CustomDataAccessor}s based on a specific criteria and provides
 * the relevant key and value to be applied to a matched {@link CustomDataAccessor}.
 *
 * @author James Tyrrell
 * @since 1.1.3
 */
public interface Matcher<T extends CustomDataAccessor> {

	List<T> matches(List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList);

	CustomDataKey<T> getCustomDataKey();
}
