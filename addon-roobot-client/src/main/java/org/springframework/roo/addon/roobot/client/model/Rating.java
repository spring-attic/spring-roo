package org.springframework.roo.addon.roobot.client.model;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Star ratings for the "addon feedback bundle" command.
 * 
 * @author Ben Alex
 * @author Stefan Schmidt
 * @since 1.1.1
 */
public enum Rating {
    BAD(2), GOOD(4), NEUTRAL(3), VERY_BAD(1), VERY_GOOD(5);

    public static Rating fromInt(final Integer rating) {
        switch (rating) {
        case 1:
            return VERY_BAD;
        case 2:
            return BAD;
        case 3:
            return NEUTRAL;
        case 4:
            return GOOD;
        case 5:
            return VERY_GOOD;
        default:
            return NEUTRAL;
        }
    }

    private Integer key;

    private Rating(final Integer key) {
        this.key = key;
    }

    public Integer getKey() {
        return key;
    }

    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("name", name());
        builder.append("key", key);
        return builder.toString();
    }
}
