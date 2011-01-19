package org.springframework.roo.addon.finder;

import java.util.Collections;
import java.util.List;

import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;

/**
 * Bean to hold the JPA query string, the method parameter types and parameter names.
 * 
 * <p>
 * Immutable once constructed.
 * 
 * @author Alan Stewart
 * @since 1.1.2
 */
public class QueryHolder {
	private String jpaQuery;
	private List<JavaType> parameterTypes;
	private List<JavaSymbolName> parameterNames;
	private List<Token> tokens;

	public QueryHolder(String jpaQuery, List<JavaType> parameterTypes, List<JavaSymbolName> parameterNames, List<Token> tokens) {
		Assert.hasText(jpaQuery, "JPA query required");
		Assert.notNull(parameterTypes, "Parameter types required");
		Assert.notNull(parameterNames, "Parameter names required");
		Assert.notNull(tokens, "Tokens required");
		this.jpaQuery = jpaQuery;
		this.parameterTypes = Collections.unmodifiableList(parameterTypes);
		this.parameterNames = Collections.unmodifiableList(parameterNames);
		this.tokens = Collections.unmodifiableList(tokens);
	}

	public String getJpaQuery() {
		return jpaQuery;
	}

	public List<JavaType> getParameterTypes() {
		return parameterTypes;
	}

	public List<JavaSymbolName> getParameterNames() {
		return parameterNames;
	}

	/**
	 * Package protected as it is only intended for internal use.
	 * 
	 * @return the tokens used to process this query (used internally; immutable)
	 */
	List<Token> getTokens() {
		return tokens;
	}
}
