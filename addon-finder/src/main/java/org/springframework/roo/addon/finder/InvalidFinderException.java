package org.springframework.roo.addon.finder;

/**
 * Thrown when a dynamic finder method is invalid.
 * 
 * @author Stefan Schmidt
 * @since 1.1.2
 */
public class InvalidFinderException extends RuntimeException {

    private static final long serialVersionUID = 2328865678880608749L;

    public InvalidFinderException(final String string) {
        super(string);
    }
}
