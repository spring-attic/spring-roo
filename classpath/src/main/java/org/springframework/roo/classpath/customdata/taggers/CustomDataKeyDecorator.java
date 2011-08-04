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

	/**
	 * Registers the given matcher
	 * 
	 * @param addingClass the class adding the matcher (required)
	 * @param matchers the matchers being registered
	 * @since 1.2
	 */
	void registerMatchers(Class<?> addingClass, Matcher<? extends CustomDataAccessor>... matchers);
	
	/**
	 * Registers the given matcher
	 * 
	 * @param addingClass the name of the class adding the matcher (required)
	 * @param matcher the matcher being registered (required)
	 */
	void registerMatcher(String addingClass, Matcher<? extends CustomDataAccessor> matcher);

	/**
	 * Unregisters any matchers registered by the given class
	 * 
	 * @param addingClass the class whose matchers are to be unregistered (required)
	 * @since 1.2
	 */
	void unregisterMatchers(Class<?> addingClass);
	
	/**
	 * Unregisters any matchers registered by the given class
	 * 
	 * @param addingClass the class whose matchers are to be unregistered (required)
	 */
	void unregisterMatchers(String addingClass);
}
