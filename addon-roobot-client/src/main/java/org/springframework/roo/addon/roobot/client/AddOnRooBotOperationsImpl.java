package org.springframework.roo.addon.roobot.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.felix.BundleSymbolicName;
import org.springframework.roo.felix.pgp.PgpKeyId;
import org.springframework.roo.felix.pgp.PgpService;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Implementation of commands that are available via the Roo shell.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component
@Service
public class AddOnRooBotOperationsImpl implements AddOnRooBotOperations {

	private Map<String, AddOnBundleInfo> bundleCache;
	private Map<String, AddOnBundleInfo> searchResultCache;
	@Reference private Shell shell;
	@Reference private PgpService pgpService;
	private Logger log = Logger.getLogger(getClass().getName());
	private Properties props;
	private ComponentContext context;
	private static String ROOBOT_XML = "http://spring-roo-repository.springsource.org/roobot.xml";
	
	protected void activate(ComponentContext context) {
		this.context = context;
		bundleCache = new HashMap<String, AddOnBundleInfo>();
		searchResultCache = new HashMap<String, AddOnBundleInfo>();
		Thread t = new Thread(new Runnable() {
			public void run() {
				populateBsnMap();
			}
		}, "Roobot XML Eager Download");
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
		AddOnBundleInfo bundle = null;
		if (bsn != null) {
			bundle = bundleCache.get(bsn.getKey());
		} 
		if (bundle == null) {
			log.warning("Could not find specified bundle with symbolic name: " + bsn);
			return;
		} 
		addOnInfo(bundle);
	}
	
	public void addOnInfo(String bundleKey) {
		Assert.hasText(bundleKey, "A valid bundle ID is required");
		AddOnBundleInfo bundle = null;
		if (searchResultCache != null) {
			bundle = searchResultCache.get(String.format("%02d", Integer.parseInt(bundleKey)));
		}
		if (bundle == null) {
			log.warning("A valid bundle ID is required");
			return;
		} 
		addOnInfo(bundle);
	}
	
	private void addOnInfo(AddOnBundleInfo bundle) {
		logInfo("Name", bundle.getName());
		logInfo("BSN", bundle.getBsn());
		logInfo("Version", bundle.getVersion());
		logInfo("OBR URL", "[TODO]");
		logInfo("JAR URL", bundle.getUrl());
		logInfo("PGP Signature", bundle.getPgpKey() + " signed by " + bundle.getSignedBy());
		logInfo("Size", bundle.getSize() + " bytes");
		Map<String, String> commands = bundle.getCommands();
		for (String command : commands.keySet()) {
			logInfo("Commands", "'" + command + "' [" + commands.get(command) + "]");
		}
		logInfo("Description", bundle.getDescription());
	}

	public void installAddOn(AddOnBundleSymbolicName bsn) {
		Assert.notNull(bsn, "A valid add-on bundle symbolic name is required");
		AddOnBundleInfo bundle = null;
		if (bsn != null) {
			bundle = bundleCache.get(bsn.getKey());
		} 
		if (bundle == null) {
			log.warning("Could not find specified bundle with symbolic name: " + bsn);
			return;
		} 
		installAddon(bundle);
	}
	
	public void installAddOn(String bundleKey) {
		Assert.hasText(bundleKey, "A valid bundle ID is required");
		AddOnBundleInfo bundle = null;
		if (searchResultCache != null) {
			bundle = searchResultCache.get(String.format("%02d", Integer.parseInt(bundleKey)));
		}
		if (bundle == null) {
			log.warning("To install an addon a valid bundle ID is required");
			return;
		} 
		installAddon(bundle);
	}
	
