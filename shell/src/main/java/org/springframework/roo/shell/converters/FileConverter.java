package org.springframework.roo.shell.converters;

import java.io.File;
import java.util.List;

import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;
import org.springframework.roo.support.util.Assert;

/**
 * {@link Converter} for {@link File}.
 *
 * @author Stefan Schmidt
 * @author Roman Kuzmik
 * @author Ben Alex
 * @since 1.0
 *
 */
@ScopeDevelopment
public class FileConverter implements Converter {

	private static final String home = System.getProperty("user.home");

	public Object convertFromText(String value, Class<?> requiredType, String optionContext) {
		return new File(removeTildeIfNeeded(value));
	}
	
	public boolean getAllPossibleValues(List<String> completions, Class<?> requiredType, String originalUserInput, String optionContext, MethodTarget target) {
		String adjustedUserInput = removeTildeIfNeeded(originalUserInput);

		String directoryData = "";
		if (adjustedUserInput != null && adjustedUserInput.contains(File.separator)) {
			directoryData = adjustedUserInput.substring(0, adjustedUserInput.lastIndexOf(File.separator) + 1);
			adjustedUserInput = adjustedUserInput.substring(adjustedUserInput.lastIndexOf(File.separator) + 1);
		}
		
		populate(completions, adjustedUserInput, originalUserInput, directoryData);

		return false;
	}

	protected void populate(List<String> completions, String adjustedUserInput, String originalUserInput, String directoryData) {
		File directory = new File(directoryData.length() > 0 ? directoryData : ".");
		Assert.isTrue(directory.isDirectory(), "Directory '" + directory.toString() + "' is not a valid directory name");

		for (File file : directory.listFiles()) {
			if (adjustedUserInput == null || adjustedUserInput.length() == 0 || 
				file.getName().toLowerCase().startsWith(adjustedUserInput.toLowerCase())) {

				String completion = "";
				if (directoryData.length() > 0)
					completion += directoryData;
				completion += file.getName();

				completion = addTildeIfNeeded(originalUserInput, completion);
				
				if (file.isDirectory()) {
					completions.add(completion + File.separator);
				} else {
					completions.add(completion);
				}
			}
		}
	}
	
	public boolean supports(Class<?> requiredType, String optionContext) {
		return File.class.isAssignableFrom(requiredType);
	}
	
	private String addTildeIfNeeded(String originalUserInput, String completion) {
		if (!originalUserInput.startsWith("~")) {
			return completion;
		}
		Assert.notNull(home, "Home directory could not be determined from system properties");
		if (!completion.startsWith(home)) {
			// this completion isn't even under the home directory (which is a bit weird given it started with ~, but anyway...)
			return completion;
		}
		return "~" + completion.substring(home.length());
	}

	private String removeTildeIfNeeded(String userInput) {
		if (!userInput.startsWith("~")) {
			return userInput;
		}
		Assert.notNull(home, "Home directory could not be determined from system properties");
		if (userInput.length() > 1) {
			return home + userInput.substring(1);
		}
		return home;
	}

}