package org.springframework.roo.addon.roobot.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.roobot.client.model.Bundle;
import org.springframework.roo.addon.roobot.client.model.BundleVersion;
import org.springframework.roo.addon.roobot.client.model.Comment;
import org.springframework.roo.addon.roobot.client.model.Rating;
import org.springframework.roo.felix.BundleSymbolicName;
import org.springframework.roo.felix.pgp.PgpKeyId;
import org.springframework.roo.felix.pgp.PgpService;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.springframework.roo.uaa.UaaRegistrationService;
import org.springframework.roo.url.stream.UrlInputStreamService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Implementation of commands that are available via the Roo shell.
 * 
 * @author Stefan Schmidt
 * @author Ben Alex
 * @since 1.1
 */
@Component
@Service
public class AddOnRooBotOperationsImpl implements AddOnRooBotOperations {

	private Map<String, Bundle> bundleCache;
	private Map<String, Bundle> searchResultCache;
	@Reference private Shell shell;
	@Reference private PgpService pgpService;
	@Reference private UrlInputStreamService urlInputStreamService;
	private static final Logger log = Logger.getLogger(AddOnRooBotOperationsImpl.class.getName());
	private Properties props;
	private ComponentContext context;
	private static String ROOBOT_XML_URL = "http://spring-roo-repository.springsource.org/roobot/roobot.xml.zip";
	private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
	
