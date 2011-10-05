package org.springframework.roo.shell;

import java.util.List;

/**
 * Converts between Strings (as displayed by and entered via the shell) and Java objects
 *
 * @author Ben Alex
 * @param <T> the type being converted to/from
 */
public interface Converter<T> {

	/**
	 * Indicates whether this converter supports the given type in the given option context
	 *
	 * @param type the type being checked
	 * @param optionContext the name of the option context
	 * @return see above
	 */
	boolean supports(Class<?> type, String optionContext);

	/**
	 * Converts from the given String value to type T
	 *
	 * @param value the value to convert
	 * @param targetType the type being converted to; can't be <code>null</code>
	 * @param optionContext the name of the option context
	 * @return see above
	 * @throws RuntimeException if the given value could not be converted
	 */
	T convertFromText(String value, Class<?> targetType, String optionContext);

	/**
	 * Populates the given list with the possible completions
	 *
	 * @param completions the list to populate; can't be <code>null</code>
	 * @param targetType the type of parameter for which a string is being entered
	 * @param existingData what the user has typed so far
	 * @param optionContext
	 * @param target
	 * @return <code>true</code> if all the added completions are complete
	 * values, or <code>false</code> if the user can press TAB to add further
	 * information to some or all of them
	 */
	boolean getAllPossibleValues(List<String> completions, Class<?> targetType, String existingData, String optionContext, MethodTarget target);
}
