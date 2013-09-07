package org.springframework.roo.addon.finder;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Bean to hold the JPA query string, the method parameter types and parameter
 * names.
 * <p>
 * Immutable once constructed.
 * 
 * @author Alan Stewart
 * @since 1.1.2
 */
public class QueryHolder {

    private final String jpaQuery;
    private List<JavaSymbolName> parameterNames;
    private List<JavaType> parameterTypes;
    private final List<Token> tokens;

    public QueryHolder(final String jpaQuery,
            final List<JavaType> parameterTypes,
            final List<JavaSymbolName> parameterNames, final List<Token> tokens) {
        Validate.notBlank(jpaQuery, "JPA query required");
        Validate.notNull(parameterTypes, "Parameter types required");
        Validate.notNull(parameterNames, "Parameter names required");
        Validate.notNull(tokens, "Tokens required");
        this.jpaQuery = jpaQuery;
        this.parameterTypes = parameterTypes;
        this.parameterNames = parameterNames;
        this.tokens = Collections.unmodifiableList(tokens);
    }

    public String getJpaQuery() {
        return jpaQuery;
    }

    public List<JavaSymbolName> getParameterNames() {
        return parameterNames;
    }

    public List<JavaType> getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Package protected as it is only intended for internal use.
     * 
     * @return the tokens used to process this query (used internally;
     *         immutable)
     */
    List<Token> getTokens() {
        return tokens;
    }
}
