package org.springframework.roo.support.ant;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.text.StrTokenizer;

/**
 * PathMatcher implementation for Ant-style path patterns. Examples are provided
 * below.
 * 
 * @author Alef Arendsen
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 16.07.2003
 */
public class AntPathMatcher implements PathMatcher {

    /** Default path separator: "/" */
    public static final String DEFAULT_PATH_SEPARATOR = "/";

    private String pathSeparator = DEFAULT_PATH_SEPARATOR;

    /**
     * Actually match the given <code>path</code> against the given
     * <code>pattern</code>.
     * 
     * @param pattern the pattern to match against
     * @param path the path String to test
     * @param fullMatch whether a full pattern match is required (else a pattern
     *            match as far as the given base path goes is sufficient)
     * @return <code>true</code> if the supplied <code>path</code> matched,
     *         <code>false</code> if it didn't
     */
    protected boolean doMatch(final String pattern, final String path,
            final boolean fullMatch,
            final Map<String, String> uriTemplateVariables) {
        if (path.startsWith(pathSeparator) != pattern.startsWith(pathSeparator)) {
            return false;
        }

        final String[] patternDirs = new StrTokenizer(pattern, pathSeparator)
                .setIgnoreEmptyTokens(true).getTokenArray();
        final String[] pathDirs = new StrTokenizer(path, pathSeparator)
                .setIgnoreEmptyTokens(true).getTokenArray();

        int pattIdxStart = 0;
        int pattIdxEnd = patternDirs.length - 1;
        int pathIdxStart = 0;
        int pathIdxEnd = pathDirs.length - 1;

        // Match all elements up to the first **
        while (pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd) {
            final String patDir = patternDirs[pattIdxStart];
            if ("**".equals(patDir)) {
                break;
            }
            if (!matchStrings(patDir, pathDirs[pathIdxStart],
                    uriTemplateVariables)) {
                return false;
            }
            pattIdxStart++;
            pathIdxStart++;
        }

        if (pathIdxStart > pathIdxEnd) {
            // Path is exhausted, only match if rest of pattern is * or **'s
            if (pattIdxStart > pattIdxEnd) {
                return pattern.endsWith(pathSeparator) ? path
                        .endsWith(pathSeparator) : !path
                        .endsWith(pathSeparator);
            }
            if (!fullMatch) {
                return true;
            }
            if (pattIdxStart == pattIdxEnd
                    && patternDirs[pattIdxStart].equals("*")
                    && path.endsWith(pathSeparator)) {
                return true;
            }
            for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
                if (!patternDirs[i].equals("**")) {
                    return false;
                }
            }
            return true;
        }
        else if (pattIdxStart > pattIdxEnd) {
            // String not exhausted, but pattern is. Failure.
            return false;
        }
        else if (!fullMatch && "**".equals(patternDirs[pattIdxStart])) {
            // Path start definitely matches due to "**" part in pattern.
            return true;
        }

