package org.springframework.roo.felix.pgp;

import java.io.InputStream;

import org.bouncycastle.openpgp.PGPSignature;
import org.springframework.roo.support.util.Assert;

/**
 * Represents the result of a signature verification via
 * {@link PgpService#isSignatureAcceptable(InputStream)}.
 * 
 * @author Ben Alex
 * @since 1.1
 */
public class SignatureDecision {
    private final PGPSignature pgpSignature;
    private final PgpKeyId signatureAsHex;
    private final boolean signatureAcceptable;

    public SignatureDecision(final PGPSignature pgpSignature,
            final PgpKeyId signatureAsHex, final boolean signatureAcceptable) {
        Assert.notNull(pgpSignature, "PGP Signature required");
        Assert.notNull(signatureAsHex, "PGP Key ID required");
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
