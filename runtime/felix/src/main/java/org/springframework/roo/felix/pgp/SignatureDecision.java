package org.springframework.roo.felix.pgp;

import java.io.InputStream;

import org.apache.commons.lang3.Validate;
import org.bouncycastle.openpgp.PGPSignature;

/**
 * Represents the result of a signature verification via
 * {@link PgpService#isSignatureAcceptable(InputStream)}.
 * 
 * @author Ben Alex
 * @since 1.1
 */
public class SignatureDecision {
    private final PGPSignature pgpSignature;
    private final boolean signatureAcceptable;
    private final PgpKeyId signatureAsHex;

    public SignatureDecision(final PGPSignature pgpSignature,
            final PgpKeyId signatureAsHex, final boolean signatureAcceptable) {
        Validate.notNull(pgpSignature, "PGP Signature required");
        Validate.notNull(signatureAsHex, "PGP Key ID required");
        this.pgpSignature = pgpSignature;
        this.signatureAsHex = signatureAsHex;
        this.signatureAcceptable = signatureAcceptable;
    }

    public PGPSignature getPgpSignature() {
        return pgpSignature;
    }

    public PgpKeyId getSignatureAsHex() {
        return signatureAsHex;
    }

    public boolean isSignatureAcceptable() {
        return signatureAcceptable;
    }

}
