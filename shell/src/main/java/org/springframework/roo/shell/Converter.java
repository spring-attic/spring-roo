package org.springframework.roo.shell;

import java.util.List;

/**
 * Converts between Strings (as displayed by and entered via the shell) and Roo domain objects
 * 
 * @author Andrew Swan 
 * @param <T> the domain type being converted to/from
 */
public interface Converter<T> {
	
	/**
	 * Indicates whether this converter supports the given domain type in the given option context
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
	 */
	T convertFromText(String value, Class<?> targetType, String optionContext);
	
	/**
	 * Populates the given list with the possible completions
	 * 
	 * @param completions the list to populate; can't be <code>null</code>
	 * @param targetType
	 * @param existingData what the user has typed so far
	 * @param optionContext
	 * @param target
	 * @return
	 */
	boolean getAllPossibleValues(List<String> completions, Class<?> targetType, String existingData, String optionContext, MethodTarget target);
}
