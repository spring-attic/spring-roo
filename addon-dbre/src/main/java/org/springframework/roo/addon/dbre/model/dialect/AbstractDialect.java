package org.springframework.roo.addon.dbre.model.dialect;

/**
 * Abstract base class for database {@link Dialect}s.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public abstract class AbstractDialect {

    public AbstractDialect() {
        super();
    }

    public boolean supportsSequences() {
        return true;
    }
}