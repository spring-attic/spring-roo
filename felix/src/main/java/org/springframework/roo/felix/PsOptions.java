package org.springframework.roo.felix;

import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Provides display formats for the Felix "ps" command.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class PsOptions implements Comparable<PsOptions> {

    public static final PsOptions BUNDLE_NAME = new PsOptions("BUNDLE_NAME", ""); // default
    public static final PsOptions LOCATION_PATH = new PsOptions(
            "LOCATION_PATH", " -l");

    public static final PsOptions SYMBOLIC_NAME = new PsOptions(
            "SYMBOLIC_NAME", " -s");
    public static final PsOptions UPDATE_PATH = new PsOptions("UPDATE_PATH",
            " -u");
    private final String felixCode;
    private final String key;

    public PsOptions(final String key, final String felixCode) {
        Assert.hasText(key, "Key required");
        Assert.notNull(felixCode, "Felix code required");
        this.key = key;
        this.felixCode = felixCode;
    }

    public final int compareTo(final PsOptions o) {
        if (o == null) {
            return -1;
        }
        final int result = key.compareTo(o.key);
        if (result == 0) {
            return felixCode.compareTo(o.felixCode);
        }
        return result;
    }

    @Override
    public final boolean equals(final Object obj) {
        return (obj instanceof PsOptions) && (compareTo((PsOptions) obj) == 0);
    }

    public String getFelixCode() {
        return felixCode;
    }

    public String getKey() {
        return key;
    }

    @Override
    public final int hashCode() {
        return key.hashCode() * felixCode.hashCode();
    }

    @Override
    public String toString() {
        final ToStringCreator tsc = new ToStringCreator(this);
        tsc.append("key", key);
        tsc.append("felixCode", felixCode);
        return tsc.toString();
    }
}
