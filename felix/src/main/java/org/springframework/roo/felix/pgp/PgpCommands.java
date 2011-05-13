package org.springframework.roo.felix.pgp;

import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TimeZone;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

/**
 * Enables a user to manage the Roo PGP keystore.
 * 
 * @author Ben Alex
 * @since 1.1
 *
 */
@Service
@Component
public class PgpCommands implements CommandMarker {
	@Reference PgpService pgpService;

	@CliCommand(value="pgp status", help="Displays the status of the PGP environment")
	public String pgpStatus() {
		List<PGPPublicKeyRing> keyRings = pgpService.getTrustedKeys();
		StringBuilder sb = new StringBuilder();
		appendLine(sb, "File: " + pgpService.getKeyStorePhysicalLocation());
		appendLine(sb, "Automatic trust: " + (pgpService.isAutomaticTrust() ? "enabled" : "disabled"));
		appendLine(sb, "Key count: " + keyRings.size());
		return sb.toString();
	}
	
	@CliCommand(value="pgp list trusted keys", help="Lists the keys you currently trust and have not been revoked at the time last downloaded from a public key server")
	public String listTrustedKeys() {
		List<PGPPublicKeyRing> keyRings = pgpService.getTrustedKeys();
		if (keyRings.size() == 0) {
			StringBuilder sb = new StringBuilder();
			appendLine(sb, "No keys trusted; use 'pgp trust' to add a key or 'pgp automatic trust' for automatic key addition");
			return sb.toString();
		}
		StringBuilder sb = new StringBuilder();
		for (PGPPublicKeyRing keyRing : keyRings) {
            formatKeyRing(sb, keyRing);
		}
		return sb.toString();
	}
	
	@CliCommand(value="pgp refresh all", help="Refreshes all keys from public key servers")
	public String refreshKeysFromServer() {
		StringBuilder sb = new StringBuilder();
		SortedMap<PgpKeyId,String> refreshOutcome = pgpService.refresh();
		for (PgpKeyId key : refreshOutcome.keySet()) {
			String outcome = refreshOutcome.get(key);
			appendLine(sb, key + " : " + outcome);
		}
		return sb.toString();
	}
	
	@CliCommand(value="pgp automatic trust", help="Indicates to automatically trust all keys encountered until the command is invoked again")
	public String automaticTrust() {
		if (pgpService.isAutomaticTrust()) {
			pgpService.setAutomaticTrust(false);
			return "Automatic PGP key trusting disabled (this is the safest option)";
		}
		pgpService.setAutomaticTrust(true);
		return "Automatic PGP key trusting enabled (this is potentially unsafe); disable by typing 'pgp automatic trust' again";
	}
	
	@CliCommand(value="pgp untrust", help="Revokes your trust for a particular key ID")
	public String untrust(@CliOption(key="keyId", mandatory=true, help="The key ID to remove trust from (eg 00B5050F or 0x00B5050F)") PgpKeyId keyId) {
		PGPPublicKeyRing keyRing = pgpService.untrust(keyId);
		StringBuilder sb = new StringBuilder();
        appendLine(sb, "Revoked trust from key:");
		formatKeyRing(sb, keyRing);
		return sb.toString();
	}
	
	@CliCommand(value="pgp key view", help="Downloads a remote key and displays it to the user (does not change any trusts)")
	public String keyView(@CliOption(key="keyId", mandatory=true, help="The key ID to view (eg 00B5050F or 0x00B5050F)") PgpKeyId keyId) {
		PGPPublicKeyRing keyRing = pgpService.getPublicKey(keyId);
		StringBuilder sb = new StringBuilder();
		formatKeyRing(sb, keyRing);
		return sb.toString();
	}

	@CliCommand(value="pgp trust", help="Grants trust to a particular key ID")
	public String trust(@CliOption(key="keyId", mandatory=true, help="The key ID to trust (eg 00B5050F or 0x00B5050F)") PgpKeyId keyId) {
		PGPPublicKeyRing keyRing = pgpService.trust(keyId);
		StringBuilder sb = new StringBuilder();
        appendLine(sb, "Added trust for key:");
		formatKeyRing(sb, keyRing);
		return sb.toString();
	}
	
