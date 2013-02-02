package org.springframework.roo.classpath.details.comments;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Mike De Haan
 */
public class CommentFormatter {

    private static Pattern commentFormattingRegex = Pattern
            .compile("[\\s]*(.+)\\r?\\n?");

    /**
     * Format a given comment string with the indent level specified.
     * 
     * @param comment
     * @param indentLevel
     * @return
     */
    public String format(String comment, int indentLevel) {

        // Return if there's nothing to do
        if (comment == null) {
            return null;
        }

        final StringBuilder indentString = new StringBuilder();
        for (int i = 0; i < indentLevel; i++) {
            indentString.append("    ");
        }

        // Comment ends with newline
        boolean endsWithNewline = (comment.endsWith("\r\n") || comment
                .endsWith("\n"));

        List<String> matchList = new ArrayList<String>();
        Matcher regexMatcher = commentFormattingRegex.matcher(comment);
        while (regexMatcher.find()) {
            matchList.add(regexMatcher.group());
        }

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < matchList.size(); i++) {

            // We need to handle the last newline
            if (i == matchList.size() - 1) {
                builder.append(indentString.toString() + " "
                        + matchList.get(i).trim()
                        + (endsWithNewline ? "\n" : ""));
            }
            else if (i == 0) {
                builder.append(indentString.toString()
                        + matchList.get(i).trim() + "\n");
            }
            else {
                builder.append(indentString.toString() + " "
                        + matchList.get(i).trim() + "\n");
            }
        }

        return builder.toString();
    }

    /**
     * Formats a plain string (including newlines) and formats it as Javadoc.
     * 
     * @param input Plain string (including newlines) to be formatted as Javadoc
     * @return Formatted Javadoc
     */
    public String formatStringAsJavadoc(String input) {

        if (input == null) {
            return null;
        }

        final StringBuilder finalComment = new StringBuilder("/**\n");
        Matcher regexMatcher = commentFormattingRegex.matcher(input);
        while (regexMatcher.find()) {
            finalComment.append("* ");
            finalComment.append(regexMatcher.group());
        }
        finalComment.append("\n*/\n");

        return finalComment.toString();
    }
}
