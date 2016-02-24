package org.springframework.roo.addon.tailor.config.xml;

import static org.springframework.roo.support.util.FileUtils.CURRENT_DIRECTORY;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.tailor.config.TailorConfiguration;
import org.springframework.roo.addon.tailor.config.TailorConfigurationFactory;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.osgi.OSGiUtils;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Factory to create a TailorConfiguration from an XML configuration file named
 * "tailor.xml" in the shell root.
 * 
 * @author Birgitta Boeckeler
 * @since 1.2.0
 */
@Component
@Service
public class XMLTailorConfigurationFactory implements
        TailorConfigurationFactory {

    private static final Logger LOGGER = HandlerUtils
            .getLogger(XMLTailorConfigurationFactory.class);

    @Reference FileManager fileManager;

    String shellRootPath = null;

    /**
     * Sample file:
     * 
     * <pre>
     * <tailorconfiguration>
     * 	<tailor name="tailorname" description="Tailor description">
     * 		<config command="inputcommand">
     * 			<action type="actionname" attribute="value"/>
     * 		</config>
     * </pre>
     */
    public List<TailorConfiguration> createTailorConfiguration() {
        String configFileIdentifier = shellRootPath + "/tailor.xml";
        if (!fileManager.exists(configFileIdentifier)) {
            configFileIdentifier = System.getProperty("user.home")
                    + "/tailor.xml";
            if (!fileManager.exists(configFileIdentifier)) {
                return null;
            }
        }

        try {
            final Document readXml = XmlUtils.readXml(fileManager
                    .getInputStream(configFileIdentifier));
            final Element root = readXml.getDocumentElement();
            return TailorParser.mapXmlToTailorConfiguration(root);
        }
        catch (final Exception e) {
            // Make sure that an invalid tailor.xml file does not crash the
            // whole shell
            logTailorXMLInvalid("Error reading file ("
                    + e.getLocalizedMessage());
        }

        return null;

    }

    // ------------ OSGi component methods ----------------
    protected void activate(final ComponentContext context) {
        // Load the root project directory at startup
        // Use this instead of the shell object, because shell.getHome()
        // sometimes
        // gives false results, e.g. when running Roo in test mode from Eclipse
        // (PathResolver cannot be used because the config file needs to be
        // available even
        // if there is no project created)
        final File shellDirectory = new File(StringUtils.defaultIfEmpty(
                OSGiUtils.getRooWorkingDirectory(context), CURRENT_DIRECTORY));
        shellRootPath = FileUtils.getCanonicalPath(shellDirectory);
    }

    private void logTailorXMLInvalid(final String msg) {
        LOGGER.warning("Invalid tailor.xml - please correct and restart the shell to use this configuration ("
                + msg + ")");
    }
}
