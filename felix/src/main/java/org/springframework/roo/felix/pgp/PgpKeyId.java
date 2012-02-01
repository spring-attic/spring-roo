package org.springframework.roo.felix.pgp;

import org.apache.commons.lang3.Validate;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSignature;

/**
 * Represents a 10 digit hexadecimal key ID (always starts with 0x, and the rest
 * of the ID is uppercase).
 * 
 * @author Ben Alex
 * @since 1.1
 */
public class PgpKeyId implements Comparable<PgpKeyId> {
    private static final long MASK = 0xFFFFFFFFL;
    private String id;

    public PgpKeyId(final long keyId) {
        id = "0x" + String.format("%08X", MASK & keyId);
    }

    public PgpKeyId(final PGPPublicKey keyId) {
        Validate.notNull(keyId, "Key ID required");
        id = "0x" + String.format("%08X", MASK & keyId.getKeyID());
    }

    public PgpKeyId(final PGPSignature signature) {
        Validate.notNull(signature, "Signautre required");
        id = "0x" + String.format("%08X", MASK & signature.getKeyID());
    }

    public PgpKeyId(String keyId) {
        Validate.notBlank(keyId,
                "A key ID is required (eg 00B5050F or 0x00B5050F)");
        if (keyId.length() == 10) {
            Validate.isTrue(keyId.toLowerCase().startsWith("0x"),
                    "10 character key IDs must start with 0x");
            keyId = keyId.toUpperCase(); // NB: the 0x will become uppercase,
                                         // which it shouldn't
            id = "0x" + keyId.substring(2);
        }
        else if (keyId.length() == 8) {
            Validate.isTrue(!keyId.toLowerCase().startsWith("0x"),
                    "8 character key IDs must not start with 0x");
            keyId = keyId.toUpperCase();
            id = "0x" + keyId;
        }
        else {
            throw new IllegalStateException(
                    "The key ID must be in a valid form (eg 00B5050F or 0x00B5050F)");
        }
    }

    public int compareTo(final PgpKeyId o) {
        if (o == null) {
            return -1;
        }
        return id.compareTo(o.id);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof PgpKeyId) {
            return id.equals(((PgpKeyId) obj).id);
        }
        return false;
    }

    public String getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