	protected void activate(ComponentContext context) {
		this.context = context;
		bundleCache = new HashMap<String, Bundle>();
		searchResultCache = new HashMap<String, Bundle>();
		Thread t = new Thread(new Runnable() {
			public void run() {
				populateBundleCache(true);
			}
		}, "Roo Add-on Index XML Eager Download");
		t.start();
		props = new Properties();
		try {
			props.load(TemplateUtils.getTemplate(getClass(), "manager.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void addOnInfo(AddOnBundleSymbolicName bsn) {
		Assert.notNull(bsn, "A valid add-on bundle symbolic name is required");
		String bsnString = bsn.getKey();
		if (bsnString.contains(";")) {
			bsnString = bsnString.split(";")[0];
		}
		Bundle bundle = bundleCache.get(bsnString);
		if (bundle == null) {
			log.warning("Unable to find specified bundle with symbolic name: " + bsn.getKey());
			return;
		} 
		addOnInfo(bundle, bundle.getBundleVersion(bsn.getKey()));
	}
	
	public void addOnInfo(String bundleKey) {
		Assert.hasText(bundleKey, "A valid bundle ID is required");
		Bundle bundle = null;
		if (searchResultCache != null) {
			bundle = searchResultCache.get(String.format("%02d", Integer.parseInt(bundleKey)));
		}
		if (bundle == null) {
			log.warning("A valid bundle ID is required");
			return;
		} 
		addOnInfo(bundle, bundle.getBundleVersion(bundleKey));
	}
	
	private void addOnInfo(Bundle bundle, BundleVersion bundleVersion) {
		StringBuilder sb = new StringBuilder(bundleVersion.getVersion());
		if (bundle.getVersions().size() > 1) {
			sb.append(" [available versions: ");
			for (BundleVersion version: BundleVersion.orderByVersion(new ArrayList<BundleVersion>(bundle.getVersions()))) {
				sb.append(version.getVersion()).append(", ");
			};
			sb.delete(sb.length() - 2, sb.length()).append("]");
		}
		logInfo("Name", bundleVersion.getPresentationName());
		logInfo("BSN", bundle.getSymbolicName());
		logInfo("Version", sb.toString());
		logInfo("Roo Version", bundleVersion.getRooVersion());
		logInfo("Ranking", new Float(bundle.getRanking()).toString());
		logInfo("JAR Size", bundleVersion.getSize() + " bytes");
		logInfo("PGP Signature", bundleVersion.getPgpKey() + " signed by " + bundleVersion.getPgpDescriptions());
		logInfo("OBR URL", bundleVersion.getObrUrl());
		logInfo("JAR URL", bundleVersion.getUri());
		Map<String, String> commands = bundleVersion.getCommands();
		for (String command : commands.keySet()) {
			logInfo("Commands", "'" + command + "' [" + commands.get(command) + "]");
		}
		logInfo("Description", bundleVersion.getDescription());
		int cc = 0;
		for (Comment comment: bundle.getComments()) {
			logInfo("Comment " + (++cc), "Rating [" + comment.getRating().name() + "], Date [" + SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT).format(comment.getDate()) + "], Comment [" + comment.getComment() + "]");
		}
	}

	public void installAddOn(AddOnBundleSymbolicName bsn) {
		Assert.notNull(bsn, "A valid add-on bundle symbolic name is required");
		String bsnString = bsn.getKey();
		if (bsnString.contains(";")) {
			bsnString = bsnString.split(";")[0];
		}
		Bundle bundle = bundleCache.get(bsnString);
		if (bundle == null) {
			log.warning("Could not find specified bundle with symbolic name: " + bsn.getKey());
			return;
		} 
		installAddon(bundle.getBundleVersion(bsn.getKey()), bsn.getKey());
	}
	
	public void installAddOn(String bundleKey) {
		Assert.hasText(bundleKey, "A valid bundle ID is required");
		Bundle bundle = null;
		if (searchResultCache != null) {
			bundle = searchResultCache.get(String.format("%02d", Integer.parseInt(bundleKey)));
		}
		if (bundle == null) {
			log.warning("To install an addon a valid bundle ID is required");
			return;
		} 
		installAddon(bundle.getBundleVersion(bundleKey), bundle.getSymbolicName());
	}
	
	private void installAddon(BundleVersion bundleVersion, String bsn) {
		if (!verifyRepository(bundleVersion.getObrUrl())) {
			return;
		}
		boolean success = true;	
		int count = countBundles();
		boolean requiresWrappedCoreDep = bundleVersion.getDescription().contains("#wrappedCoreDependency");
		if (requiresWrappedCoreDep && !shell.executeCommand("osgi obr url add --url http://spring-roo-repository.springsource.org/repository.xml")) {
			success = false;
		}
		if (!shell.executeCommand("osgi obr url add --url " + bundleVersion.getObrUrl())) {
			success = false;
		}
		if (!shell.executeCommand("osgi obr start --bundleSymbolicName " + bsn)) {
			success = false;
		}
		if (!shell.executeCommand("osgi obr url remove --url " + bundleVersion.getObrUrl())) {
			success = false;
		}
		if (requiresWrappedCoreDep && !shell.executeCommand("osgi obr url remove --url http://spring-roo-repository.springsource.org/repository.xml")) {
			success = false;
		}
		if (count == countBundles()) {
			return; // most likely PgP verification required before the bundle can be installed, no log needed
		}

		if (success) {
			log.info("Successfully installed add-on: " + bundleVersion.getPresentationName() + " [version: " + bundleVersion.getVersion() + "]");
			log.warning("[Hint] Please consider rating this add-on with the following command:");
			log.warning("[Hint] addon feedback bundle --bundleSymbolicName " + bsn.substring(0, bsn.indexOf(";") != -1 ? bsn.indexOf(";") : bsn.length()) + " --rating ... --comment \"...\"");
		} else {
			log.warning("Unable to install add-on: " + bundleVersion.getPresentationName() + " [version: " + bundleVersion.getVersion() + "]");
		}
	}

	public void removeAddOn(BundleSymbolicName bsn) {
		Assert.notNull(bsn, "Bundle symbolic name required");
		boolean success = false;
		int count = countBundles();
		success = shell.executeCommand("osgi uninstall --bundleSymbolicName " + bsn.getKey());
		if (count == countBundles() || !success) {
			log.warning("Unable to remove add-on: " + bsn.getKey());
		} else {
			log.info("Successfully removed add-on: " + bsn.getKey());
		}
	}
	
	public Integer searchAddOns(boolean showFeedback, String searchTerms, boolean refresh, int linesPerResult, int maxResults, boolean trustedOnly, boolean compatibleOnly, String requiresCommand) {
		if (maxResults > 99) {
			maxResults = 99;
		}
		if (maxResults < 1) {
			maxResults = 10;
		}
		if (bundleCache.size() == 0) {
			// We should refresh regardless in this case
			refresh = true;
		}
		if (refresh && populateBundleCache(false)) {
			if (showFeedback) {
				log.info("Successfully downloaded Roo add-on Data");
			}
		}
		if (bundleCache.size() != 0) {
			boolean onlyRelevantBundles = false;
			if (searchTerms != null && !"".equals(searchTerms)) {
				onlyRelevantBundles = true;
				String [] terms = searchTerms.split(",");
				for (Bundle bundle: bundleCache.values()) {
					//first set relevance of all bundles to zero
					bundle.setSearchRelevance(0f);
					int hits = 0;
					BundleVersion latest = bundle.getLatestVersion();
					for (String term: terms) {
						if ((bundle.getSymbolicName() + ";" + latest.getSummary()).toLowerCase().contains(term.trim().toLowerCase())) {
							hits++;
						}
					}
					bundle.setSearchRelevance(hits / terms.length);
				}
			}
			List<Bundle> bundles = Bundle.orderBySearchRelevance(new ArrayList<Bundle>(bundleCache.values()));
			LinkedList<Bundle> filteredSearchResults = filterList(bundles, trustedOnly, compatibleOnly, requiresCommand, onlyRelevantBundles);
			if (showFeedback) {
				printResultList(filteredSearchResults, maxResults, linesPerResult);
			}
			return filteredSearchResults.size();
		}
		
		// There is a problem with the add-on index
		if (showFeedback) {
			log.info("No add-ons known. Are you online? Try the 'download status' command");
		}
		
		return null;
	}

	public void listAddOns(boolean refresh, int linesPerResult, int maxResults, boolean trustedOnly, boolean compatibleOnly, String requiresCommand) {
		if (bundleCache.size() == 0) {
			// We should refresh regardless in this case
			refresh = true;
		}
		if (refresh && populateBundleCache(false)) {
			log.info("Successfully downloaded Roo add-on Data");
		}
		if (bundleCache.size() != 0) {
			List<Bundle> bundles = Bundle.orderByRanking(new ArrayList<Bundle>(bundleCache.values()));
			LinkedList<Bundle> filteredList = filterList(bundles, trustedOnly, compatibleOnly, requiresCommand, false);
			printResultList(filteredList, maxResults, linesPerResult);
		} else {
			log.info("No add-ons known. Are you online? Try the 'download status' command");
		}
	}
	
	private LinkedList<Bundle> filterList(List<Bundle> bundles, boolean trustedOnly, boolean compatibleOnly, String requiresCommand, boolean onlyRelevantBundles) {
		LinkedList<Bundle> filteredList = new LinkedList<Bundle>();
		List<PGPPublicKeyRing> keys = null;
		if (trustedOnly) {
			keys = pgpService.getTrustedKeys();
		}
		bundle_loop: for (Bundle bundle: bundles) {
			BundleVersion latest = bundle.getLatestVersion();
			if (onlyRelevantBundles && !(bundle.getSearchRelevance() > 0)) {
				continue bundle_loop;
			}
			if (trustedOnly && !isTrustedKey(keys, latest.getPgpKey())) {
				continue bundle_loop;
			} 
			if (compatibleOnly && !isCompatible(latest.getRooVersion())) {
				continue bundle_loop;
			}
			if (requiresCommand != null && requiresCommand.length() > 0) {
				boolean matchingCommand = false;
				for (String cmd : latest.getCommands().keySet()) {
					if (cmd.startsWith(requiresCommand) || requiresCommand.startsWith(cmd)) {
						matchingCommand = true;
						break;
					}
				}
				if (!matchingCommand) {
					continue bundle_loop;
				}
			}
			filteredList.add(bundle);
		}
		return filteredList;
	}
	
	private void printResultList(LinkedList<Bundle> bundles, int maxResults, int linesPerResult) {
		int bundleId = 1;
		searchResultCache.clear();
		StringBuilder sb = new StringBuilder();
		List<PGPPublicKeyRing> keys = pgpService.getTrustedKeys();
		log.info(bundles.size() + " found, sorted by rank; T = trusted developer; R = Roo " + getVersionForCompatibility() + " compatible");
		log.warning("ID T R DESCRIPTION -------------------------------------------------------------");
		for (Bundle bundle: bundles) {
			if (maxResults-- == 0) {
				break;
			}
			BundleVersion latest = bundle.getLatestVersion();
			String bundleKey = String.format("%02d", bundleId++);
			searchResultCache.put(bundleKey, bundle);
			sb.append(bundleKey);
			sb.append(isTrustedKey(keys, latest.getPgpKey()) ? " Y " : " - ");
			sb.append(isCompatible(latest.getRooVersion()) ? "Y " : "- "); 
			sb.append(latest.getVersion());
			sb.append(" ");
			ArrayList<String> split = new ArrayList<String>(Arrays.asList(latest.getDescription().split("\\s")));
			int lpr = linesPerResult;
			while (split.size() > 0 && --lpr >= 0) {
				while (!(split.size() == 0) && ((split.get(0).length() + sb.length()) < (lpr == 0 ? 77 : 80))) {
					sb.append(split.get(0)).append(" ");
					split.remove(0);
				}
				String line = sb.toString().substring(0, sb.toString().length() - 1);
				if (lpr == 0 && split.size() > 0) {
					line += "...";
				}
				log.info(line);
				sb.setLength(0);
				sb.append("       ");
			}
			if(sb.toString().trim().length() > 0) {
				log.info(sb.toString());
			}
			sb.setLength(0);
		}
		log.warning("--------------------------------------------------------------------------------");
		log.info("[HINT] use 'addon info TAB' to see details about a bundle");
		log.info("[HINT] use 'addon install TAB' to install a specific bundle");
	}
	
	@SuppressWarnings("unchecked")
	private boolean isTrustedKey(List<PGPPublicKeyRing> keys, String keyId) {
		for (PGPPublicKeyRing keyRing: keys) {
			Iterator<PGPPublicKey> it = keyRing.getPublicKeys();
			while (it.hasNext()) {
				PGPPublicKey pgpKey = (PGPPublicKey) it.next();
				if (new PgpKeyId(pgpKey).equals(new PgpKeyId(keyId))) { 
					return true;
				}
			}
		}
		return false;
	}

	public Map<String, Bundle> getAddOnCache(boolean refresh) {
		if (refresh) {
			populateBundleCache(false);
		}
		return Collections.unmodifiableMap(bundleCache);
	}

	private boolean populateBundleCache(boolean startupTime) {
		boolean success = false;
		InputStream is = null;
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			String url = props.getProperty("roobot.url", ROOBOT_XML_URL);
			if (url == null) {
				log.warning("Bundle properties could not be loaded");
				return false;
			}
			if (url.startsWith("http://")) {
				// Handle it as HTTP
				URL httpUrl = new URL(url);
				String failureMessage = urlInputStreamService.getUrlCannotBeOpenedMessage(httpUrl);
				if (failureMessage != null) {
					if (!startupTime) {
						// This wasn't just an eager startup time attempt, so let's display the error reason
						// (for startup time, we just fail quietly)
						log.warning(failureMessage);
					}
					return false;
				}
				// It appears we can acquire the URL, so let's do it
				is = urlInputStreamService.openConnection(httpUrl);
			} else {
				// Fallback to normal protocol handler (likely in local development testing etc
				is = new URL(url).openStream();
			}
			if (is == null) {
				log.warning("Could not connect to Roo Addon bundle repository index");
				return false;
			}
			
			ZipInputStream zip = new ZipInputStream(is);
			zip.getNextEntry();
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buffer = new byte[8192];
			int length = -1;
			    while (zip.available() > 0) {
			    length = zip.read(buffer, 0, 8192);
			         if (length > 0) {
			         baos.write(buffer, 0, length);
			    }
			}

			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			Document roobotXml = db.parse(bais);

			if (roobotXml != null) {
				bundleCache.clear();
				for (Element bundleElement : XmlUtils.findElements("/roobot/bundles/bundle", roobotXml.getDocumentElement())) {
					String bsn = bundleElement.getAttribute("bsn");
					
					List<Comment> comments = new LinkedList<Comment>();
					for (Element commentElement: XmlUtils.findElements("comments/comment", bundleElement)) {
						comments.add(new Comment(Rating.fromInt(new Integer(commentElement.getAttribute("rating"))), commentElement.getAttribute("comment"), dateFormat.parse(commentElement.getAttribute("date"))));
					}
					Bundle bundle = new Bundle(bundleElement.getAttribute("bsn"), new Float(bundleElement.getAttribute("uaa-ranking")).floatValue(), comments);

					for (Element versionElement: XmlUtils.findElements("versions/version", bundleElement)) {
						if (bsn != null && bsn.length() > 0 && versionElement != null) {
							String signedBy = "";
							String pgpKey = versionElement.getAttribute("pgp-key-id");
							if (pgpKey != null && pgpKey.length() > 0) {
								Element pgpSigned = XmlUtils.findFirstElement("/roobot/pgp-keys/pgp-key[@id='" + pgpKey + "']/pgp-key-description", roobotXml.getDocumentElement());
								if (pgpSigned != null) {
									signedBy = pgpSigned.getAttribute("text");
								}
							}
	
							Map<String, String> commands = new HashMap<String, String>();
							for (Element shell : XmlUtils.findElements("shell-commands/shell-command", versionElement)) {
								commands.put(shell.getAttribute("command"), shell.getAttribute("help"));
							}
							
					    	StringBuilder versionBuilder = new StringBuilder();
					    	versionBuilder.append(versionElement.getAttribute("major")).append(".").append(versionElement.getAttribute("minor"));
					    	String versionMicro = versionElement.getAttribute("micro");
					    	if (versionMicro != null && versionMicro.length() > 0) {
					    		versionBuilder.append(".").append(versionMicro);
					    	}
					    	String versionQualifier = versionElement.getAttribute("qualifier");
					    	if (versionQualifier != null && versionQualifier.length() > 0) {
					    		versionBuilder.append(".").append(versionQualifier);
					    	}   
					    	
					    	String rooVersion = versionElement.getAttribute("roo-version");
					    	if (rooVersion.equals("*") || rooVersion.length() == 0) {
					    		rooVersion = getVersionForCompatibility();
					    	} else {
					    		String[] split = rooVersion.split(".");
					    		if (split.length > 2) {
					    			//only interested in major.minor
					    			rooVersion = split[0] + "." + split[1];
					    		}
					    	}
							
					    	BundleVersion version = new BundleVersion(versionElement.getAttribute("url"), versionElement.getAttribute("obr-url"), versionBuilder.toString(), versionElement.getAttribute("name"), new Long(versionElement.getAttribute("size")).longValue(), versionElement.getAttribute("description"), pgpKey, signedBy, rooVersion, commands);
					    	// For security reasons we ONLY accept httppgp:// add-on versions
					    	if (!version.getUri().startsWith("httppgp://")) {
								continue;
							}
							bundle.addVersion(version);
						}
						bundleCache.put(bsn, bundle);
					}
				}
				success = true;
			}
			zip.close();
			baos.close();
			bais.close();
		} catch (Throwable ignore) {
		} finally {
			try {
				if (is != null) {
					is.close();
				}
			} catch (IOException ignored) {
			}
		}
		return success;
	}

	private int countBundles() {
		BundleContext bc = context.getBundleContext();
		if (bc != null) {
			org.osgi.framework.Bundle[] bundles = bc.getBundles();
			if (bundles != null) {
				return bundles.length;
			}
		}
		return 0;
	}

	private void logInfo(String label, String content) {
		StringBuilder sb = new StringBuilder();
		sb.append(label);
		for (int i = 0; i < 13 - label.length(); i++) {
			sb.append(".");
		}
		sb.append(": ");
		if (content.length() < 65) {
			sb.append(content);
			log.info(sb.toString());
		} else {
			ArrayList<String> split = new ArrayList<String>(Arrays.asList(content.split("\\s")));
			if (split.size() == 1) {
				while (content.length() > 65) {
					sb.append(content.substring(0, 65));
					content = content.substring(65);
					log.info(sb.toString());
					sb.setLength(0);
					sb.append("               ");
				}
				if (content.length() > 0) {
					log.info(sb.append(content).toString());
				}
			} else {
				while (split.size() > 0) {
					while (!(split.size() == 0) && ((split.get(0).length() + sb.length()) < 79)) {
						sb.append(split.get(0)).append(" ");
						split.remove(0);
					}
					log.info(sb.toString().substring(0, sb.toString().length() - 1));
					sb.setLength(0);
					sb.append("               ");
				}
			}
		}
	}
	
	private boolean isCompatible(String version) {
		return version.equals(getVersionForCompatibility());
	}
	
	private String getVersionForCompatibility() {
		return UaaRegistrationService.SPRING_ROO.getMajorVersion() + "." + UaaRegistrationService.SPRING_ROO.getMinorVersion();
	}
	
	private boolean verifyRepository(String repoUrl) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Document doc = null;
		try {
			URL obrUrl = null;
			obrUrl = new URL(repoUrl);
			DocumentBuilder db = dbf.newDocumentBuilder();
			doc = db.parse(obrUrl.openStream());
			Assert.notNull(doc, "RooBot was unable to parse the repository document of this add-on");
			for (Element resource: XmlUtils.findElements("resource", doc.getDocumentElement())) {
				if (resource.hasAttribute("uri")) {
					if (!resource.getAttribute("uri").startsWith("httppgp")) {
						log.warning("Sorry, the resource " + resource.getAttribute("uri") + " does not follow HTTPPGP conventions mandated by Spring Roo so the OBR file at " + repoUrl + " is unacceptable at this time");
						return false;
					}
				}
			}
			doc = null;
		} catch (Exception e) {
			throw new IllegalStateException("RooBot was unable to parse the repository document of this add-on", e);
		}
		return true;
	}
}