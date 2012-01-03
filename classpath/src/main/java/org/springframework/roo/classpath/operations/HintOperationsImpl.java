package org.springframework.roo.classpath.operations;

import java.io.File;
import java.math.BigDecimal;
import java.util.Enumeration;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.shell.AbstractShell;
import org.springframework.roo.support.util.NumberUtils;
import org.springframework.roo.support.util.StringUtils;

/**
 * Base implementation of {@link HintOperations}.
 * <p>
 * This implementation relies on a predefined resource bundle containing all of
 * this hints. This is likely to be replaced in the future with a more
 * extensible implementation so third-party add-ons can provide their own hints
 * (see ROO-610 for details).
 * 
 * @author Ben Alex
 * @author Alan Stewart
 * @since 1.1
 */
@Service
@Component
public class HintOperationsImpl implements HintOperations {

    // Constants
    private static final String ANT_MATCH_DIRECTORY_PATTERN = File.separator
            + "**" + File.separator;
    private static ResourceBundle bundle = ResourceBundle
            .getBundle(HintCommands.class.getName());

    // Fields
    @Reference private FileManager fileManager;
    @Reference private PathResolver pathResolver;
    @Reference private ProjectOperations projectOperations;

    public String hint(String topic) {
        if (StringUtils.isBlank(topic)) {
            topic = determineTopic();
        }
        try {
            String message = bundle.getString(topic);
            return message.replace("\r", StringUtils.LINE_SEPARATOR).replace(
                    "${completion_key}", AbstractShell.completionKeys);
        }
        catch (MissingResourceException exception) {
            return "Cannot find topic '" + topic + "'";
        }
    }

    public SortedSet<String> getCurrentTopics() {
        SortedSet<String> result = new TreeSet<String>();
        String topic = determineTopic();
        if ("general".equals(topic)) {
            for (Enumeration<String> keys = bundle.getKeys(); keys
                    .hasMoreElements();) {
                result.add(keys.nextElement());
            }
            // result.addAll(bundle.keySet()); ResourceBundle.keySet() method in
            // JDK 6+
        }
        else {
            result.add(topic);
        }
        return result;
    }

    private String determineTopic() {
        if (!projectOperations.isFocusedProjectAvailable()) {
            return "start";
        }

        if (!(fileManager.exists(pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml")) || fileManager
                .exists(pathResolver.getFocusedIdentifier(
                        Path.SRC_MAIN_RESOURCES,
                        "META-INF/spring/applicationContext-mongo.xml")))) {
            return "persistence";
        }

        if (NumberUtils.max(getItdCount("Jpa_ActiveRecord"),
                getItdCount("Jpa_Entity"), getItdCount("Mongo_Entity"))
                .compareTo(BigDecimal.ZERO) == 0) {
            return "entities";
        }

        int javaBeanCount = getItdCount("JavaBean");
        if (javaBeanCount == 0) {
            return "fields";
        }

        return "general";
    }

    private int getItdCount(final String itdUniquenessFilenameSuffix) {
        return fileManager.findMatchingAntPath(
                pathResolver.getFocusedRoot(Path.SRC_MAIN_JAVA)
                        + ANT_MATCH_DIRECTORY_PATTERN + "*_Roo_"
                        + itdUniquenessFilenameSuffix + ".aj").size();
    }
}
