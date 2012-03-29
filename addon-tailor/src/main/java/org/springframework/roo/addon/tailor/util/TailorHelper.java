package org.springframework.roo.addon.tailor.util;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.roo.addon.tailor.CommandTransformation;
import org.springframework.roo.addon.tailor.actions.ActionConfig;

/**
 * Helper static operations.
 * 
 * @author Birgitta Boeckeler
 * @author Vladimir Tihomirov
 */
public class TailorHelper {

    /**
     * Pattern to look for ${xxx} usage in a command string
     */
    private static final Pattern VAR_PATTERN = Pattern
            .compile("\\$\\{([\\w\\*]*)\\}");

    public static void removeComment(final CommentedLine commentedLine) {
        String line = commentedLine.getLine();
        boolean inBlockComment = commentedLine.getInBlockComment();
        if (StringUtils.isBlank(line)) {
            return;
        }
        if (line.contains("/*")) {
            inBlockComment = true;
            final String lhs = line.substring(0, line.lastIndexOf("/*"));
            if (line.contains("*/")) {
                line = lhs + line.substring(line.lastIndexOf("*/") + 2);
                inBlockComment = false;
            }
            else {
                line = lhs;
            }
        }
        else if (inBlockComment && line.contains("*/")) {
            line = line.substring(line.lastIndexOf("*/") + 2);
            inBlockComment = false;
        }
        else if (inBlockComment) {
            line = "";
        }
        else if (line.trim().startsWith("//") || line.trim().startsWith("#")) {
            line = "";
        }
        commentedLine.setLine(line.replace('\t', ' '));
        commentedLine.setInBlockComment(inBlockComment);
    }

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
    public static String replaceVars(final CommandTransformation trafo,
            String text) {
        /*
         * TODO: This could also be done the other way around: iterate over all
         * arguments of the input command and replace the corresponding ${}
         * occurrences. >> Think about which makes more sense.
         */
        final Map<String, String> inputArguments = trafo.getArguments();
        if (inputArguments == null || inputArguments.isEmpty()) {
            return text;
        }

        final Matcher matcher = VAR_PATTERN.matcher(text);
        while (matcher.find()) {
            // Placeholder name between ${}
            final String placeholder = matcher.group(1);
            String inputValue = null;
            if ("*".equals(placeholder)) {
                // In this case, take the last fragment of the original command
                // that is not defined with "--" > assumed this is the first
                // argument,
                // which can sometimes be given without a "--" name
                final String[] split = inputArguments.get("").split(" ");
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
