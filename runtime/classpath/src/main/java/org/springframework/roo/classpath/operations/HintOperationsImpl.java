package org.springframework.roo.classpath.operations;

import java.io.File;
import java.math.BigDecimal;
import java.util.Enumeration;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.shell.AbstractShell;

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

    private static final String ANT_MATCH_DIRECTORY_PATTERN = File.separator
            + "**" + File.separator;
    private static ResourceBundle bundle = ResourceBundle
            .getBundle(HintCommands.class.getName());

    @Reference private FileManager fileManager;
    @Reference private PathResolver pathResolver;
    @Reference private ProjectOperations projectOperations;

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

        if (new BigDecimal(NumberUtils.max(getItdCount("Jpa_ActiveRecord"),
                getItdCount("Jpa_Entity"), getItdCount("Mongo_Entity")))
                .compareTo(BigDecimal.ZERO) == 0) {
            return "entities";
        }

        final int javaBeanCount = getItdCount("JavaBean");
        if (javaBeanCount == 0) {
            return "fields";
        }

        return "general";
    }

    public SortedSet<String> getCurrentTopics() {
        final SortedSet<String> result = new TreeSet<String>();
        final String topic = determineTopic();
        if ("general".equals(topic)) {
            for (final Enumeration<String> keys = bundle.getKeys(); keys
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

    private int getItdCount(final String itdUniquenessFilenameSuffix) {
        return fileManager.findMatchingAntPath(
                pathResolver.getFocusedRoot(Path.SRC_MAIN_JAVA)
                        + ANT_MATCH_DIRECTORY_PATTERN + "*_Roo_"
                        + itdUniquenessFilenameSuffix + ".aj").size();
    }

    public String hint(String topic) {
        if (StringUtils.isBlank(topic)) {
            topic = determineTopic();
        }
        try {
            final String message = bundle.getString(topic);
            return message.replace("\r", IOUtils.LINE_SEPARATOR).replace(
                    "${completion_key}", AbstractShell.completionKeys);
        }
        catch (final MissingResourceException exception) {
            return "Cannot find topic '" + topic + "'";
        }
    }
}
