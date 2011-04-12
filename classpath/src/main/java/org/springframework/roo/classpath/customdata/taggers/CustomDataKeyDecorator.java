package org.springframework.roo.classpath.customdata.taggers;

import org.springframework.roo.classpath.scanner.MemberDetailsDecorator;
import org.springframework.roo.model.CustomDataAccessor;

/**
 * Provides a universal registry for {@link Matcher} objects. Initially
 * no checks are being performed upon adding new Matcher instances, it
 * is envisioned that this will change and an alert would be provided
 * if a {@link Matcher} object was in conflict with another. For this to
 * happen a more fleshed out matching API needs to be implemented as so
 * comparison of {@link Matcher}s can be performed easily. In addition to
 * registering {@link Matcher}s this interface allows {@link Matcher} objects
 * to be unregistered based on the registering class which is useful if
 * the registering class is no longer in play.
 *
 * @author James Tyrrell
 * @since 1.1.3
 */
public interface CustomDataKeyDecorator extends MemberDetailsDecorator {

	void registerMatcher(String addingClass, Matcher<? extends CustomDataAccessor> matcher);

	void unregisterMatchers(String addingClass);
}
