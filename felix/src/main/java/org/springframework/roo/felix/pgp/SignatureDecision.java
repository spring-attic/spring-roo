package org.springframework.roo.felix.pgp;

import java.io.InputStream;

import org.bouncycastle.openpgp.PGPSignature;

/**
 * Represents the result of a signature verification via {@link PgpService#isSignatureAcceptable(InputStream)}.
 * 
 * @author Ben Alex
 * @since 1.1
 *
 */
public class SignatureDecision {
	private PGPSignature pgpSignature;
	private String signatureAsHex;
	private boolean signatureAcceptable;
	
	public SignatureDecision(PGPSignature pgpSignature, String signatureAsHex, boolean signatureAcceptable) {
		this.pgpSignature = pgpSignature;
		this.signatureAsHex = signatureAsHex;
		this.signatureAcceptable = signatureAcceptable;
	}

	public PGPSignature getPgpSignature() {
		return pgpSignature;
	}

	public String getSignatureAsHex() {
		return signatureAsHex;
	}

	public boolean isSignatureAcceptable() {
		return signatureAcceptable;
	}
	
}
