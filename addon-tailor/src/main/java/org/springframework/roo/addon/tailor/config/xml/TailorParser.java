package org.springframework.roo.addon.tailor.config.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.springframework.roo.addon.tailor.actions.ActionConfig;
import org.springframework.roo.addon.tailor.config.CommandConfiguration;
import org.springframework.roo.addon.tailor.config.TailorConfiguration;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class TailorParser {
    private static final Logger LOGGER = HandlerUtils
            .getLogger(TailorParser.class);

    /**
     * Maps the XML file contents to a TailorConfiguration object. It is
     * possible to have multiple configurations in tailor.xml
     * 
     * @param root
     * @return list of tailor configurations
     */
    public static List<TailorConfiguration> mapXmlToTailorConfiguration(
            Element root) {
        List<Element> elTailors = XmlUtils.findElements(
                "/tailorconfiguration/tailor", root);
        if (elTailors.isEmpty()) {
            logTailorXMLInvalid("no <tailor> definitions found in <tailorconfiguration> root element");
            return null;
        }
        List<TailorConfiguration> configs = new ArrayList<TailorConfiguration>();
        for (Element eTailor : elTailors) {
            TailorConfiguration config = parseTailorConfiguration(eTailor);
            if (config != null) {
                configs.add(config);
            }
        }
        return configs;
    }

    /**
     * Maps the single XML tailor configuration to a TailorConfiguration object.
     * 
     * @param tailor element
     * @return tailor configurations
     */
    public static TailorConfiguration parseTailorConfiguration(Element elTailor) {

        if (StringUtils.isBlank(elTailor.getAttribute("name"))) {
            logTailorXMLInvalid("<tailor> must have a name attribute");
            return null;
        }

        TailorConfiguration result = new TailorConfiguration(
                elTailor.getAttribute("name"),
                elTailor.getAttribute("description"));

        String activeAttribute = elTailor.getAttribute("activate");
        if (StringUtils.isNotBlank(activeAttribute)) {
            boolean isActive = "true".equalsIgnoreCase(activeAttribute)
                    || "yes".equalsIgnoreCase(activeAttribute);
            result.setActive(isActive);
        }

        List<Element> elConfigs = XmlUtils.findElements("config", elTailor);
        if (elConfigs.isEmpty()) {
            logTailorXMLInvalid("<tailor> must have <config> child elements");
            return null;
        }

        for (Element elConfig : elConfigs) {
            String command = elConfig.getAttribute("command");
            if (StringUtils.isBlank(command)) {
                logTailorXMLInvalid("found <config> without command attribute");
                return null;
            }

            CommandConfiguration newCmdConfig = new CommandConfiguration();
            newCmdConfig.setCommandName(command);
            List<Element> elActions = XmlUtils.findElements("action", elConfig);
            for (Element elAction : elActions) {
                // Determine the action type
                if (StringUtils.isBlank(elAction.getAttribute("type"))) {
                    logTailorXMLInvalid("found <action> without type attribute");
                    return null;
                }
                ActionConfig newAction = new ActionConfig(
                        elAction.getAttribute("type"));
                NamedNodeMap attributes = elAction.getAttributes();
                for (int i = 0; i < attributes.getLength(); i++) {
                    Node item = attributes.item(i);
                    String attributeKey = item.getNodeName();
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

    private static void logTailorXMLInvalid(String msg) {
        LOGGER.warning("Invalid tailor.xml - please correct and restart the shell to use this configuration ("
                + msg + ")");
    }

}
