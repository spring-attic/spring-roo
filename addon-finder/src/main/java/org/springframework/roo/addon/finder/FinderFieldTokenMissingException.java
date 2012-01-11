package org.springframework.roo.addon.finder;

/**
 * Thrown when a dynamic finder method cannot be matched.
 * 
 * @author Alan Stewart
 * @since 1.1.2
 */
public class FinderFieldTokenMissingException extends RuntimeException {

    private static final long serialVersionUID = 2328865678880608749L;

    public FinderFieldTokenMissingException(final String string) {
        super(string);
    }
}
