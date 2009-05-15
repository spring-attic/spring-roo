/**
 * 
 */
package org.springframework.roo.addon.finder;

import java.util.HashSet;
import java.util.Set;

import org.springframework.roo.support.util.Assert;

/**
 * A reserved token is a reserved word which is used as part of a JPA compliant SQL query.
 * 
 * @author Stefan Schmidt
 * @author Ben Alex
 * @since 1.0
 *
 */
public class ReservedToken implements Token {
	
	private static Set<String> reservedTokens = new HashSet<String>();
	
	private String token;
	
	/**
	 * Create an instance of the {@link ReservedToken} 
	 * 
	 * @param token This token must be defined as part of {@link ReservedToken#getAllReservedTokens()}
	 */
	public ReservedToken(String token){
		Assert.hasText(token, "Reserved token required");
		Assert.isTrue(getAllReservedTokens().contains(token), "Token not recognized");
		this.token = token;
	}
	
	public String getToken() {
		return token;
	}

	/**
	 * Use this to get a set of all possible keywords for boolean types. DO NOT use this 
	 * to to getBooleanReservedTokens().contains(token) tests as it will always return true. 
	 * Use getAllReservedTokens().contains(token) if you want to know if a given String is 
	 * a reserved keyword
	 * 
	 * @return The keywords that apply to boolean type.
	 */
	public static Set<String> getBooleanReservedTokens(){
		reservedTokens.clear();
		//applicable for boolean only
		reservedTokens.add("");
		reservedTokens.add("Not");
		return reservedTokens;
	}

	/**
	 * Use this to get a set of all possible keywords for numeric types. DO NOT use this 
	 * to to getNumericReservedTokens().contains(token) tests as it will always return true. 
	 * Use getAllReservedTokens().contains(token) if you want to know if a given String is 
	 * a reserved keyword
	 * 
	 * @return The keywords that apply to numeric type.
	 */
	public static Set<String> getNumericReservedTokens(){
		reservedTokens.clear();
		//applicable for date and number only
		reservedTokens.add("Between");
		reservedTokens.add("LessThan");
		reservedTokens.add("LessThanEquals");
		reservedTokens.add("GreaterThan");
		reservedTokens.add("GreaterThanEquals");
		reservedTokens.add("IsNotNull");
		reservedTokens.add("IsNull");
		reservedTokens.add("NotEquals");
		reservedTokens.add("Equals");
		reservedTokens.add("");
		return reservedTokens;
	}
	
	/**
	 * Use this to get a set of all possible keywords for literal types. DO NOT use this 
	 * to to getStringReservedTokens().contains(token) tests as it will always return true. 
	 * Use getAllReservedTokens().contains(token) if you want to know if a given String is 
	 * a reserved keyword
	 * 
	 * @return The keywords that apply to literal type.
	 */
	public static Set<String> getStringReservedTokens(){
		reservedTokens.clear();
		//applicable for date and number only
		
		//what would be the difference between findByName and findByNameEqual?
		reservedTokens.add("Equals");
		
		//what would be the difference between findByNameNot and findByNameNotEqual?
		reservedTokens.add("NotEquals");
		
		reservedTokens.add("Like");
//		reservedTokens.add("Ilike"); // ignore case like
		reservedTokens.add("IsNotNull");
		reservedTokens.add("IsNull");
		reservedTokens.add("");
		return reservedTokens;
	}
	
	/**
	 * Use this to get a set of all possible JPA keywords Use getAllReservedTokens().contains(token) 
	 * if you want to know if a given String is a reserved keyword
	 * 
	 * @return The keywords that are used in JPA.
	 */
	public static Set<String> getAllReservedTokens(){
		reservedTokens.clear();
		reservedTokens.add("And");
		reservedTokens.add("Or");
		reservedTokens.add("Not");
		reservedTokens.add("NotEquals");
		reservedTokens.add("Equals");
		reservedTokens.add("Like");
		reservedTokens.add("Ilike"); // ignore case like
		reservedTokens.add("IsNotNull");
		reservedTokens.add("IsNull");
		reservedTokens.add("Between");
		reservedTokens.add("LessThan");
		reservedTokens.add("LessThanEquals");
		reservedTokens.add("GreaterThan");
		reservedTokens.add("GreaterThanEquals");
		return reservedTokens;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((token == null) ? 0 : token.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ReservedToken other = (ReservedToken) obj;
		if (token == null) {
			if (other.token != null)
				return false;
		} else if (!token.equals(other.token))
			return false;
		return true;
	}
}