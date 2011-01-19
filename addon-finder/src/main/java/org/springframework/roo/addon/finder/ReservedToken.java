package org.springframework.roo.addon.finder;

import org.springframework.roo.support.util.Assert;

/**
 * A reserved token is a reserved word which is used as part of a JPA compliant SQL query.
 * 
 * @author Stefan Schmidt
 * @author Ben Alex
 * @since 1.0
 */
public class ReservedToken implements Token, Comparable<ReservedToken> {
	private String value;
	
	/**
	 * Create an instance of the {@link ReservedToken} 
	 * 
	 * @param token the String token.
	 */
	public ReservedToken(String token) {
		Assert.hasText(token, "Reserved token required");
		this.value = token;
	}
	
	public String getValue() {
		return value;
	}

	public int compareTo(ReservedToken o) {
		int l = o.getValue().length() - this.getValue().length();
		return l == 0 ? -1 : l;
	}

	@Override 
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
}