	private void installAddon(AddOnBundleInfo bundle) {
		boolean success = false;	
		String url = bundle.getUrl();
		if (url != null && url.length() > 0) {
			int count = countBundles();
			success = shell.executeCommand("osgi start --url " + url);
			if (count == countBundles()) {
				return; // most likely PgP verification required before the bundle can be installed, no log needed
			}
		}
		if (success) {
			log.info("Successfully installed add-on: " + bundle.getBsn());
		} else {
			log.warning("Unable to install add-on: " + bundle.getBsn());
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
	
	public void searchAddOns(String searchTerms, boolean refresh, int linesPerResult, int maxResults, boolean trustedOnly, boolean compatibleOnly, String requiresCommand) {
		Assert.hasText(searchTerms, "Search terms for add-on required");
		if (refresh && populateBsnMap()) {
			log.info("Successfully downloaded Roobot Addon Data");
		}
		if (bundleCache.size() != 0) {
			searchResultCache.clear();
			String [] terms = searchTerms.split(",");
			for (AddOnBundleInfo bundle: bundleCache.values()) {
				//first set relevance of all bundles to zero
				bundle.setSearchRelevance(0f);
				int hits = 0;
				for (String term: terms) {
					if (bundle.getSummary().toLowerCase().contains(term.trim().toLowerCase())) {
						hits++;
					}
				}
				bundle.setSearchRelevance(hits / terms.length);
			}
			LinkedList<AddOnBundleInfo> bundles = new LinkedList<AddOnBundleInfo>(bundleCache.values());
			Collections.sort(bundles, new SearchComparator());
			LinkedList<AddOnBundleInfo> filteredSearchResults = filterList(bundles, maxResults, trustedOnly, compatibleOnly, requiresCommand, true);
			printResultList(filteredSearchResults, linesPerResult);
		} else {
			log.info("No addons available for installation. (Are you connected to the Internet?)");
		}
	}

	public void listAddOns(boolean refresh, int linesPerResult, int maxResults, boolean trustedOnly, boolean compatibleOnly, String requiresCommand) {
		if (refresh && populateBsnMap()) {
			log.info("Successfully downloaded Roobot Addon Data");
		}
		if (bundleCache.size() != 0) {
			LinkedList<AddOnBundleInfo> bundles = new LinkedList<AddOnBundleInfo>(bundleCache.values());
			Collections.sort(bundles, new RankingComparator());
			LinkedList<AddOnBundleInfo> filteredList = filterList(bundles, maxResults, trustedOnly, compatibleOnly, requiresCommand, false);
			printResultList(filteredList, linesPerResult);
		} else {
			log.info("No addons available for installation. (Are you connected to the Internet?)");
		}
	}
	
	private LinkedList<AddOnBundleInfo> filterList(LinkedList<AddOnBundleInfo> bundles, int maxResults, boolean trustedOnly, boolean compatibleOnly, String requiresCommand, boolean onlyRelevantBundles) {
		LinkedList<AddOnBundleInfo> filteredList = new LinkedList<AddOnBundleInfo>();
		List<PGPPublicKeyRing> keys = null;
		if (trustedOnly) {
			keys = pgpService.getTrustedKeys();
		}
		bundle_loop: for (AddOnBundleInfo bundle: bundles) {
			if (onlyRelevantBundles && !(bundle.getSearchRelevance() > 0)) {
				continue bundle_loop;
			}
			if (trustedOnly && !isTrustedKey(keys, bundle.getPgpKey())) {
				continue bundle_loop;
			} 
			if (compatibleOnly && !isCompatible(bundle.getVersion())) {
				continue bundle_loop;
			}
			if (requiresCommand != null && requiresCommand.length() > 0 && !bundle.getCommands().keySet().contains(requiresCommand.trim())) {
				continue bundle_loop;
			}
			if (maxResults-- == 0) {
				break;
			}
			filteredList.add(bundle);
		}
		return filteredList;
	}
	
	private void printResultList(LinkedList<AddOnBundleInfo> bundles, int linesPerResult) {
		int bundleId = 1;
		searchResultCache.clear();
		StringBuilder sb = new StringBuilder();
		List<PGPPublicKeyRing> keys = pgpService.getTrustedKeys();
		log.info("Ordered by ranking; T = Trusted developer; R = Roo version XX compatible");
		log.warning("ID T R DESCRIPTION -------------------------------------------------------------");
		for (AddOnBundleInfo bundle: bundles) {
			String bundleKey = String.format("%02d", bundleId++);
			searchResultCache.put(bundleKey, bundle);
			sb.append(bundleKey);
			sb.append(isTrustedKey(keys, bundle.getPgpKey()) ? " Y " : " - ");
			sb.append(isCompatible(bundle.getVersion()) ? "Y " : "- "); 
			sb.append(bundle.getVersion());
			sb.append(" ");
			ArrayList<String> split = new ArrayList<String>(Arrays.asList(bundle.getDescription().split("\\s")));
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
		log.info("[HINT] use 'addon info' to see details about a bundle");
		log.info("[HINT] use 'addon install' to install a specific bundle");
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

	public Set<String> getAddOnBsnSet() {
		if (bundleCache == null) {
			populateBsnMap();
		}
		if (bundleCache != null && bundleCache.size() > 0) {
			return bundleCache.keySet();
		}
		return new HashSet<String>();
	}

	public Map<String, AddOnBundleInfo> getAddOnCache(boolean refresh) {
		if (refresh) {
			populateBsnMap();
		}
		return Collections.unmodifiableMap(bundleCache);
	}

	private boolean populateBsnMap() {
		boolean success = false;
		InputStream is = null;
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			String url = props.getProperty("roobot.url", ROOBOT_XML);
			if (url == null) {
				log.warning("Bundle properties could not be loaded");
				return false;
			}
			is = new URL(url).openStream();
			if (is == null) {
				log.warning("Could not connect to Roo Addon bundle repository index");
				return false;
			}
			Document roobotXml = db.parse(is);

			if (roobotXml != null) {
				bundleCache.clear();
				for (Element bundle : XmlUtils.findElements("/roobot/bundles/bundle", roobotXml.getDocumentElement())) {

					String bsn = bundle.getAttribute("bsn");
					Element version = XmlUtils.findFirstElement("version", bundle);
					if (bsn != null && bsn.length() > 0 && version != null) {
						String signedBy = "";
						String pgpKey = version.getAttribute("pgp-key-id");
						if (pgpKey != null && pgpKey.length() > 0) {
							Element pgpSigned = XmlUtils.findFirstElement("/roobot/pgp-keys/pgp-key[@id='" + pgpKey + "']/pgp-key-description", roobotXml.getDocumentElement());
							if (pgpSigned != null) {
								signedBy = pgpSigned.getAttribute("text");
							}
						}

						Date updatedDate = null;
						String[] updatedArray = version.getAttribute("last-updated").split(".");
						if (updatedArray.length > 0) {
							String updated = updatedArray[0];
							updatedDate = new Date(new Long(updated));
						}

						Map<String, String> commands = new HashMap<String, String>();
						for (Element shell : XmlUtils.findElements("shell", version)) {
							commands.put(shell.getAttribute("command"), shell.getAttribute("help"));
						}

						AddOnBundleInfo addonBundle = new AddOnBundleInfo(bsn, new Float(bundle.getAttribute("uaa-ranking")), version.getAttribute("name"), version.getAttribute("description"), updatedDate, version.getAttribute("major") + "." + version.getAttribute("minor") + (version.getAttribute("micro").length() > 0 ? "." + version.getAttribute("micro") : "") + (version.getAttribute("qualifier").length() > 0 ? "." + version.getAttribute("qualifier") : ""), pgpKey, signedBy, new Long(version.getAttribute("size")), version.getAttribute("url"), commands);

						bundleCache.put(bsn, addonBundle);
					}
				}
				success = true;
			}
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
			Bundle[] bundles = bc.getBundles();
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
		return true; //TODO use projectMetadata.getProperty("roo.version"); to get the project version and read roo compatibility version from roobot.xml
	}
	
	private class RankingComparator implements Comparator<AddOnBundleInfo> {
		public int compare(AddOnBundleInfo o1, AddOnBundleInfo o2) {
			if (o1.getRanking() == o2.getRanking()) return 0;
			else if (o1.getRanking() > o2.getRanking()) return 1;
			else return -1;
		}
	}
	
	private class SearchComparator implements Comparator<AddOnBundleInfo> {
		public int compare(AddOnBundleInfo o1, AddOnBundleInfo o2) {
			if (o1.getSearchRelevance() < o2.getSearchRelevance()) return -1;
			else if (o1.getSearchRelevance() > o2.getSearchRelevance()) return 1;
			//order by ranking if search relevance is equal
			else {
				if (o1.getRanking() == o2.getRanking()) return 0;
				else if (o1.getRanking() > o2.getRanking()) return 1;
				else return -1;	
			}
		}
	}
}