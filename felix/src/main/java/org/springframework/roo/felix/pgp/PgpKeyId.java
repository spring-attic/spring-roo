package org.springframework.roo.felix.pgp;

import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSignature;
import org.springframework.roo.support.util.Assert;

/**
 * Represents a 10 digit hexadecimal key ID (always starts with 0x, and the rest of the ID is uppercase).
 * 
 * @author Ben Alex
 * @since 1.1
 * 
 */
public class PgpKeyId implements Comparable<PgpKeyId> {
	private static final long MASK = 0xFFFFFFFFL;
	private String id;

    public PgpKeyId(long keyId) {
		id = "0x" + String.format("%08X", (MASK & keyId));
	}

    public PgpKeyId(PGPPublicKey keyId) {
    	Assert.notNull(keyId, "Key ID required");
		id = "0x" + String.format("%08X", (MASK & keyId.getKeyID()));
    }
    
    public PgpKeyId(PGPSignature signature) {
    	Assert.notNull(signature, "Signautre required");
		id = "0x" + String.format("%08X", (MASK & signature.getKeyID()));
    }
    
    public PgpKeyId(String keyId) {
		Assert.hasText(keyId, "A key ID is required (eg 00B5050F or 0x00B5050F)");
		if (keyId.length() == 10) {
			Assert.isTrue(keyId.toLowerCase().startsWith("0x"), "10 character key IDs must start with 0x");
			keyId = keyId.toUpperCase(); // NB: the 0x will become uppercase, which it shouldn't
			id = "0x" + keyId.substring(2);
		} else if (keyId.length() == 8) {
			Assert.isTrue(!keyId.toLowerCase().startsWith("0x"), "8 character key IDs must not start with 0x");
			keyId = keyId.toUpperCase();
			id = "0x" + keyId;
		} else {
			throw new IllegalStateException("The key ID must be in a valid form (eg 00B5050F or 0x00B5050F)");
		}
	}
    
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PgpKeyId) {
			return id.equals(((PgpKeyId) obj).id);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public String toString() {
		return id.toString();
	}

	public int compareTo(PgpKeyId o) {
		if (o == null) return -1;
		return id.compareTo(o.id);
	}

	public String getId() {
		return id;
	}
}
