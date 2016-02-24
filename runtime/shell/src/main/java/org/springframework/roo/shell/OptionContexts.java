package org.springframework.roo.shell;

/**
 * Common constants used in the option contexts of {@link CliOption}s.
 * 
 * @author Alan Stewart
 */
public final class OptionContexts {

    /**
     * If this string appears in an option context, a {@link Converter} will
     * return only interface types appearing in any module of the user's
     * project.
     */
    public static final String INTERFACE = "interface";

    /**
     * If this string appears in an option context, a {@link Converter} will
     * return types appearing in any module of the user's project.
     */
    public static final String PROJECT = "project";

    /**
     * If this string appears in an option context, this converter will return
     * non-final types appearing in any module of the user's project.
     */
    public static final String SUPERCLASS = "superclass";

    /**
     * If this string appears in an option context, this converter will update
     * the last used type and the focused module as applicable.
     */
    public static final String UPDATE = "update";

    public static final String UPDATE_PROJECT = "update,project";

    private OptionContexts() {
    }
}
