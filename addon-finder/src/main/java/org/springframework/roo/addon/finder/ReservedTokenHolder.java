package org.springframework.roo.addon.finder;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Contains utility methods to return {@link SortedSet}s of
 * {@link ReservedToken}s.
 * <p>
 * Collections available through this class are immutable (non-modifiable).
 * 
 * @author Stefan Schmidt
 * @since 1.0;
 */
public abstract class ReservedTokenHolder {

    public static final SortedSet<ReservedToken> ALL_TOKENS;
    public static final SortedSet<ReservedToken> BOOLEAN_TOKENS;
    public static final SortedSet<ReservedToken> NUMERIC_TOKENS;
    public static final SortedSet<ReservedToken> STRING_TOKENS;

    static {
        NUMERIC_TOKENS = Collections.unmodifiableSortedSet(getNumericTokens());
        BOOLEAN_TOKENS = Collections.unmodifiableSortedSet(getBooleanTokens());
        STRING_TOKENS = Collections.unmodifiableSortedSet(getStringTokens());
        ALL_TOKENS = Collections.unmodifiableSortedSet(getAllTokens());
    }

    private static SortedSet<ReservedToken> getAllTokens() {
        final SortedSet<ReservedToken> reservedTokens = new TreeSet<ReservedToken>();
        reservedTokens.add(new ReservedToken("Or"));
        reservedTokens.add(new ReservedToken("And"));
        reservedTokens.add(new ReservedToken("Not"));
        reservedTokens.add(new ReservedToken("Like"));
        reservedTokens.add(new ReservedToken("Ilike")); // Ignore case like
        reservedTokens.add(new ReservedToken("LessThanEquals"));
        reservedTokens.add(new ReservedToken("IsNull"));
        reservedTokens.add(new ReservedToken("Equals"));
        reservedTokens.add(new ReservedToken("Between"));
        reservedTokens.add(new ReservedToken("LessThan"));
        reservedTokens.add(new ReservedToken("NotEquals"));
        reservedTokens.add(new ReservedToken("IsNotNull"));
        reservedTokens.add(new ReservedToken("GreaterThan"));
        reservedTokens.add(new ReservedToken("GreaterThanEquals"));
        reservedTokens.add(new ReservedToken("Member"));
        return reservedTokens;
    }

    private static SortedSet<ReservedToken> getBooleanTokens() {
        final SortedSet<ReservedToken> booleanTokens = new TreeSet<ReservedToken>();
        booleanTokens.add(new ReservedToken("Not"));
        return booleanTokens;
    }

    private static SortedSet<ReservedToken> getNumericTokens() {
        final SortedSet<ReservedToken> numericTokens = new TreeSet<ReservedToken>();
        numericTokens.add(new ReservedToken("Between"));
        numericTokens.add(new ReservedToken("LessThan"));
        numericTokens.add(new ReservedToken("LessThanEquals"));
        numericTokens.add(new ReservedToken("GreaterThan"));
        numericTokens.add(new ReservedToken("GreaterThanEquals"));
        numericTokens.add(new ReservedToken("IsNotNull"));
        numericTokens.add(new ReservedToken("IsNull"));
        numericTokens.add(new ReservedToken("NotEquals"));
        numericTokens.add(new ReservedToken("Equals"));
        return numericTokens;
    }

    private static SortedSet<ReservedToken> getStringTokens() {
        final SortedSet<ReservedToken> stringTokens = new TreeSet<ReservedToken>();
        stringTokens.add(new ReservedToken("Equals"));
        stringTokens.add(new ReservedToken("NotEquals"));
        stringTokens.add(new ReservedToken("Like"));
        stringTokens.add(new ReservedToken("IsNotNull"));
        stringTokens.add(new ReservedToken("IsNull"));
        return stringTokens;
    }
}