	@SuppressWarnings("unchecked")
	private String getKeySummaryIfPossible(PgpKeyId keyId) {
		List<PGPPublicKeyRing> keyRings = pgpService.getTrustedKeys();
		
		for (PGPPublicKeyRing keyRing : keyRings) {
			Iterator<PGPPublicKey> it = keyRing.getPublicKeys();
			while (it.hasNext()) {
			    PGPPublicKey pgpKey = it.next();
		        if (new PgpKeyId(pgpKey.getKeyID()).equals(keyId)) {
		        	// We know about this key, so return a one-liner
		        	StringBuilder sb = new StringBuilder();
		        	sb.append("Key ").append(keyId).append(" (");
		        	Iterator<String> userIds = pgpKey.getUserIDs();
		        	if (userIds.hasNext()) {
		        		String userId = userIds.next();
		        		sb.append(userId);
		        	} else {
		        		sb.append("no user ID in key");
		        	}
		        	sb.append(")");
		        	return sb.toString();
		        }
			}
		}
		
		return "Key " + keyId + " - not locally trusted";
	}
	
	@SuppressWarnings("unchecked")
	private void formatKeyRing(StringBuilder sb, PGPPublicKeyRing keyRing) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss Z");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		Iterator<PGPPublicKey> it = keyRing.getPublicKeys();
		boolean first = true;
		while (it.hasNext()) {
		    PGPPublicKey pgpKey = it.next();
		    if (first) {
		        appendLine(sb, ">>>> KEY ID: " + new PgpKeyId(pgpKey) + " <<<<");
			    appendLine(sb, "     More Info: " + pgpService.getKeyServerUrlToRetrieveKeyInformation(new PgpKeyId(pgpKey)));
			    appendLine(sb, "     Created: " + sdf.format(pgpKey.getCreationTime()));
			    appendLine(sb, "     Fingerprint: " + new String(Hex.encode(pgpKey.getFingerprint())));
			    appendLine(sb, "     Algorithm: " + getAlgorithm(pgpKey.getAlgorithm()));
			    Iterator<String> userIdIterator = pgpKey.getUserIDs();
			    while (userIdIterator.hasNext()) {
			    	String userId = userIdIterator.next();
				    appendLine(sb, "     User ID: " + userId);
				    Iterator<PGPSignature> signatureIterator = pgpKey.getSignaturesForID(userId);
				    while (signatureIterator.hasNext()) {
				    	PGPSignature signature = signatureIterator.next();
					    appendLine(sb, "          Signed By: " + getKeySummaryIfPossible(new PgpKeyId(signature)));
				    }
			    }
			    
		        first = false;
		    } else {
		        appendLine(sb, "     Subkey ID: " + new PgpKeyId(pgpKey) + " [" + getAlgorithm(pgpKey.getAlgorithm()) + "]");
		    }
		}
	}

	private static String getAlgorithm(int algId) {
        switch (algId) {
        case PublicKeyAlgorithmTags.RSA_GENERAL:
            return "RSA_GENERAL";
        case PublicKeyAlgorithmTags.RSA_ENCRYPT:
            return "RSA_ENCRYPT";
        case PublicKeyAlgorithmTags.RSA_SIGN:
            return "RSA_SIGN";
        case PublicKeyAlgorithmTags.ELGAMAL_ENCRYPT:
            return "ELGAMAL_ENCRYPT";
        case PublicKeyAlgorithmTags.DSA:
            return "DSA";
        case PublicKeyAlgorithmTags.EC:
            return "EC";
        case PublicKeyAlgorithmTags.ECDSA:
            return "ECDSA";
        case PublicKeyAlgorithmTags.ELGAMAL_GENERAL:
            return "ELGAMAL_GENERAL";
        case PublicKeyAlgorithmTags.DIFFIE_HELLMAN:
            return "DIFFIE_HELLMAN";
        }
        return "unknown";
    }
	
	private void appendLine(StringBuilder sb, String line) {
		sb.append(line).append(System.getProperty("line.separator"));
	}
}
