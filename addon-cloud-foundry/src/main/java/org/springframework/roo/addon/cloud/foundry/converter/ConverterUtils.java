package org.springframework.roo.addon.cloud.foundry.converter;

/**
 * Utility methods for use by converters.
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public final class ConverterUtils {

	/**
	 * Constructor is private to prevent instantiation
	 */
	private ConverterUtils() {
		// Empty
	}

	/**
	 * Returns the value of the given option from the given command line
	 *
	 * @param option the option whose value to retrieve
	 * @param buffer the command line to parse; can't be <code>null</code>
	 * @return <code>null</code> if that option isn't present or doesn't have a value
	 */
	public static String getOptionValue(final String option, final String buffer) {
		final String[] words = buffer.split(" ");
		for (int i = 0; i < words.length; i++) {
			if (words[i].equals("--" + option) && i + 1 < words.length) {
				return words[i + 1];
			}
		}
		return null;
	}
}
