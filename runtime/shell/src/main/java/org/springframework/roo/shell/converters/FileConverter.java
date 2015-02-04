package org.springframework.roo.shell.converters;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.roo.shell.Completion;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

/**
 * {@link Converter} for {@link File}.
 * 
 * @author Stefan Schmidt
 * @author Roman Kuzmik
 * @author Ben Alex
 * @since 1.0
 */
public abstract class FileConverter implements Converter<File> {

    private static final String HOME_DIRECTORY_SYMBOL = "~";
    private static final String home = System.getProperty("user.home");
    private static final String WINDOWS_DRIVE_PREFIX = "^[A-Za-z]:";

    // Doesn't check for backslash after the colon, since Java has no issues
    // with paths like c:/Windows
    private static final Pattern WINDOWS_DRIVE_PATH = Pattern
            .compile(WINDOWS_DRIVE_PREFIX + ".*");

    private String convertCompletionBackIntoUserInputStyle(
            final String originalUserInput, final String completion) {
        if (denotesAbsolutePath(originalUserInput)) {
            // Input was originally as a fully-qualified path, so we just keep
            // the completion in that form
            return completion;
        }
        if (originalUserInput.startsWith(HOME_DIRECTORY_SYMBOL)) {
            // Input originally started with this symbol, so replace the user's
            // home directory with it again
            Validate.notNull(home,
                    "Home directory could not be determined from system properties");
            return HOME_DIRECTORY_SYMBOL + completion.substring(home.length());
        }
        // The path was working directory specific, so strip the working
        // directory given the user never typed it
        return completion.substring(getWorkingDirectoryAsString().length());
    }

    public File convertFromText(final String value,
            final Class<?> requiredType, final String optionContext) {
        return new File(convertUserInputIntoAFullyQualifiedPath(value));
    }

    /**
     * If the user input starts with a tilde character (~), replace the tilde
     * character with the user's home directory. If the user input does not
     * start with a tilde, simply return the original user input without any
     * changes if the input specifies an absolute path, or return an absolute
     * path based on the working directory if the input specifies a relative
     * path.
     * 
     * @param userInput the user input, which may commence with a tilde
     *            (required)
     * @return a string that is guaranteed to no longer contain a tilde as the
     *         first character (never null)
     */
    private String convertUserInputIntoAFullyQualifiedPath(
            final String userInput) {
        if (denotesAbsolutePath(userInput)) {
            // Input is already in a fully-qualified path form
            return userInput;
        }
        if (userInput.startsWith(HOME_DIRECTORY_SYMBOL)) {
            // Replace this symbol with the user's actual home directory
            Validate.notNull(home,
                    "Home directory could not be determined from system properties");
            if (userInput.length() > 1) {
                return home + userInput.substring(1);
            }
        }
        // The path is working directory specific, so prepend the working
        // directory
        final String fullPath = getWorkingDirectoryAsString() + userInput;
        return fullPath;
    }

    /**
     * Checks if the provided fileName denotes an absolute path on the file
     * system. On Windows, this includes both paths with and without drive
     * letters, where the latter have to start with '\'. No check is performed
     * to see if the file actually exists!
     * 
     * @param fileName name of a file, which could be an absolute path
     * @return true if the fileName looks like an absolute path for the current
     *         OS
     */
    private boolean denotesAbsolutePath(final String fileName) {
        if (SystemUtils.IS_OS_WINDOWS) {
            // first check for drive letter
            if (WINDOWS_DRIVE_PATH.matcher(fileName).matches()) {
                return true;
            }
        }
        return fileName.startsWith(File.separator);
    }

    public boolean getAllPossibleValues(final List<Completion> completions,
            final Class<?> requiredType, final String originalUserInput,
            final String optionContext, final MethodTarget target) {
        String adjustedUserInput = convertUserInputIntoAFullyQualifiedPath(originalUserInput);

        final String directoryData = adjustedUserInput.substring(0,
                adjustedUserInput.lastIndexOf(File.separator) + 1);
        adjustedUserInput = adjustedUserInput.substring(adjustedUserInput
                .lastIndexOf(File.separator) + 1);

        populate(completions, adjustedUserInput, originalUserInput,
                directoryData);

        return false;
    }

    /**
     * @return the "current working directory" this {@link FileConverter} should
     *         use if the user fails to provide an explicit directory in their
     *         input (required)
     */
    protected abstract File getWorkingDirectory();

    private String getWorkingDirectoryAsString() {
        try {
            return getWorkingDirectory().getCanonicalPath() + File.separator;
        }
        catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    protected void populate(final List<Completion> completions,
            final String adjustedUserInput, final String originalUserInput,
            final String directoryData) {
        final File directory = new File(directoryData);

        if (!directory.isDirectory()) {
            return;
        }

        for (final File file : directory.listFiles()) {
            if (adjustedUserInput == null
                    || adjustedUserInput.length() == 0
                    || file.getName().toLowerCase()
                            .startsWith(adjustedUserInput.toLowerCase())) {

                String completion = "";
                if (directoryData.length() > 0) {
                    completion += directoryData;
                }
                completion += file.getName();

                completion = convertCompletionBackIntoUserInputStyle(
                        originalUserInput, completion);

                if (file.isDirectory()) {
                    completions
                            .add(new Completion(completion + File.separator));
                }
                else {
                    completions.add(new Completion(completion));
                }
            }
        }
    }

    public boolean supports(final Class<?> requiredType,
            final String optionContext) {
        return File.class.isAssignableFrom(requiredType);
    }
}