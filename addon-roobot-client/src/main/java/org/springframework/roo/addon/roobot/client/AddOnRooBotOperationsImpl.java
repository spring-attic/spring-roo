package org.springframework.roo.addon.roobot.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
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
	private @Reference Shell shell;
	private Logger log = Logger.getLogger(getClass().getName());
	private Properties props;
	private ComponentContext context;
	
	protected void activate(ComponentContext context) {
		this.context = context;
		bundleCache = new HashMap<String, AddOnBundleInfo>();
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
		Assert.notNull(bsn, "Bundle symbolic name required");
		AddOnBundleInfo bundle = bundleCache.get(bsn.getKey());
		if (bundle == null) {
			log.warning("Could not find information about the '" + bsn.getKey() + "' bundle");
		} else {
			log.info("Name: " + bundle.getName());
			log.info("Ranking: " + bundle.getRanking());
			log.info("Description: " + bundle.getDescription());
			log.info("Version: " + bundle.getVersion());
//			log.info("Last updated: " + bundle.getLastUpdated());
			log.info("Size: " + bundle.getSize());
			log.info("Pgp Key '" + bundle.getPgpKey() + "' signed by: " + bundle.getSignedBy());
			Map<String, String> commands = bundle.getCommands();
			for (String command: commands.keySet()) {
				log.info("Command name [" + command + "]; help ['" + commands.get(command) + "']");
			}
		}
	}
	
	public void installAddOn(AddOnBundleSymbolicName bsn) {
		boolean success = false;
		AddOnBundleInfo bundle = bundleCache.get(bsn.getKey());
		if (bundle == null) {
			log.warning("Could not find the '" + bsn.getKey() + "' bundle");
		} else {
			String url = bundle.getUrl();
			if (url != null && url.length() > 0) {
				int count = countBundles();
				success = shell.executeCommand("felix shell start " + url); 
				if (count == countBundles()) {
					return; // most likely PgP verification required before the bundle can be installed, no log needed 
				}
			}
			if (success) {
				log.info("Successfully installed add-on: " + bsn.getKey());
			} else {
				log.warning("Unable to install add-on: " + bsn.getKey());
			}
		}
	}

	public void listAddOns(boolean refresh) {
		if (refresh && populateBsnMap()) {
			log.info("Successfully downloaded Roobot Addon Data");
		}
		if (bundleCache.size() != 0) {
			log.info("List of known Spring Roo Add-ons:");
			for (String key: bundleCache.keySet()) {
				log.info("---------------------------------");
				log.info(bundleCache.get(key).toString());
			}
			log.info("---------------------------------");
			log.info("[HINT] use 'addon info --bundleSymbolicName ...' to see details about a specific bundle");
			log.info("[HINT] use 'addon install --bundleSymbolicName ...' to install a specific bundle");
		} else {
			log.info("No addons available for installation. (Are you connected to the Internet?)");
		}
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
			String url = props.getProperty("roobot.url", "http://spring-roo-repository.springsource.org/roobot.xml");
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
				for (Element bundle: XmlUtils.findElements("/roobot/bundles/bundle", roobotXml.getDocumentElement())) {
					
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
						for(Element shell: XmlUtils.findElements("shell", version)) {
							commands.put(shell.getAttribute("command"), shell.getAttribute("help"));
						}

						AddOnBundleInfo addonBundle = new AddOnBundleInfo(
							bsn, 
							new Float(bundle.getAttribute("uaa-ranking")), 
							version.getAttribute("name"), 
							version.getAttribute("description"), 
							updatedDate, 
							version.getAttribute("major") + "." + version.getAttribute("minor") + (version.getAttribute("micro").length() > 0 ? "." + version.getAttribute("micro") : "") + (version.getAttribute("qualifier").length() > 0 ? "." + version.getAttribute("qualifier") : ""),
							pgpKey,
							signedBy, 
							new Long(version.getAttribute("size")), 
							version.getAttribute("url"), 
							commands);
						
						bundleCache.put(bsn, addonBundle);
					}
				}
				success =  true;
			}
		} catch (Throwable ignore) {
		} finally {
			try {
				if (is != null) {
					is.close();
				}
			} catch (IOException ignored) {}
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
}