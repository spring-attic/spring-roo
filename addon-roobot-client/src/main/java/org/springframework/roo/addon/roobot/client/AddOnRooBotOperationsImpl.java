package org.springframework.roo.addon.roobot.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
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
import org.springframework.roo.classpath.preferences.Preferences;
import org.springframework.roo.classpath.preferences.PreferencesService;
import org.springframework.roo.felix.BundleSymbolicName;
import org.springframework.roo.felix.pgp.PgpKeyId;
import org.springframework.roo.felix.pgp.PgpService;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.springframework.roo.uaa.UaaRegistrationService;
import org.springframework.roo.url.stream.UrlInputStreamService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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

    public static final String ADDON_UPGRADE_STABILITY_LEVEL = "ADDON_UPGRADE_STABILITY_LEVEL";
    private static final Logger LOGGER = HandlerUtils
            .getLogger(AddOnRooBotOperationsImpl.class);
    private static final List<String> NO_UPGRADE_BSN_LIST = Arrays.asList(
            "org.springframework.uaa.client",
            "org.springframework.roo.url.stream.jdk",
            "org.springframework.roo.url.stream",
            "org.springframework.roo.file.monitor",
            "org.springframework.roo.file.monitor.polling",
            "org.springframework.roo.file.monitor.polling.roo",
            "org.springframework.roo.bootstrap",
            "org.springframework.roo.classpath",
            "org.springframework.roo.classpath.javaparser",
            "org.springframework.roo.deployment.support",
            "org.springframework.roo.felix",
            "org.springframework.roo.file.undo",
            "org.springframework.roo.metadata",
            "org.springframework.roo.model",
            "org.springframework.roo.osgi.bundle",
            "org.springframework.roo.osgi.roo.bundle",
            "org.springframework.roo.process.manager",
            "org.springframework.roo.project", "org.springframework.roo.root",
            "org.springframework.roo.shell",
            "org.springframework.roo.shell.jline",
            "org.springframework.roo.shell.jline.osgi",
            "org.springframework.roo.shell.osgi",
            "org.springframework.roo.startlevel",
            "org.springframework.roo.support",
            "org.springframework.roo.support.osgi",
            "org.springframework.roo.uaa");

    @Reference private PgpService pgpService;
    @Reference private PreferencesService preferencesService;
    @Reference private Shell shell;
    @Reference private UrlInputStreamService urlInputStreamService;

    private Map<String, Bundle> bundleCache;
    private ComponentContext context;
    private final DateFormat dateFormat = new SimpleDateFormat(
            "yyyy-MM-dd hh:mm:ss");
    private final Object mutex = new Object();
    private Preferences prefs;
    private volatile Thread rooBotEagerDownload;
    private boolean rooBotIndexDownload = true;
    private String rooBotXmlUrl = "http://spring-roo-repository.springsource.org/roobot/roobot.xml.zip";
    private Map<String, Bundle> searchResultCache;

    protected void activate(final ComponentContext context) {
        this.context = context;
        prefs = preferencesService
                .getPreferencesFor(AddOnRooBotOperationsImpl.class);
        bundleCache = new HashMap<String, Bundle>();
        searchResultCache = new HashMap<String, Bundle>();
        final BundleContext bundleContext = context.getBundleContext();
        if (bundleContext != null) {
            final String roobot = bundleContext.getProperty("roobot.url");
            if (roobot != null && roobot.length() > 0) {
                rooBotXmlUrl = roobot;
            }
            rooBotIndexDownload = Boolean.valueOf(bundleContext
                    .getProperty("roobot.index.dowload"));
        }
        if (rooBotIndexDownload) {
            rooBotEagerDownload = new Thread(new Runnable() {
                public void run() {
                    synchronized (mutex) {
                        populateBundleCache(true);
                    }
                }
            }, "Spring Roo RooBot Add-In Index Eager Download");
            rooBotEagerDownload.start();
        }
    }

    public void addOnInfo(final AddOnBundleSymbolicName bsn) {
        Validate.notNull(bsn, "A valid add-on bundle symbolic name is required");
        synchronized (mutex) {
            String bsnString = bsn.getKey();
            if (bsnString.contains(";")) {
                bsnString = bsnString.split(";")[0];
            }
            final Bundle bundle = bundleCache.get(bsnString);
            if (bundle == null) {
                LOGGER.warning("Unable to find specified bundle with symbolic name: "
                        + bsn.getKey());
                return;
            }
            addOnInfo(bundle, bundle.getBundleVersion(bsn.getKey()));
        }
    }

    private void addOnInfo(final Bundle bundle,
            final BundleVersion bundleVersion) {
        final StringBuilder sb = new StringBuilder(bundleVersion.getVersion());
        if (bundle.getVersions().size() > 1) {
            sb.append(" [available versions: ");
            for (final BundleVersion version : BundleVersion
                    .orderByVersion(new ArrayList<BundleVersion>(bundle
                            .getVersions()))) {
                sb.append(version.getVersion()).append(", ");
            }
            sb.delete(sb.length() - 2, sb.length()).append("]");
        }
        logInfo("Name", bundleVersion.getPresentationName());
        logInfo("BSN", bundle.getSymbolicName());
        logInfo("Version", sb.toString());
        logInfo("Roo Version", bundleVersion.getRooVersion());
        logInfo("Ranking", Float.toString(bundle.getRanking()));
        logInfo("JAR Size", bundleVersion.getSize() + " bytes");
        logInfo("PGP Signature", bundleVersion.getPgpKey() + " signed by "
                + bundleVersion.getPgpDescriptions());
        logInfo("OBR URL", bundleVersion.getObrUrl());
        logInfo("JAR URL", bundleVersion.getUri());
        for (final Entry<String, String> entry : bundleVersion.getCommands()
                .entrySet()) {
            logInfo("Commands", "'" + entry.getKey() + "' [" + entry.getValue()
                    + "]");
        }
        logInfo("Description", bundleVersion.getDescription());
        int cc = 0;
        for (final Comment comment : bundle.getComments()) {
            logInfo("Comment " + ++cc,
                    "Rating ["
                            + comment.getRating().name()
                            + "], grade ["
                            + DateFormat.getDateInstance(DateFormat.SHORT)
                                    .format(comment.getDate()) + "], Comment ["
                            + comment.getComment() + "]");
        }
    }

    public void addOnInfo(final String bundleKey) {
        Validate.notBlank(bundleKey, "A valid bundle ID is required");
        synchronized (mutex) {
            Bundle bundle = null;
            if (searchResultCache != null) {
                bundle = searchResultCache.get(String.format("%02d",
                        Integer.parseInt(bundleKey)));
            }
            if (bundle == null) {
                LOGGER.warning("A valid bundle ID is required");
                return;
            }
            addOnInfo(bundle, bundle.getBundleVersion(bundleKey));
        }
    }

    private AddOnStabilityLevel checkAddOnStabilityLevel(
            AddOnStabilityLevel addOnStabilityLevel) {
        if (addOnStabilityLevel == null) {
            addOnStabilityLevel = AddOnStabilityLevel.fromLevel(prefs.getInt(
                    ADDON_UPGRADE_STABILITY_LEVEL, /* default */
                    AddOnStabilityLevel.RELEASE.getLevel()));
        }
        return addOnStabilityLevel;
    }

    private int countBundles() {
        final BundleContext bc = context.getBundleContext();
        if (bc != null) {
            final org.osgi.framework.Bundle[] bundles = bc.getBundles();
            if (bundles != null) {
                return bundles.length;
            }
        }
        return 0;
    }

    protected void deactivate(final ComponentContext context) {
        if (rooBotEagerDownload != null && rooBotEagerDownload.isAlive()) {
            rooBotEagerDownload = null;
        }
    }

    private List<Bundle> filterList(final List<Bundle> bundles,
            final boolean trustedOnly, final boolean compatibleOnly,
            final boolean communityOnly, final String requiresCommand,
            final boolean onlyRelevantBundles) {
        final List<Bundle> filteredList = new ArrayList<Bundle>();
        List<PGPPublicKeyRing> keys = null;
        if (trustedOnly) {
            keys = pgpService.getTrustedKeys();
        }
        bundle_loop: for (final Bundle bundle : bundles) {
            final BundleVersion latest = bundle.getLatestVersion();
            if (onlyRelevantBundles && !(bundle.getSearchRelevance() > 0)) {
                continue bundle_loop;
            }
            if (trustedOnly && !isTrustedKey(keys, latest.getPgpKey())) {
                continue bundle_loop;
            }
            if (communityOnly
                    && latest
                            .getObrUrl()
                            .equals("http://spring-roo-repository.springsource.org/repository.xml")) {
                continue bundle_loop;
            }
            if (compatibleOnly && !isCompatible(latest.getRooVersion())) {
                continue bundle_loop;
            }
            if (isBundleInstalled(bundle)) {
                continue bundle_loop;
            }
            if (requiresCommand != null && requiresCommand.length() > 0) {
                boolean matchingCommand = false;
                for (final String cmd : latest.getCommands().keySet()) {
                    if (cmd.startsWith(requiresCommand)
                            || requiresCommand.startsWith(cmd)) {
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

    public List<Bundle> findAddons(final boolean showFeedback,
            final String searchTerms, boolean refresh,
            final int linesPerResult, int maxResults,
            final boolean trustedOnly, final boolean compatibleOnly,
            final boolean communityOnly, final String requiresCommand) {
        synchronized (mutex) {
            if (maxResults > 99) {
                maxResults = 99;
            }
            if (maxResults < 1) {
                maxResults = 10;
            }
            if (bundleCache.isEmpty()) {
                // We should refresh regardless in this case
                refresh = true;
            }
            if (refresh && populateBundleCache(false)) {
                if (showFeedback) {
                    LOGGER.info("Successfully downloaded Roo add-on Data");
                }
            }
            if (bundleCache.size() != 0) {
                boolean onlyRelevantBundles = false;
                if (searchTerms != null && !"".equals(searchTerms)) {
                    onlyRelevantBundles = true;
                    final String[] terms = searchTerms.split(",");
                    for (final Bundle bundle : bundleCache.values()) {
                        // First set relevance of all bundles to zero
                        bundle.setSearchRelevance(0f);
                        int hits = 0;
                        final BundleVersion latest = bundle.getLatestVersion();
                        for (final String term : terms) {
                            if ((bundle.getSymbolicName() + ";" + latest
                                    .getSummary()).toLowerCase().contains(
                                    term.trim().toLowerCase())
                                    || term.equals("*")) {
                                hits++;
                            }
                        }
                        bundle.setSearchRelevance(hits / terms.length);
                    }
                }
                final List<Bundle> bundles = Bundle
                        .orderBySearchRelevance(new ArrayList<Bundle>(
                                bundleCache.values()));
                final List<Bundle> filteredSearchResults = filterList(bundles,
                        trustedOnly, compatibleOnly, communityOnly,
                        requiresCommand, onlyRelevantBundles);
                if (showFeedback) {
                    printResultList(filteredSearchResults, maxResults,
                            linesPerResult);
                }
                return filteredSearchResults;
            }

            // There is a problem with the add-on index
            if (showFeedback) {
                LOGGER.info("No add-ons known. Are you online? Try the 'download status' command");
            }

            return null;
        }
    }

    public Map<String, Bundle> getAddOnCache(final boolean refresh) {
        synchronized (mutex) {
            if (refresh) {
                populateBundleCache(false);
            }
            return Collections.unmodifiableMap(bundleCache);
        }
    }

    private Map<String, Bundle> getUpgradableBundles(
            final AddOnStabilityLevel asl) {
        final Map<String, Bundle> bundles = new HashMap<String, Bundle>();
        if (context == null) {
            return bundles;
        }
        final BundleContext bundleContext = context.getBundleContext();
        for (final org.osgi.framework.Bundle bundle : bundleContext
                .getBundles()) {
            final Bundle b = bundleCache.get(bundle.getSymbolicName());
            if (b == null) {
                continue;
            }
            final BundleVersion bundleVersion = b.getLatestVersion();
            final String rooBotBundleVersion = bundleVersion.getVersion();
            final Object ebv = bundle.getHeaders().get("Bundle-Version");
            if (ebv == null) {
                continue;
            }
            final String exisingBundleVersion = ebv.toString().trim();
            if (isCompatible(b.getLatestVersion().getRooVersion())
                    && rooBotBundleVersion
                            .compareToIgnoreCase(exisingBundleVersion) > 0
                    && asl.getLevel() > AddOnStabilityLevel
                            .getAddOnStabilityLevel(exisingBundleVersion)) {

                bundles.put(b.getSymbolicName() + ";" + exisingBundleVersion, b);
            }
        }
        return bundles;
    }

    private String getVersionForCompatibility() {
        return UaaRegistrationService.SPRING_ROO.getMajorVersion() + "."
                + UaaRegistrationService.SPRING_ROO.getMinorVersion();
    }

    private InstallOrUpgradeStatus installAddon(
            final BundleVersion bundleVersion, final String bsn) {
        final InstallOrUpgradeStatus status = installOrUpgradeAddOn(
                bundleVersion, bsn, true);
        switch (status) {
        case SUCCESS:
            LOGGER.info("Successfully installed add-on: "
                    + bundleVersion.getPresentationName() + " [version: "
                    + bundleVersion.getVersion() + "]");
            LOGGER.warning("[Hint] Please consider rating this add-on with the following command:");
            LOGGER.warning("[Hint] addon feedback bundle --bundleSymbolicName "
                    + bsn.substring(
                            0,
                            bsn.indexOf(";") != -1 ? bsn.indexOf(";") : bsn
                                    .length())
                    + " --rating ... --comment \"...\"");
            break;
        case SHELL_RESTART_NEEDED:
            LOGGER.warning("You have upgraded a Roo core addon. To complete this installation please restart the Roo shell.");
            break;
        case PGP_VERIFICATION_NEEDED:
            LOGGER.warning("PGP verification of the bundle required");
            break;
        default:
            LOGGER.warning("Unable to install add-on: "
                    + bundleVersion.getPresentationName() + " [version: "
                    + bundleVersion.getVersion() + "]");
            break;
        }

        return status;
    }

    public InstallOrUpgradeStatus installAddOn(final AddOnBundleSymbolicName bsn) {
        synchronized (mutex) {
            Validate.notNull(bsn,
                    "A valid add-on bundle symbolic name is required");
            String bsnString = bsn.getKey();
            if (bsnString.contains(";")) {
                bsnString = bsnString.split(";")[0];
            }
            final Bundle bundle = bundleCache.get(bsnString);
            if (bundle == null) {
                LOGGER.warning("Could not find specified bundle with symbolic name: "
                        + bsn.getKey());
                return InstallOrUpgradeStatus.FAILED;
            }
            return installAddon(bundle.getBundleVersion(bsn.getKey()),
                    bsn.getKey());
        }
    }

    public InstallOrUpgradeStatus installAddOn(final String bundleKey) {
        synchronized (mutex) {
            Validate.notBlank(bundleKey, "A valid bundle ID is required");
            Bundle bundle = null;
            if (searchResultCache != null) {
                bundle = searchResultCache.get(String.format("%02d",
                        Integer.parseInt(bundleKey)));
            }
            if (bundle == null) {
                LOGGER.warning("To install an addon a valid bundle ID is required");
                return InstallOrUpgradeStatus.FAILED;
            }
            return installAddon(bundle.getBundleVersion(bundleKey),
                    bundle.getSymbolicName());
        }
    }

    private InstallOrUpgradeStatus installOrUpgradeAddOn(
            final BundleVersion bundleVersion, final String bsn,
            final boolean install) {
        if (!verifyRepository(bundleVersion.getObrUrl())) {
            return InstallOrUpgradeStatus.INVALID_OBR_URL;
        }

        final int count = countBundles();
        final boolean requiresWrappedCoreDependency = bundleVersion
                .getDescription().contains("#wrappedCoreDependency");

        boolean success = !(requiresWrappedCoreDependency && !shell
                .executeCommand("osgi obr url add --url http://spring-roo-repository.springsource.org/repository.xml"));
        success &= shell.executeCommand("osgi obr url add --url "
                + bundleVersion.getObrUrl());
        success &= shell.executeCommand("osgi obr deploy --bundleSymbolicName "
                + bsn);
        success &= shell.executeCommand("osgi obr url remove --url "
                + bundleVersion.getObrUrl());
        success &= !(requiresWrappedCoreDependency && !shell
                .executeCommand("osgi obr url remove --url http://spring-roo-repository.springsource.org/repository.xml"));

        if (install && count == countBundles()) {
            // Most likely PgP verification required before the bundle can be
            // installed, no log needed
            return InstallOrUpgradeStatus.PGP_VERIFICATION_NEEDED;
        }
        return success ? InstallOrUpgradeStatus.SUCCESS
                : InstallOrUpgradeStatus.FAILED;
    }

    private boolean isBundleInstalled(final Bundle search) {
        final BundleContext bundleContext = context.getBundleContext();
        for (final org.osgi.framework.Bundle bundle : bundleContext
                .getBundles()) {
            final String bsn = (String) bundle.getHeaders().get(
                    "Bundle-SymbolicName");
            if (StringUtils.isNotBlank(bsn)
                    && bsn.equals(search.getSymbolicName())) {
                return true;
            }
        }
        return false;
    }

    private boolean isCompatible(final String version) {
        return version.equals(getVersionForCompatibility());
    }

    @SuppressWarnings("unchecked")
    private boolean isTrustedKey(final List<PGPPublicKeyRing> keys,
            final String keyId) {
        for (final PGPPublicKeyRing keyRing : keys) {
            final Iterator<PGPPublicKey> it = keyRing.getPublicKeys();
            while (it.hasNext()) {
                final PGPPublicKey pgpKey = it.next();
                if (new PgpKeyId(pgpKey).equals(new PgpKeyId(keyId))) {
                    return true;
                }
            }
        }
        return false;
    }

    public void listAddOns(boolean refresh, final int linesPerResult,
            final int maxResults, final boolean trustedOnly,
            final boolean compatibleOnly, final boolean communityOnly,
            final String requiresCommand) {
        synchronized (mutex) {
            if (bundleCache.isEmpty()) {
                // We should refresh regardless in this case
                refresh = true;
            }
            if (refresh && populateBundleCache(false)) {
                LOGGER.info("Successfully downloaded Roo add-on Data");
            }
            if (bundleCache.size() != 0) {
                final List<Bundle> bundles = Bundle
                        .orderByRanking(new ArrayList<Bundle>(bundleCache
                                .values()));
                final List<Bundle> filteredList = filterList(bundles,
                        trustedOnly, compatibleOnly, communityOnly,
                        requiresCommand, false);
                printResultList(filteredList, maxResults, linesPerResult);
            }
            else {
                LOGGER.info("No add-ons known. Are you online? Try the 'download status' command");
            }
        }
    }

    private void logInfo(final String label, String content) {
        final StringBuilder sb = new StringBuilder();
        sb.append(label);
        for (int i = 0; i < 13 - label.length(); i++) {
            sb.append(".");
        }
        sb.append(": ");
        if (content.length() < 65) {
            sb.append(content);
            LOGGER.info(sb.toString());
        }
        else {
            final List<String> split = new ArrayList<String>(
                    Arrays.asList(content.split("\\s")));
            if (split.size() == 1) {
                while (content.length() > 65) {
                    sb.append(content.substring(0, 65));
                    content = content.substring(65);
                    LOGGER.info(sb.toString());
                    sb.setLength(0);
                    sb.append("               ");
                }
                if (content.length() > 0) {
                    LOGGER.info(sb.append(content).toString());
                }
            }
            else {
                while (split.size() > 0) {
                    while (!split.isEmpty()
                            && split.get(0).length() + sb.length() < 79) {
                        sb.append(split.get(0)).append(" ");
                        split.remove(0);
                    }
                    LOGGER.info(sb.toString().substring(0,
                            sb.toString().length() - 1));
                    sb.setLength(0);
                    sb.append("               ");
                }
            }
        }
    }

    private boolean populateBundleCache(final boolean startupTime) {
        boolean success = false;
        InputStream is = null;
        ByteArrayInputStream bais = null;
        ByteArrayOutputStream baos = null;
        try {
            final DocumentBuilderFactory dbf = DocumentBuilderFactory
                    .newInstance();
            final DocumentBuilder db = dbf.newDocumentBuilder();
            if (rooBotXmlUrl.startsWith("http://")) {
                // Handle it as HTTP
                final URL httpUrl = new URL(rooBotXmlUrl);
                final String failureMessage = urlInputStreamService
                        .getUrlCannotBeOpenedMessage(httpUrl);
                if (failureMessage != null) {
                    if (!startupTime) {
                        // This wasn't just an eager startup time attempt, so
                        // let's display the error reason
                        // (for startup time, we just fail quietly)
                        LOGGER.warning(failureMessage);
                    }
                    return false;
                }
                // It appears we can acquire the URL, so let's do it
                is = urlInputStreamService.openConnection(httpUrl);
            }
            else {
                // Fall back to normal protocol handler (likely in local
                // development testing etc)
                is = new URL(rooBotXmlUrl).openStream();
            }
            if (is == null) {
                LOGGER.warning("Could not connect to Roo Addon bundle repository index");
                return false;
            }

            final ZipInputStream zip = new ZipInputStream(is);
            zip.getNextEntry();

            baos = new ByteArrayOutputStream();
            IOUtils.copy(zip, baos);

            bais = new ByteArrayInputStream(baos.toByteArray());
            final Document roobotXml = db.parse(bais);
            if (roobotXml != null) {
                populateBundleCache(roobotXml);
                success = true;
            }
            zip.close();
        }
        catch (final Throwable ignored) {
        }
        finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(baos);
            IOUtils.closeQuietly(bais);
        }
        if (success && startupTime) {
            printAddonStats();
        }
        return success;
    }

    private void populateBundleCache(final Document roobotXml)
            throws ParseException {
        bundleCache.clear();
        for (final Element bundleElement : XmlUtils.findElements(
                "/roobot/bundles/bundle", roobotXml.getDocumentElement())) {
            final String bsn = bundleElement.getAttribute("bsn");
            if (NO_UPGRADE_BSN_LIST.contains(bsn)) {
                // List only add-ons which are not core (see ROO-2190)
                continue;
            }
            final List<Comment> comments = new ArrayList<Comment>();
            for (final Element commentElement : XmlUtils.findElements(
                    "comments/comment", bundleElement)) {
                comments.add(new Comment(Rating.fromInt(new Integer(
                        commentElement.getAttribute("rating"))), commentElement
                        .getAttribute("comment"), dateFormat
                        .parse(commentElement.getAttribute("date"))));
            }
            final Bundle bundle = new Bundle(bundleElement.getAttribute("bsn"),
                    new Float(bundleElement.getAttribute("uaa-ranking")),
                    comments);
            for (final Element versionElement : XmlUtils.findElements(
                    "versions/version", bundleElement)) {
                if (bsn != null && bsn.length() > 0 && versionElement != null) {
                    String signedBy = "";
                    final String pgpKey = versionElement
                            .getAttribute("pgp-key-id");
                    if (pgpKey != null && pgpKey.length() > 0) {
                        final Element pgpSigned = XmlUtils.findFirstElement(
                                "/roobot/pgp-keys/pgp-key[@id='" + pgpKey
                                        + "']/pgp-key-description",
                                roobotXml.getDocumentElement());
                        if (pgpSigned != null) {
                            signedBy = pgpSigned.getAttribute("text");
                        }
                    }

                    final Map<String, String> commands = new HashMap<String, String>();
                    for (final Element shell : XmlUtils.findElements(
                            "shell-commands/shell-command", versionElement)) {
                        commands.put(shell.getAttribute("command"),
                                shell.getAttribute("help"));
                    }

                    final StringBuilder versionBuilder = new StringBuilder();
                    versionBuilder.append(versionElement.getAttribute("major"))
                            .append(".")
                            .append(versionElement.getAttribute("minor"));
                    final String versionMicro = versionElement
                            .getAttribute("micro");
                    if (versionMicro != null && versionMicro.length() > 0) {
                        versionBuilder.append(".").append(versionMicro);
                    }
                    final String versionQualifier = versionElement
                            .getAttribute("qualifier");
                    if (versionQualifier != null
                            && versionQualifier.length() > 0) {
                        versionBuilder.append(".").append(versionQualifier);
                    }

                    String rooVersion = versionElement
                            .getAttribute("roo-version");
                    if (rooVersion.equals("*") || rooVersion.length() == 0) {
                        rooVersion = getVersionForCompatibility();
                    }
                    else {
                        final String[] split = rooVersion.split("\\.");
                        if (split.length > 2) {
                            // Only interested in major.minor
                            rooVersion = split[0] + "." + split[1];
                        }
                    }
                    final BundleVersion version = new BundleVersion(
                            versionElement.getAttribute("url"),
                            versionElement.getAttribute("obr-url"),
                            versionBuilder.toString(),
                            versionElement.getAttribute("name"),
                            new Long(versionElement.getAttribute("size"))
                                    .longValue(),
                            versionElement.getAttribute("description"), pgpKey,
                            signedBy, rooVersion, commands);
                    // For security reasons we ONLY accept httppgp://
                    // add-on versions
                    if (!version.getUri().startsWith("httppgp://")) {
                        continue;
                    }
                    bundle.addVersion(version);
                }
                bundleCache.put(bsn, bundle);
            }
        }
    }

    private void printAddonStats() {
        String msg = null;
        final AddOnStabilityLevel currentLevel = AddOnStabilityLevel
                .fromLevel(prefs.getInt(ADDON_UPGRADE_STABILITY_LEVEL,
                        AddOnStabilityLevel.RELEASE.getLevel()));
        final Map<String, Bundle> currentLevelBundles = getUpgradableBundles(currentLevel);
        if (currentLevelBundles.size() > 0) {
            msg = currentLevelBundles.size() + " upgrade"
                    + (currentLevelBundles.size() > 1 ? "s" : "")
                    + " available";
        }
        final Map<String, Bundle> anyLevelBundles = getUpgradableBundles(AddOnStabilityLevel.ANY);
        if (anyLevelBundles.size() != 0) {
            if (msg == null) {
                msg = "0 upgrades available";
            }
            final int plusSize = anyLevelBundles.size()
                    - currentLevelBundles.size();
            msg += " (plus " + plusSize + " upgrade"
                    + (plusSize > 1 ? "s" : "")
                    + " not visible due to your version stability setting of "
                    + currentLevel.name() + ")";
        }
        if (msg != null) {
            Thread.currentThread().setName(""); // Prevent thread name from
                                                // being presented in Roo shell
            LOGGER.info(msg);
        }
    }

    private void printResultList(final Collection<Bundle> bundles,
            int maxResults, final int linesPerResult) {
        int bundleId = 1;
        searchResultCache.clear();
        final StringBuilder sb = new StringBuilder();
        final List<PGPPublicKeyRing> keys = pgpService.getTrustedKeys();
        LOGGER.info(bundles.size()
                + " found, sorted by rank; T = trusted developer; R = Roo "
                + getVersionForCompatibility() + " compatible");
        LOGGER.warning("ID T R DESCRIPTION -------------------------------------------------------------");
        for (final Bundle bundle : bundles) {
            if (maxResults-- == 0) {
                break;
            }
            final BundleVersion latest = bundle.getLatestVersion();
            final String bundleKey = String.format("%02d", bundleId++);
            searchResultCache.put(bundleKey, bundle);
            sb.append(bundleKey);
            sb.append(isTrustedKey(keys, latest.getPgpKey()) ? " Y " : " - ");
            sb.append(isCompatible(latest.getRooVersion()) ? "Y " : "- ");
            sb.append(latest.getVersion());
            sb.append(" ");
            final List<String> split = new ArrayList<String>(
                    Arrays.asList(latest.getDescription().split("\\s")));
            int lpr = linesPerResult;
            while (split.size() > 0 && --lpr >= 0) {
                while (!split.isEmpty()
                        && split.get(0).length() + sb.length() < (lpr == 0 ? 77
                                : 80)) {
                    sb.append(split.get(0)).append(" ");
                    split.remove(0);
                }
                String line = sb.toString().substring(0,
                        sb.toString().length() - 1);
                if (lpr == 0 && split.size() > 0) {
                    line += "...";
                }
                LOGGER.info(line);
                sb.setLength(0);
                sb.append("       ");
            }
            if (sb.toString().trim().length() > 0) {
                LOGGER.info(sb.toString());
            }
            sb.setLength(0);
        }
        printSeparator();
        LOGGER.info("[HINT] use 'addon info id --searchResultId ..' to see details about a search result");
        LOGGER.info("[HINT] use 'addon install id --searchResultId ..' to install a specific search result, or");
        LOGGER.info("[HINT] use 'addon install bundle --bundleSymbolicName TAB' to install a specific add-on version");
    }

    private void printSeparator() {
        LOGGER.warning("--------------------------------------------------------------------------------");
    }

    public InstallOrUpgradeStatus removeAddOn(final BundleSymbolicName bsn) {
        synchronized (mutex) {
            Validate.notNull(bsn, "Bundle symbolic name required");
            boolean success = false;
            final int count = countBundles();
            success = shell
                    .executeCommand("osgi uninstall --bundleSymbolicName "
                            + bsn.getKey());
            InstallOrUpgradeStatus status;
            if (count == countBundles() || !success) {
                LOGGER.warning("Unable to remove add-on: " + bsn.getKey());
                status = InstallOrUpgradeStatus.FAILED;
            }
            else {
                LOGGER.info("Successfully removed add-on: " + bsn.getKey());
                status = InstallOrUpgradeStatus.SUCCESS;
            }
            return status;
        }
    }

    public Integer searchAddOns(final boolean showFeedback,
            final String searchTerms, final boolean refresh,
            final int linesPerResult, final int maxResults,
            final boolean trustedOnly, final boolean compatibleOnly,
            final boolean communityOnly, final String requiresCommand) {
        final List<Bundle> result = findAddons(showFeedback, searchTerms,
                refresh, linesPerResult, maxResults, trustedOnly,
                compatibleOnly, communityOnly, requiresCommand);
        return result != null ? result.size() : null;
    }

    public InstallOrUpgradeStatus upgradeAddOn(final AddOnBundleSymbolicName bsn) {
        synchronized (mutex) {
            Validate.notNull(bsn,
                    "A valid add-on bundle symbolic name is required");
            String bsnString = bsn.getKey();
            if (bsnString.contains(";")) {
                bsnString = bsnString.split(";")[0];
            }
            final Bundle bundle = bundleCache.get(bsnString);
            if (bundle == null) {
                LOGGER.warning("Could not find specified bundle with symbolic name: "
                        + bsn.getKey());
                return InstallOrUpgradeStatus.FAILED;
            }
            final BundleVersion bundleVersion = bundle.getBundleVersion(bsn
                    .getKey());
            final InstallOrUpgradeStatus status = installOrUpgradeAddOn(
                    bundleVersion, bsn.getKey(), false);
            if (status.equals(InstallOrUpgradeStatus.SUCCESS)) {
                LOGGER.info("Successfully upgraded: "
                        + bundle.getSymbolicName() + " [version: "
                        + bundleVersion.getVersion() + "]");
                LOGGER.warning("Please restart the Roo shell to complete the upgrade");
            }
            else if (status.equals(InstallOrUpgradeStatus.FAILED)) {
                LOGGER.warning("Unable to upgrade: " + bundle.getSymbolicName()
                        + " [version: " + bundleVersion.getVersion() + "]");
            }
            return status;
        }
    }

    public InstallOrUpgradeStatus upgradeAddOn(final String bundleId) {
        synchronized (mutex) {
            Validate.notBlank(bundleId, "A valid bundle ID is required");
            Bundle bundle = null;
            if (searchResultCache != null) {
                bundle = searchResultCache.get(String.format("%02d",
                        Integer.parseInt(bundleId)));
            }
            if (bundle == null) {
                LOGGER.warning("A valid bundle ID is required");
                return InstallOrUpgradeStatus.FAILED;
            }
            final BundleVersion bundleVersion = bundle
                    .getBundleVersion(bundleId);
            final InstallOrUpgradeStatus status = installOrUpgradeAddOn(
                    bundleVersion, bundle.getSymbolicName(), false);
            if (status.equals(InstallOrUpgradeStatus.SUCCESS)) {
                LOGGER.info("Successfully upgraded: "
                        + bundle.getSymbolicName() + " [version: "
                        + bundleVersion.getVersion() + "]");
                LOGGER.warning("Please restart the Roo shell to complete the upgrade");
            }
            else if (status.equals(InstallOrUpgradeStatus.FAILED)) {
                LOGGER.warning("Unable to upgrade: " + bundle.getSymbolicName()
                        + " [version: " + bundleVersion.getVersion() + "]");
            }
            return status;
        }
    }

    public void upgradeAddOns() {
        synchronized (mutex) {
            final AddOnStabilityLevel addonStabilityLevel = checkAddOnStabilityLevel(null);
            final Map<String, Bundle> bundles = getUpgradableBundles(addonStabilityLevel);
            boolean upgraded = false;
            for (final Bundle bundle : bundles.values()) {
                final BundleVersion bundleVersion = bundle.getLatestVersion();
                final InstallOrUpgradeStatus status = installOrUpgradeAddOn(
                        bundleVersion, bundle.getSymbolicName(), false);
                if (status.equals(InstallOrUpgradeStatus.SUCCESS)) {
                    LOGGER.info("Successfully upgraded: "
                            + bundle.getSymbolicName() + " [version: "
                            + bundleVersion.getVersion() + "]");
                    upgraded = true;
                }
                else if (status.equals(InstallOrUpgradeStatus.FAILED)) {
                    LOGGER.warning("Unable to upgrade: "
                            + bundle.getSymbolicName() + " [version: "
                            + bundleVersion.getVersion() + "]");
                }
            }
            if (upgraded) {
                LOGGER.warning("Please restart the Roo shell to complete the upgrade");
            }
            else {
                LOGGER.info("No add-ons / components are available for upgrade for level: "
                        + addonStabilityLevel.name());
            }
        }
    }

    public void upgradesAvailable(AddOnStabilityLevel addonStabilityLevel) {
        synchronized (mutex) {
            addonStabilityLevel = checkAddOnStabilityLevel(addonStabilityLevel);
            final Map<String, Bundle> bundles = getUpgradableBundles(addonStabilityLevel);
            if (bundles.isEmpty()) {
                LOGGER.info("No add-ons / components are available for upgrade for level: "
                        + addonStabilityLevel.name());
            }
            else {
                LOGGER.info("The following add-ons / components are available for upgrade for level: "
                        + addonStabilityLevel.name());
                printSeparator();
                for (final Entry<String, Bundle> entry : bundles.entrySet()) {
                    final BundleVersion latest = entry.getValue()
                            .getLatestVersion();
                    if (latest != null) {
                        LOGGER.info("[level: "
                                + AddOnStabilityLevel.fromLevel(
                                        AddOnStabilityLevel
                                                .getAddOnStabilityLevel(latest
                                                        .getVersion())).name()
                                + "] " + entry.getKey() + " > "
                                + latest.getVersion());
                    }
                }
                printSeparator();
            }
        }
    }

    public void upgradeSettings(AddOnStabilityLevel addOnStabilityLevel) {
        if (addOnStabilityLevel == null) {
            addOnStabilityLevel = checkAddOnStabilityLevel(null);
            LOGGER.info("Current Add-on Stability Level: "
                    + addOnStabilityLevel.name());
        }
        else {
            boolean success = true;
            prefs.putInt(ADDON_UPGRADE_STABILITY_LEVEL,
                    addOnStabilityLevel.getLevel());
            try {
                prefs.flush();
            }
            catch (final IllegalStateException ignore) {
                success = false;
            }
            if (success) {
                LOGGER.info("Add-on Stability Level: "
                        + addOnStabilityLevel.name() + " stored");
            }
            else {
                LOGGER.warning("Unable to store add-on stability level at this time");
            }
        }
    }

    private boolean verifyRepository(final String repoUrl) {
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document doc = null;
        try {
            URL obrUrl = null;
            obrUrl = new URL(repoUrl);
            final DocumentBuilder db = dbf.newDocumentBuilder();
            if (obrUrl.toExternalForm().endsWith(".zip")) {
                ByteArrayInputStream bais = null;
                ByteArrayOutputStream baos = null;
                ZipInputStream zip = null;
                try {
                    zip = new ZipInputStream(obrUrl.openStream());
                    zip.getNextEntry();

                    baos = new ByteArrayOutputStream();
                    final byte[] buffer = new byte[8192];
                    int length = -1;
                    while (zip.available() > 0) {
                        length = zip.read(buffer, 0, 8192);
                        if (length > 0) {
                            baos.write(buffer, 0, length);
                        }
                    }
                    bais = new ByteArrayInputStream(baos.toByteArray());
                    doc = db.parse(bais);
                }
                finally {
                    IOUtils.closeQuietly(zip);
                    IOUtils.closeQuietly(bais);
                    IOUtils.closeQuietly(baos);
                }
            }
            else {
                doc = db.parse(obrUrl.openStream());
            }
            Validate.notNull(doc,
                    "RooBot was unable to parse the repository document of this add-on");
            for (final Element resource : XmlUtils.findElements("resource",
                    doc.getDocumentElement())) {
                if (resource.hasAttribute("uri")) {
                    if (!resource.getAttribute("uri").startsWith("httppgp")) {
                        LOGGER.warning("Sorry, the resource "
                                + resource.getAttribute("uri")
                                + " does not follow HTTPPGP conventions mangraded by Spring Roo so the OBR file at "
                                + repoUrl + " is unacceptable at this time");
                        return false;
                    }
                }
            }
            doc = null;
        }
        catch (final Exception e) {
            throw new IllegalStateException(
                    "RooBot was unable to parse the repository document of this add-on",
                    e);
        }
        return true;
    }
}