        // Up to last '**'
        while (pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd) {
            final String patDir = patternDirs[pattIdxEnd];
            if (patDir.equals("**")) {
                break;
            }
            if (!matchStrings(patDir, pathDirs[pathIdxEnd],
                    uriTemplateVariables)) {
                return false;
            }
            pattIdxEnd--;
            pathIdxEnd--;
        }
        if (pathIdxStart > pathIdxEnd) {
            // String is exhausted
            for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
                if (!patternDirs[i].equals("**")) {
                    return false;
                }
            }
            return true;
        }

        while (pattIdxStart != pattIdxEnd && pathIdxStart <= pathIdxEnd) {
            int patIdxTmp = -1;
            for (int i = pattIdxStart + 1; i <= pattIdxEnd; i++) {
                if (patternDirs[i].equals("**")) {
                    patIdxTmp = i;
                    break;
                }
            }
            if (patIdxTmp == pattIdxStart + 1) {
                // '**/**' situation, so skip one
                pattIdxStart++;
                continue;
            }
            // Find the pattern between padIdxStart & padIdxTmp in str between
            // strIdxStart & strIdxEnd
            final int patLength = patIdxTmp - pattIdxStart - 1;
            final int strLength = pathIdxEnd - pathIdxStart + 1;
            int foundIdx = -1;

            strLoop: for (int i = 0; i <= strLength - patLength; i++) {
                for (int j = 0; j < patLength; j++) {
                    final String subPat = patternDirs[pattIdxStart + j + 1];
                    final String subStr = pathDirs[pathIdxStart + i + j];
                    if (!matchStrings(subPat, subStr, uriTemplateVariables)) {
                        continue strLoop;
                    }
                }
                foundIdx = pathIdxStart + i;
                break;
            }

            if (foundIdx == -1) {
                return false;
            }

            pattIdxStart = patIdxTmp;
            pathIdxStart = foundIdx + patLength;
        }

        for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
            if (!patternDirs[i].equals("**")) {
                return false;
            }
        }

        return true;
    }

    /**
     * Given a pattern and a full path, determine the pattern-mapped part.
     * <p>
     * For example:
     * <ul>
     * <li>'<code>/docs/cvs/commit.html</code>' and '
     * <code>/docs/cvs/commit.html</code> -> ''</li>
     * <li>'<code>/docs/*</code>' and '<code>/docs/cvs/commit</code> -> '
     * <code>cvs/commit</code>'</li>
     * <li>'<code>/docs/cvs/*.html</code>' and '
     * <code>/docs/cvs/commit.html</code> -> '<code>commit.html</code>'</li>
     * <li>'<code>/docs/**</code>' and '<code>/docs/cvs/commit</code> -> '
     * <code>cvs/commit</code>'</li>
     * <li>'<code>/docs/**\/*.html</code>' and '
     * <code>/docs/cvs/commit.html</code> -> '<code>cvs/commit.html</code>'</li>
     * <li>'<code>/*.html</code>' and '<code>/docs/cvs/commit.html</code> -> '
     * <code>docs/cvs/commit.html</code>'</li>
     * <li>'<code>*.html</code>' and '<code>/docs/cvs/commit.html</code> -> '
     * <code>/docs/cvs/commit.html</code>'</li>
     * <li>'<code>*</code>' and '<code>/docs/cvs/commit.html</code> -> '
     * <code>/docs/cvs/commit.html</code>'</li>
     * </ul>
     * <p>
     * Assumes that {@link #match} returns <code>true</code> for '
     * <code>pattern</code>' and '<code>path</code>', but does
     * <strong>not</strong> enforce this.
     */
    public String extractPathWithinPattern(final String pattern,
            final String path) {
        final String[] patternParts = new StrTokenizer(pattern, pathSeparator)
                .setIgnoreEmptyTokens(true).getTokenArray();
        final String[] pathParts = new StrTokenizer(path, pathSeparator)
                .setIgnoreEmptyTokens(true).getTokenArray();

        final StringBuilder builder = new StringBuilder();

        // Add any path parts that have a wildcarded pattern part.
        int puts = 0;
        for (int i = 0; i < patternParts.length; i++) {
            final String patternPart = patternParts[i];
            if ((patternPart.indexOf('*') > -1 || patternPart.indexOf('?') > -1)
                    && pathParts.length >= i + 1) {
                if (puts > 0 || i == 0 && !pattern.startsWith(pathSeparator)) {
                    builder.append(pathSeparator);
                }
                builder.append(pathParts[i]);
                puts++;
            }
        }

        // Append any trailing path parts.
        for (int i = patternParts.length; i < pathParts.length; i++) {
            if (puts > 0 || i > 0) {
                builder.append(pathSeparator);
            }
            builder.append(pathParts[i]);
        }

        return builder.toString();
    }

    public Map<String, String> extractUriTemplateVariables(
            final String pattern, final String path) {
        final Map<String, String> variables = new LinkedHashMap<String, String>();
        final boolean result = doMatch(pattern, path, true, variables);
        Validate.validState(result, "Pattern \"" + pattern
                + "\" is not a match for \"" + path + "\"");
        return variables;
    }

    public boolean isPattern(final String path) {
        return path.indexOf('*') != -1 || path.indexOf('?') != -1;
    }

    public boolean match(final String pattern, final String path) {
        return doMatch(pattern, path, true, null);
    }

    public boolean matchStart(final String pattern, final String path) {
        return doMatch(pattern, path, false, null);
    }

    /**
     * Tests whether or not a string matches against a pattern. The pattern may
     * contain two special characters:<br>
     * '*' means zero or more characters<br>
     * '?' means one and only one character
     * 
     * @param pattern pattern to match against. Must not be <code>null</code>.
     * @param str string which must be matched against the pattern. Must not be
     *            <code>null</code>.
     * @return <code>true</code> if the string matches against the pattern, or
     *         <code>false</code> otherwise.
     */
    private boolean matchStrings(final String pattern, final String str,
            final Map<String, String> uriTemplateVariables) {
        final AntPatchStringMatcher matcher = new AntPatchStringMatcher(
                pattern, str, uriTemplateVariables);
        return matcher.matchStrings();
    }

    /**
     * Set the path separator to use for pattern parsing. Default is "/", as in
     * Ant.
     */
    public void setPathSeparator(final String pathSeparator) {
        this.pathSeparator = pathSeparator != null ? pathSeparator
                : DEFAULT_PATH_SEPARATOR;
    }
}
