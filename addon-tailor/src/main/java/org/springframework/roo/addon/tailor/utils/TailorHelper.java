package org.springframework.roo.addon.tailor.utils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.roo.addon.tailor.CommandTransformation;
import org.springframework.roo.addon.tailor.actions.ActionConfig;

/**
 * Helper static operations
 * 
 * @author Birgitta Boeckeler
 * @author Vladimir Tihomirov
 */
public class TailorHelper {

    /**
     * Pattern to look for ${xxx} usage in a command string
     */
    private static final Pattern varPattern = Pattern
            .compile("\\$\\{([\\w\\*]*)\\}");

    /**
     * Looks for ${xxx} pattern in {@link ActionConfig#getCommand()} and
     * replaces those placeholders with the respective values from the
     * inputCommand's arguments.
     * 
     * @param trafo The CommandTransformation instance with the inputCommand to
     *            use to extract the values
     * @param text A string with potential occurrences of placeholders
     * @return The new command string
     */
    public static String replaceVars(CommandTransformation trafo, String text) {
        /*
         * TODO: This could also be done the other way around: iterate over all
         * arguments of the input command and replace the corresponding ${}
         * occurrences. >> Think about which makes more sense.
         */
        Map<String, String> inputArguments = trafo.getArguments();

        Matcher matcher = varPattern.matcher(text);
        while (matcher.find()) {
            // Placeholder name between ${}
            String placeholder = matcher.group(1);
            String inputValue = null;
            if ("*".equals(placeholder)) {
                // In this case, take the last fragment of the original command
                // that is not defined with "--" > assumed this is the first
                // argument,
                // which can sometimes be given without a "--" name
                String[] split = inputArguments.get("").split(" ");
                inputValue = split[split.length - 1];
            }
            else {
                inputValue = inputArguments.get(placeholder);
            }
            if (inputValue != null) {
                // Escape the special characters to ensure correct replacement
                String replace = matcher.group().replace("$", "\\$");
                replace = replace.replace("{", "\\{");
                replace = replace.replace("}", "\\}");
                replace = replace.replace("*", "\\*");
                // Do the actual replacement
                text = text.replaceAll(replace, inputValue);
            }
        }
        return text;
    }

}
