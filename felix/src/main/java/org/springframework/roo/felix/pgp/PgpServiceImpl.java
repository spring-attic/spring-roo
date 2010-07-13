package org.springframework.roo.felix.pgp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Security;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.bouncycastle.bcpg.ArmoredInputStream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPUtil;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.support.osgi.UrlFindingUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.url.stream.UrlInputStreamService;

/**
 * Default implementation of {@link PgpService}.
 *
 * <p>
 * Stores the user's PGP information in the <code>~/.spring_roo_pgp.bpg<code> file. Every key in this
 * file is considered trusted by the user. Expiration times of keys are ignored. Default keys that
 * ship with Roo are added to this file automatically when the file is not present on disk.
 * 
 * <p>
 * This implementation will only verify "detached armored signatures". Produce such a file via
 * "gpg --armor --detach-sign file_to_sign.ext".
 * 
 * @author Ben Alex
 * @since 1.1
 *
 */
@Component
@Service
public class PgpServiceImpl implements PgpService {
	@Reference private UrlInputStreamService urlInputStreamService;
	private boolean automaticTrust = false;
	private BundleContext context;
	private static final File ROO_PGP_FILE = new File(System.getProperty("user.home") + File.separatorChar + ".spring_roo_pgp.bpg");
//	private static final String DEFAULT_KEYSERVER_URL = "http://pgpkeys.pca.dfn.de/pks/lookup?op=get&search=";
	private static final String DEFAULT_KEYSERVER_URL = "http://pgp.mit.edu:11371/pks/lookup?op=get&search=";
	private static final long MASK = 0xFFFFFFFFL;
    private static final int BUFFER_SIZE = 1024;
    
    static {
		Security.addProvider(new BouncyCastleProvider());
    }
    
    protected void activate(ComponentContext context) {
    	this.context = context.getBundleContext();
    	trustDefaultKeysIfRequired();
    }
    
    protected void trustDefaultKeysIfRequired() {
    	// Setup default keys we trust automatically if the user doesn't have a PGP file already
    	if (!ROO_PGP_FILE.exists()) {
    		trustDefaultKeys();
    	}
    }
    
