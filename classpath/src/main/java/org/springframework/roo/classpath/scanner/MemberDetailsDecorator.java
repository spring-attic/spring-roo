package org.springframework.roo.classpath.scanner;

import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.model.CustomDataAccessor;

/**
 * Provides the ability to modify or log the result of a
 * {@link MemberDetailsScanner} operation before the method returns. This is
 * useful to customize the results for a particular requesting class.
 * 
 * @author Ben Alex
 * @since 1.1
 */
public interface MemberDetailsDecorator {

    /**
     * Evaluates the incoming {@link MemberDetails} and either (a) returns the
     * same instance if no changes are necessary or (b) returns a new instance
     * of {@link MemberDetails} if changes are needed.
     * <p>
     * This method will be called repeatedly until such time as every
     * {@link MemberDetailsDecorator} returns the same {@link MemberDetails}
     * instance as was passed. <em>It is therefore essential that a
     * new instance is only returned if an actual change to the result is required.</em>
     * An implementation can safely return the same {@link MemberDetails} as it
     * was passed if in doubt, as it will always be re-invoked later on if
     * another decorator changes the {@link MemberDetails} instance. Thus even
     * decorators that depend on other decorators populating the
     * {@link MemberDetails} (eg with new {@link CustomDataAccessor}
     * information) can be executed in any order whatsoever, as they need only
     * look for the expected data and return the same {@link MemberDetails} if
     * it is not found.
     * 
     * @param requestingClass the fully-qualified class name requesting the
     *            member details (required)
     * @param memberDetails the current member holders (required)
     * @return the originally-passed details (where possible) or a replacement
     *         details (never returns null)
     */
    MemberDetails decorate(String requestingClass, MemberDetails memberDetails);

    /**
     * Performs essentially the same function as decorate but only decorates
     * {@link MemberHoldingTypeDetails} instances and ignores the type's
     * members.
     * 
     * @param requestingClass the fully-qualified class name requesting the
     *            member details (required)
     * @param memberDetails the current member holders (required)
     * @return the originally-passed details (where possible) or a replacement
     *         details (never returns null)
     */
    MemberDetails decorateTypes(String requestingClass,
            MemberDetails memberDetails);
}
