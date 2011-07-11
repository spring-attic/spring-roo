package org.springframework.roo.addon.cloud.foundry.converter;

import java.io.File;
import java.io.IOException;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.cloud.foundry.model.CloudDeployableFile;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.MavenOperationsImpl;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

/**
 * Provides conversion to and from Cloud Foundry model classes.
 *
 * @author James Tyrrell
 * @author Andrew Swan
 * @since 1.2.0
 */
@Component
@Service
public class CloudDeployableFileConverter implements Converter<CloudDeployableFile> {
	private static final Logger logger = Logger.getLogger(CloudDeployableFileConverter.class.getName());
	private static final String CREATE_OPTION = "CREATE";
	@Reference private FileManager fileManager;
	@Reference private ProjectOperations projectOperations;

	public CloudDeployableFile convertFromText(String value, Class<?> requiredType, String optionContext) {
		if (value == null || "".equals(value)) {
			return null;
		}
		if (CREATE_OPTION.equals(value)) {
			if (projectOperations instanceof MavenOperationsImpl) {
				try {
					if (projectOperations.getPathResolver() == null) {
						return null;
					}
					((MavenOperationsImpl) projectOperations).executeMvnCommand("clean package");
					String rootPath = projectOperations.getPathResolver().getRoot(Path.ROOT);
					Set<FileDetails> fileDetails = fileManager.findMatchingAntPath(rootPath + File.separator + "**" + File.separator + "*.war");
					if (fileDetails.size() > 0) {
						FileDetails fileToDeploy = fileDetails.iterator().next();
						return new CloudDeployableFile(fileToDeploy);
					}
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}
			}
			return null;
		}
		String oppositeFileSeparator = getOppositeFileSeparator();
		if (value.contains(oppositeFileSeparator)) {
			value = value.replaceAll(escapeString(oppositeFileSeparator), escapeString(File.separator));
		}
		String path = projectOperations.getPathResolver().getRoot(Path.ROOT) + value;
		if (!new File(path).exists())  {
			logger.severe("The file at path '" + path + "' doesn't exist; cannot continue");
			return null;
		}
		FileDetails fileToDeploy = fileManager.readFile(projectOperations.getPathResolver().getRoot(Path.ROOT) + value);
		return new CloudDeployableFile(fileToDeploy);
	}

	public boolean supports(Class<?> requiredType, String optionContext) {
		return CloudDeployableFile.class.isAssignableFrom(requiredType);
	}

	public boolean getAllPossibleValues(List<String> completions, Class<?> requiredType, String existingData, String optionContext, MethodTarget target) {
		if (projectOperations.getPathResolver() == null) {
			logger.warning("A project has not been created please specify the full path of the file you wish to deploy");
			return false;
		}
		String rootPath = projectOperations.getPathResolver().getRoot(Path.ROOT);
		Set<FileDetails> fileDetails = fileManager.findMatchingAntPath(rootPath + File.separator + "**" + File.separator + "*.war");

		if (fileDetails.isEmpty()) {
			logger.warning("No deployable files found in the project directory. Please use the '" + CREATE_OPTION + "' option to build the war.");
			completions.add(CREATE_OPTION);
		}
		for (FileDetails fileDetail : fileDetails) {
			completions.add(fileDetail.getCanonicalPath().replaceAll(escapeString(rootPath), ""));
		}

		return false;
	}

	private String getOppositeFileSeparator() {
		Character fileSeparator = File.separatorChar;
		if (fileSeparator.equals('/')) {
			return "\\";
		} else if (fileSeparator.equals('\\')) {
			return "/";
		}
		throw new IllegalStateException("Unexpected file separator character encountered; cannot continue");
	}

	public static String escapeString(String toEscape) {
		final StringBuilder result = new StringBuilder();
		final StringCharacterIterator iterator = new StringCharacterIterator(toEscape);
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
}