    private void trustDefaultKeys() {
		Set<URL> urls = UrlFindingUtils.findMatchingClasspathResources(context, "/org/springframework/roo/felix/pgp/*.asc");
		
		SortedSet<URL> sortedUrls = new TreeSet<URL>(new Comparator<URL>() {
			public int compare(URL o1, URL o2) {
				return o1.toExternalForm().compareTo(o2.toExternalForm());
			}
		});
		sortedUrls.addAll(urls);
		
		for (URL url : sortedUrls) {
			try {
				PGPPublicKeyRing key = getPublicKey(url.openStream());
				trust(key);
			} catch (IOException ignore) {}
		}
    }
	public String getKeyStorePhysicalLocation() {
		try {
			return ROO_PGP_FILE.getCanonicalPath();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public boolean isAutomaticTrust() {
		return automaticTrust;
	}

	public void setAutomaticTrust(boolean automaticTrust) {
		this.automaticTrust = automaticTrust;
	}
	
	@SuppressWarnings("unchecked")
	public List<PGPPublicKeyRing> getTrustedKeys() {
		List<PGPPublicKeyRing> result = new ArrayList<PGPPublicKeyRing>();
		if (!ROO_PGP_FILE.exists()) {
			return result;
		}
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(ROO_PGP_FILE);
			PGPPublicKeyRingCollection pubRings = new PGPPublicKeyRingCollection(PGPUtil.getDecoderStream(fis));
	        Iterator<PGPPublicKeyRing> rIt = pubRings.getKeyRings();
	        while (rIt.hasNext()) {
	            PGPPublicKeyRing pgpPub = rIt.next();
	            result.add(pgpPub);
	        }
		} catch (Exception e) {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException ignore) {}
			}
			throw new IllegalArgumentException(e);
		}
		return result;
	}

	private String prepareKeyId(String keyId) {
		Assert.hasText(keyId, "Key ID is required");
		if (keyId.length() == 10) {
			Assert.isTrue(keyId.toLowerCase().startsWith("0x"), "10 character key IDs must start with 0x");
			keyId = keyId.toUpperCase(); // NB: the 0x will become uppercase, which it shouldn't
			return "0x" + keyId.substring(2);
		}
		if (keyId.length() == 8) {
			Assert.isTrue(!keyId.toLowerCase().startsWith("0x"), "8 character key IDs must not start with 0x");
			keyId = keyId.toUpperCase();
			return "0x" + keyId;
		}
		throw new IllegalStateException("A key ID is required (eg 00B5050F or 0x00B5050F)");
	}

	public PGPPublicKeyRing trust(String keyId) {
		PGPPublicKeyRing keyRing = getPublicKey(keyId);
		return trust(keyRing);
	}
	
	private PGPPublicKeyRing trust(PGPPublicKeyRing keyRing) {
		// get the keys we currently trust
		List<PGPPublicKeyRing> trusted = getTrustedKeys();
		
	    // Do not store if the first key is revoked
	    if (keyRing.getPublicKey().isRevoked()) {
	    	throw new IllegalStateException("The public key ID '" + getKeyId(keyRing.getPublicKey().getKeyID()) + "' has been revoked and cannot be trusted");
	    }

		// trust it and write back to disk
		trusted.add(keyRing);
		OutputStream fos = null;
		try {
			PGPPublicKeyRingCollection newCollection = new PGPPublicKeyRingCollection(trusted);
			fos = new FileOutputStream(ROO_PGP_FILE);
			newCollection.encode(fos);
			fos.close();
		} catch (Exception e) {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException ignore) {}
			}
			throw new IllegalStateException(e);
		}
		return keyRing;
	}
	
	@SuppressWarnings("unchecked")
	public PGPPublicKeyRing untrust(String keyId) {
		keyId = prepareKeyId(keyId);
		// get the keys we currently trust
		List<PGPPublicKeyRing> trusted = getTrustedKeys();
		
		// build a new list of keys we'll continue to trust after this method ends
		List<PGPPublicKeyRing> stillTrusted = new ArrayList<PGPPublicKeyRing>();
		
		// Locate the element to remove (we need to record it so the method can return it)
		PGPPublicKeyRing removed = null;
		for (PGPPublicKeyRing candidate : trusted) {
			boolean stillTrust = true;
			Iterator<PGPPublicKey> it = candidate.getPublicKeys();
			while (it.hasNext()) {
			    PGPPublicKey pgpKey = (PGPPublicKey) it.next();
			    String candidateKeyId = getKeyId(pgpKey.getKeyID());
			    if (removed == null && candidateKeyId.equals(keyId)) {
			    	stillTrust = false;
			    	removed = candidate;
			    	break;
			    }
			}
			if (stillTrust) {
				stillTrusted.add(candidate);
			}
		}
		
		Assert.notNull(removed, "The public key ID '" + keyId + "' is not currently trusted");
		
		// write back to disk
		OutputStream fos = null;
		try {
			PGPPublicKeyRingCollection newCollection = new PGPPublicKeyRingCollection(stillTrusted);
			fos = new FileOutputStream(ROO_PGP_FILE);
			newCollection.encode(fos);
			fos.close();
		} catch (Exception e) {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException ignore) {}
			}
			throw new IllegalStateException(e);
		}
		return removed;
	}

	public Map<String,String> refresh() {
		Map<String,String> result = new TreeMap<String,String>();
		// get the keys we currently trust
		List<PGPPublicKeyRing> trusted = getTrustedKeys();
		
		// build a new list of our refreshed keys
		List<PGPPublicKeyRing> stillTrusted = new ArrayList<PGPPublicKeyRing>();
		
		// Locate the element to remove (we need to record it so the method can return it)
		for (PGPPublicKeyRing candidate : trusted) {
			PGPPublicKey firstKey = candidate.getPublicKey();
		    String candidateKeyId = getKeyId(firstKey.getKeyID());
			// Try to refresh
		    PGPPublicKeyRing newKeyRing;
		    try {
		    	newKeyRing = getPublicKey(candidateKeyId);
		    } catch (Exception e) {
		    	// can't retrieve, so keep the old one for now
		    	stillTrusted.add(candidate);
		    	result.put(candidateKeyId, "WARNING: Retained original (download issue)");
		    	continue;
		    }
		    // Do not store if the first key is revoked
		    if (newKeyRing.getPublicKey().isRevoked()) {
		    	result.put(candidateKeyId, "WARNING: Key revoked, so removed from trust list");
		    } else {
			    stillTrusted.add(newKeyRing);
		    	result.put(candidateKeyId, "SUCCESS");
		    }
		}
		
		// write back to disk
		OutputStream fos = null;
		try {
			PGPPublicKeyRingCollection newCollection = new PGPPublicKeyRingCollection(stillTrusted);
			fos = new FileOutputStream(ROO_PGP_FILE);
			newCollection.encode(fos);
			fos.close();
		} catch (Exception e) {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException ignore) {}
			}
			throw new IllegalStateException(e);
		}
		
		return result;
	}

	public PGPPublicKeyRing getPublicKey(String keyId) {
		keyId = prepareKeyId(keyId);
		try {
			URL lookup = getKeyServerUrlToRetrieveKeyId(keyId);
			InputStream in = urlInputStreamService.openConnection(lookup);
			return getPublicKey(in);
		} catch (Exception e) {
			throw new IllegalStateException("Public key ID '" + keyId + "' not available from key server", e);
		}
	}

	public PGPPublicKeyRing getPublicKey(InputStream in) {
		Object obj;
		try {
			PGPObjectFactory pgpFact = new PGPObjectFactory(PGPUtil.getDecoderStream(in));
			obj = pgpFact.nextObject();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		
		if (obj != null && obj instanceof PGPPublicKeyRing) {
			PGPPublicKeyRing keyRing = (PGPPublicKeyRing) obj;
			return keyRing;
		}

		throw new IllegalStateException("Pblic key not available");
	}

	public URL getKeyServerUrlToRetrieveKeyId(String keyId) {
		try {
			return new URL(DEFAULT_KEYSERVER_URL + keyId);
		} catch (MalformedURLException e) {
			throw new IllegalStateException(e);
		}
	}

    public URL getKeyServerUrlToRetrieveKeyInformation(String keyId) {
	    URL keyUrl = getKeyServerUrlToRetrieveKeyId(keyId);
		try {
	    	URL keyIndexUrl = new URL(keyUrl.getProtocol() + "://" + keyUrl.getAuthority() + keyUrl.getPath() + "?fingerprint=on&op=index&search=");
			return new URL(keyIndexUrl.toString() + keyId);
		} catch (MalformedURLException e) {
			throw new IllegalStateException(e);
		}
    }
    
    public String getKeyId(long keyId) {
		return "0x" + String.format("%08X", (MASK & keyId));
	}

	public SignatureDecision isSignatureAcceptable(InputStream signature) throws IOException {

		PGPObjectFactory factory = new PGPObjectFactory(PGPUtil.getDecoderStream(signature));
		Object obj = factory.nextObject();

		PGPSignatureList p3;
		if (obj instanceof PGPCompressedData)  {
			try {
			    factory = new PGPObjectFactory(((PGPCompressedData) obj).getDataStream());
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		    p3 = (PGPSignatureList) factory.nextObject();
		} else {
		    p3 = (PGPSignatureList) obj;
		}
	
		PGPSignature pgpSignature = p3.get(0);
		
		Assert.notNull(pgpSignature, "Unable to retrieve signature from stream");

		String keyIdInHex = getKeyId(pgpSignature.getKeyID());
		boolean signatureAcceptable = false;
		
		// Loop to see if the user trusts this key
		for (PGPPublicKeyRing keyRing : getTrustedKeys()) {
			String candidate = getKeyId(keyRing.getPublicKey().getKeyID());
			if (candidate.equals(keyIdInHex)) {
				signatureAcceptable = true;
				break;
			}
		}
		
		if (!signatureAcceptable && automaticTrust) {
			// We don't approve of this signature, but the user has told us it's OK
			trust(keyIdInHex);
			signatureAcceptable = true;
		}
		
		return new SignatureDecision(pgpSignature, keyIdInHex, signatureAcceptable);
	}

	public boolean isResourceSignedBySignature(InputStream resource, InputStream signature) {
		PGPPublicKey publicKey = null;
		PGPSignature pgpSignature = null;

        try {
    		if (!(signature instanceof ArmoredInputStream)) {
    			signature = new ArmoredInputStream(signature);
    		}

    		pgpSignature = isSignatureAcceptable(signature).getPgpSignature();
            PGPPublicKeyRing keyRing = getPublicKey(getKeyId(pgpSignature.getKeyID()));
            publicKey = keyRing.getPublicKey();
            
            Assert.notNull(publicKey, "Could not obtain public key for signer key ID '" + getKeyId(pgpSignature.getKeyID()) + "'");
            
            pgpSignature.initVerify(publicKey, "BC");

            // now verify the signed content
            byte[] buff = new byte[BUFFER_SIZE]; 
            int chunk;
            do {
	        	chunk = resource.read(buff);
	        	if (chunk > 0) {
	        		pgpSignature.update(buff, 0, chunk);
	        	}
            } while (chunk >= 0);

            return pgpSignature.verify();
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
	}

}
