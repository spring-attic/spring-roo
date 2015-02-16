package org.springframework.roo.classpath.customdata.taggers;

import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;

/**
 * {@link MemberHoldingTypeDetails}-specific implementation of {@link Matcher}.
 * Matches are based on the the type's MID.
 * 
 * @author James Tyrrell
 * @since 1.1.3
 */
public abstract class TypeMatcher implements Matcher<MemberHoldingTypeDetails> {
}