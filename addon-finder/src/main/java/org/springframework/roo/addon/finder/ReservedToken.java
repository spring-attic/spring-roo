package org.springframework.roo.addon.finder;

import org.apache.commons.lang3.Validate;

/**
 * A reserved token is a reserved word which is used as part of a JPA compliant
 * SQL query.
 * 
 * @author Stefan Schmidt
 * @author Ben Alex
 * @since 1.0
 */
public class ReservedToken implements Token, Comparable<ReservedToken> {

    private final String value;

    /**
     * Create an instance of the {@link ReservedToken}
     * 
     * @param token the String token.
     */
    public ReservedToken(final String token) {
        Validate.notBlank(token, "Reserved token required");
        value = token;
    }

    public int compareTo(final ReservedToken o) {
        final int l = o.getValue().length() - getValue().length();
        return l == 0 ? -1 : l;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ReservedToken other = (ReservedToken) obj;
        if (value == null) {
            if (other.value != null) {
                return false;
            }
        }
        else if (!value.equals(other.value)) {
            return false;
        }
        return true;
    }

    public String getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (value == null ? 0 : value.hashCode());
        return result;
    }
}