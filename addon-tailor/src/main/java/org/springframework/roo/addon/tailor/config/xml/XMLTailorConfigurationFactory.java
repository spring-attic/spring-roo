package org.springframework.roo.addon.tailor.config.xml;

import static org.springframework.roo.support.util.FileUtils.CURRENT_DIRECTORY;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.tailor.actions.ActionConfig;
import org.springframework.roo.addon.tailor.config.CommandConfiguration;
import org.springframework.roo.addon.tailor.config.TailorConfiguration;
import org.springframework.roo.addon.tailor.config.TailorConfigurationFactory;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.osgi.OSGiUtils;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

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
    public TailorConfiguration createTailorConfiguration() {

        // TODO: This factory could also look for this file in the user.dir:
        // So that wherever the user starts the shell, he would always have his
        // central "tailors" available
        final String configFileIdentifier = shellRootPath + "/tailor.xml";
        if (!fileManager.exists(configFileIdentifier)) {
            return null;
        }

        try {
            final Document readXml = XmlUtils.readXml(fileManager
                    .getInputStream(configFileIdentifier));
            final Element root = readXml.getDocumentElement();
            return mapXmlToTailorConfiguration(root);
        }
        catch (final Exception e) {
            // Make sure that an invalid tailor.xml file does not crash the
            // whole shell
            // TODO: Log exception only in development mode
            logTailorXMLInvalid("Error reading file ("
                    + e.getLocalizedMessage());
        }

        return null;

    }

    private void logTailorXMLInvalid(final String msg) {
        LOGGER.warning("Invalid tailor.xml - please correct and restart the shell to use this configuration ("
                + msg + ")");
    }

    /**
     * Maps the XML file contents to a TailorConfiguration object
     * 
     * @param root
     * @return
     */
    private TailorConfiguration mapXmlToTailorConfiguration(final Element root) {
        final List<Element> elTailors = XmlUtils.findElements(
                "/tailorconfiguration/tailor", root);
        // TODO: Currently only one tailor supported in the configuration file
        // > Should be extended to support multiple tailors, the XML already
        // supports this, but is not evaluated
        if (elTailors.isEmpty()) {
            logTailorXMLInvalid("no <tailor> definitions found in <tailorconfiguration> root element");
            return null;
        }

        final Element elTailor = elTailors.get(0);
        if (!StringUtils.hasLength(elTailor.getAttribute("name"))) {
            logTailorXMLInvalid("<tailor> must have a name attribute");
            return null;
        }

        final TailorConfiguration result = new TailorConfiguration(
                elTailor.getAttribute("name"),
                elTailor.getAttribute("description"));

        final List<Element> elConfigs = XmlUtils.findElements(
                "/tailorconfiguration/tailor/config", root);
        if (elConfigs.isEmpty()) {
            logTailorXMLInvalid("<tailor> must have <config> child elements");
            return null;
        }

        for (final Element elConfig : elConfigs) {
            final String command = elConfig.getAttribute("command");
            if (!StringUtils.hasText(command)) {
                logTailorXMLInvalid("found <config> without command attribute");
                return null;
            }

            final CommandConfiguration newCmdConfig = new CommandConfiguration();
            newCmdConfig.setCommandName(command);
            final List<Element> elActions = XmlUtils.findElements("action",
                    elConfig);
            for (final Element elAction : elActions) {
                // Determine the action type
                if (!StringUtils.hasText(elAction.getAttribute("type"))) {
                    logTailorXMLInvalid("found <action> without type attribute");
                    return null;
                }
                final ActionConfig newAction = new ActionConfig(
                        elAction.getAttribute("type"));
                final NamedNodeMap attributes = elAction.getAttributes();
                for (int i = 0; i < attributes.getLength(); i++) {
                    final Node item = attributes.item(i);
                    final String attributeKey = item.getNodeName();
                    if (!"type".equals(attributeKey)) {
                        newAction.setAttribute(attributeKey,
                                item.getNodeValue());
                    }
                }
                newCmdConfig.addAction(newAction);
            }
            result.addCommandConfig(newCmdConfig);

        }
        return result;
    }
}
