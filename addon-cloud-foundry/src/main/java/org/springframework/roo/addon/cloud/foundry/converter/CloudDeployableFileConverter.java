package org.springframework.roo.addon.cloud.foundry.converter;

import java.io.File;
import java.io.IOException;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.cloud.foundry.model.CloudDeployableFile;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.MavenOperationsImpl;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.shell.Completion;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Provides conversion to and from Cloud Foundry model classes.
 * 
 * @author James Tyrrell
 * @author Andrew Swan
 * @since 1.2.0
 */
@Component
@Service
public class CloudDeployableFileConverter implements
        Converter<CloudDeployableFile> {

    private static final String CREATE_OPTION = "CREATE";
    private static final Logger LOGGER = HandlerUtils
            .getLogger(CloudDeployableFileConverter.class);

    public static String escapeString(final String toEscape) {
        final StringBuilder result = new StringBuilder();
        final StringCharacterIterator iterator = new StringCharacterIterator(
                toEscape);
        char character = iterator.current();
        while (character != CharacterIterator.DONE) {
            switch (character) {
            case '.': {
                result.append("\\.");
                break;
            }
            case '\\': {
                result.append("\\\\");
                break;
            }
            case '?': {
                result.append("\\?");
                break;
            }
            case '*': {
                result.append("\\*");
                break;
            }
            case '+': {
                result.append("\\+");
                break;
            }
            case '&': {
                result.append("\\&");
                break;
            }
            case ':': {
                result.append("\\:");
                break;
            }
            case '{': {
                result.append("\\{");
                break;
            }
            case '}': {
                result.append("\\}");
                break;
            }
            case '[': {
                result.append("\\[");
                break;
            }
            case ']': {
                result.append("\\]");
                break;
            }
            case '(': {
                result.append("\\(");
                break;
            }
            case ')': {
                result.append("\\)");
                break;
            }
            case '^': {
                result.append("\\^");
                break;
            }
            case '$': {
                result.append("\\$");
                break;
            }
            default: {
                result.append(character);
            }
            }
            character = iterator.next();
        }
        return result.toString();
    }

    @Reference private FileManager fileManager;

    @Reference private ProjectOperations projectOperations;

    public CloudDeployableFile convertFromText(String value,
            final Class<?> requiredType, final String optionContext) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        if (CREATE_OPTION.equals(value)) {
            if (projectOperations instanceof MavenOperationsImpl) {
                try {
                    if (projectOperations.getPathResolver() == null) {
                        return null;
                    }
                    ((MavenOperationsImpl) projectOperations)
                            .executeMvnCommand("clean package");
                    final String rootPath = projectOperations
                            .getFocusedModule().getRoot();
                    final Set<FileDetails> fileDetails = fileManager
                            .findMatchingAntPath(rootPath + File.separator
                                    + "**" + File.separator + "*.war");
                    if (fileDetails.size() > 0) {
                        final FileDetails fileToDeploy = fileDetails.iterator()
                                .next();
                        return new CloudDeployableFile(fileToDeploy);
                    }
                }
                catch (final IOException e) {
                    throw new IllegalStateException(e);
                }
            }
            return null;
        }
        final String oppositeFileSeparator = getOppositeFileSeparator();
        if (value.contains(oppositeFileSeparator)) {
            value = value.replaceAll(escapeString(oppositeFileSeparator),
                    escapeString(File.separator));
        }
        final String path = projectOperations.getFocusedModule().getRoot()
                + value;
        if (!new File(path).exists()) {
            LOGGER.severe("The file at path '" + path
                    + "' doesn't exist; cannot continue");
            return null;
        }
        final FileDetails fileToDeploy = fileManager.readFile(projectOperations
                .getFocusedModule().getRoot() + value);
        return new CloudDeployableFile(fileToDeploy);
    }

    public boolean getAllPossibleValues(final List<Completion> completions,
            final Class<?> requiredType, final String existingData,
            final String optionContext, final MethodTarget target) {
        if (projectOperations.getPathResolver() == null) {
            LOGGER.warning("A project has not been created please specify the full path of the file you wish to deploy");
            return false;
        }
        final String rootPath = projectOperations.getFocusedModule().getRoot();
        final Set<FileDetails> fileDetails = fileManager
                .findMatchingAntPath(rootPath + File.separator + "**"
                        + File.separator + "*.war");

        if (fileDetails.isEmpty()) {
            LOGGER.warning("No deployable files found in the project directory. Please use the '"
                    + CREATE_OPTION + "' option to build the war.");
            completions.add(new Completion(CREATE_OPTION));
        }
        for (final FileDetails fileDetail : fileDetails) {
            completions.add(new Completion(fileDetail.getCanonicalPath()
                    .replaceAll(escapeString(rootPath), "")));
        }

        return false;
    }

    private String getOppositeFileSeparator() {
        final Character fileSeparator = File.separatorChar;
        if (fileSeparator.equals('/')) {
            return "\\";
        }
        else if (fileSeparator.equals('\\')) {
            return "/";
        }
        throw new IllegalStateException(
                "Unexpected file separator character encountered; cannot continue");
    }

    public boolean supports(final Class<?> requiredType,
            final String optionContext) {
        return CloudDeployableFile.class.isAssignableFrom(requiredType);
    }
}
