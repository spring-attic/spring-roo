package org.springframework.roo.shell.converters;

import java.io.File;
import java.util.List;

import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileUtils;

/**
 * {@link Converter} for {@link File}.
 *
 * @author Stefan Schmidt
 * @author Roman Kuzmik
 * @author Ben Alex
 * @since 1.0
 *
 */
public abstract class FileConverter implements Converter {

	private static final String home = System.getProperty("user.home");

	/**
	 * @return the "current working directory" this {@link FileConverter} should use if the user fails to provide
	 * an explicit directory in their input (required)
	 */
	protected abstract File getWorkingDirectory();
	
	public Object convertFromText(String value, Class<?> requiredType, String optionContext) {
		return new File(convertUserInputIntoAFullyQualifiedPath(value));
	}
	
	public boolean getAllPossibleValues(List<String> completions, Class<?> requiredType, String originalUserInput, String optionContext, MethodTarget target) {
		String adjustedUserInput = convertUserInputIntoAFullyQualifiedPath(originalUserInput);

		String directoryData = adjustedUserInput.substring(0, adjustedUserInput.lastIndexOf(File.separator) + 1);
		adjustedUserInput = adjustedUserInput.substring(adjustedUserInput.lastIndexOf(File.separator) + 1);
		
		populate(completions, adjustedUserInput, originalUserInput, directoryData);

		return false;
	}

	protected void populate(List<String> completions, String adjustedUserInput, String originalUserInput, String directoryData) {
		File directory = new File(directoryData);
		
		if (!directory.isDirectory()) {
			return;
		}

		for (File file : directory.listFiles()) {
			if (adjustedUserInput == null || adjustedUserInput.length() == 0 || 
				file.getName().toLowerCase().startsWith(adjustedUserInput.toLowerCase())) {

				String completion = "";
				if (directoryData.length() > 0)
					completion += directoryData;
				completion += file.getName();

				completion = convertCompletionBackIntoUserInputStyle(originalUserInput, completion);
				
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
	
	private String convertCompletionBackIntoUserInputStyle(String originalUserInput, String completion) {
		if (FileUtils.denotesAbsolutePath(originalUserInput)) {
			// Input was originally as a fully-qualified path, so we just keep the completion in that form
			return completion;
		}
		if (originalUserInput.startsWith("~")) {
			// Input originally started with a ~, so replace the user's home directory with a ~ again
			Assert.notNull(home, "Home directory could not be determined from system properties");
			return "~" + completion.substring(home.length());
		}
		// The path was working directory specific, so strip the working directory given the user never typed it
		return completion.substring(getWorkingDirectoryAsString().length());
	}

	/**
	 * If the user input starts with a tilde character (~), replace the tilde character with the
	 * user's home directory. If the user input does not start with a tilde, simply return the original
	 * user input without any changes if the input specifies an absolute path, or return an absolute path 
	 * based on the working directory if the input specifies a relative path.
	 * 
	 * @param userInput the user input, which may commence with a tilde (required)
	 * @return a string that is guaranteed to no longer contain a tilde as the first character (never null)
	 */
	private String convertUserInputIntoAFullyQualifiedPath(String userInput) {
		if (FileUtils.denotesAbsolutePath(userInput)) {
			// Input is already in a fully-qualified path form
			return userInput;
		}
		if (userInput.startsWith("~")) {
			// Input starts with a ~, so replace with the user's home directory
			Assert.notNull(home, "Home directory could not be determined from system properties");
			if (userInput.length() > 1) {
				return home + userInput.substring(1);
			}
		}
		// The path is working directory specific, so prepend the working directory
		String fullPath = getWorkingDirectoryAsString() + userInput;
		return fullPath;
	}
	
	private String getWorkingDirectoryAsString() {
		try {
			return getWorkingDirectory().getCanonicalPath() + File.separator;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

}