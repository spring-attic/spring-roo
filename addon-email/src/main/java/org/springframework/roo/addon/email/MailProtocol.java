package org.springframework.roo.addon.email;

import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Protocols known to the email add-on.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
public class MailProtocol implements Comparable<MailProtocol> {
	public static final MailProtocol SMTP = new MailProtocol("SMTP", "smtp");
	public static final MailProtocol POP3 = new MailProtocol("POP3", "pop3");
	public static final MailProtocol IMAP = new MailProtocol("IMAP", "imap");

	private String protocolLabel;
	private String protocol;

	public MailProtocol(String protocolLabel, String protocol) {
		Assert.notNull(protocolLabel, "Protocol label required");
		Assert.notNull(protocol, "protocol required");
		this.protocolLabel = protocolLabel;
		this.protocol = protocol;
	}

	public String getProtocol() {
		return protocol;
	}

	public final boolean equals(Object obj) {
		return obj != null && obj instanceof MailProtocol && this.compareTo((MailProtocol) obj) == 0;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((protocolLabel == null) ? 0 : protocolLabel.hashCode());
		return result;
	}

	public final int compareTo(MailProtocol o) {
		if (o == null) return -1;
		int result = this.protocolLabel.compareTo(o.protocolLabel);

		return result;
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("provider", protocolLabel);
		return tsc.toString();
	}

	public String getKey() {
		return this.protocolLabel;
	}
}