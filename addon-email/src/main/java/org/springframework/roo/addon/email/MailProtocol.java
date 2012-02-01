package org.springframework.roo.addon.email;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Protocols known to the email add-on.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
public class MailProtocol implements Comparable<MailProtocol> {

    public static final MailProtocol IMAP = new MailProtocol("IMAP", "imap");
    public static final MailProtocol POP3 = new MailProtocol("POP3", "pop3");
    public static final MailProtocol SMTP = new MailProtocol("SMTP", "smtp");

    private final String protocol;
    private final String protocolLabel;

    public MailProtocol(final String protocolLabel, final String protocol) {
        Validate.notNull(protocolLabel, "Protocol label required");
        Validate.notNull(protocol, "protocol required");
        this.protocolLabel = protocolLabel;
        this.protocol = protocol;
    }

    public final int compareTo(final MailProtocol o) {
        if (o == null) {
            return -1;
        }
        final int result = protocolLabel.compareTo(o.protocolLabel);

        return result;
    }

    @Override
    public final boolean equals(final Object obj) {
        return obj instanceof MailProtocol
                && compareTo((MailProtocol) obj) == 0;
    }

    public String getKey() {
        return protocolLabel;
    }

    public String getProtocol() {
        return protocol;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + (protocolLabel == null ? 0 : protocolLabel.hashCode());
        return result;
    }

    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("provider", protocolLabel);
        return builder.toString();
    }
}