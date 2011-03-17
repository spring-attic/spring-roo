package org.springframework.roo.classpath.converters;

import java.io.File;
import java.util.List;
import java.util.SortedSet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

/**
 * Provides conversion to and from {@link JavaPackage}, with full support for using "~" as denoting the user's top-level package.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component
@Service
public class JavaPackageConverter implements Converter {
	@Reference private LastUsed lastUsed;
	@Reference private MetadataService metadataService;
	@Reference private FileManager fileManager;
	
	public Object convertFromText(String value, Class<?> requiredType, String optionContext) {
		if (value == null || "".equals(value)) {
			return null;
		}
		String newValue = value.toLowerCase();
		if (value.startsWith("~")) {
			try {
				String topLevelPath = "";
				ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
				if (projectMetadata != null) {
					topLevelPath = projectMetadata.getTopLevelPackage().getFullyQualifiedPackageName();
				}
				if (value.length() > 1) {
					newValue = (!(value.charAt(1) == '.') ? topLevelPath + "." : topLevelPath) + value.substring(1);
				} else {
					newValue = topLevelPath;
				}
			} catch (RuntimeException ignored) {}
		}
		if (newValue.endsWith(".")) {
			newValue = newValue.substring(0, newValue.length() - 1);
		}
		JavaPackage result = new JavaPackage(newValue);
		if (optionContext.contains("update")) {
			lastUsed.setPackage(result);
		}
		return result;
	}

	public boolean supports(Class<?> requiredType, String optionContext) {
		return JavaPackage.class.isAssignableFrom(requiredType);
	}

	public boolean getAllPossibleValues(List<String> completions, Class<?> requiredType, String existingData, String optionContext, MethodTarget target) {
		if (existingData == null) {
			existingData = "";
		}

		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		if (projectMetadata == null) {
			return false;
		}

		String topLevelPath = projectMetadata.getTopLevelPackage().getFullyQualifiedPackageName();

		String newValue = existingData;
		if (existingData.startsWith("~")) {
			if (existingData.length() > 1) {
				newValue = (existingData.charAt(1) == '.' ? topLevelPath : topLevelPath + ".") + existingData.substring(1);
			} else {
				newValue = topLevelPath + File.separator;
			}
		}

		PathResolver pathResolver = projectMetadata.getPathResolver();
		String antPath = pathResolver.getRoot(Path.SRC_MAIN_JAVA) + File.separatorChar + newValue.replace(".", File.separator).toLowerCase() + "*";
		SortedSet<FileDetails> entries = fileManager.findMatchingAntPath(antPath);

		for (FileDetails fileIdentifier : entries) {
			String candidate = pathResolver.getRelativeSegment(fileIdentifier.getCanonicalPath()).substring(1); // Drop the leading "/"
			boolean include = false;
			// Do not include directories that start with ., as this is used for purposes like SVN (see ROO-125)
			if (fileIdentifier.getFile().isDirectory() && !fileIdentifier.getFile().getName().startsWith(".")) {
				include = true;
			}

			if (include) {
				// Convert this path back into something the user would type
				if (existingData.startsWith("~")) {
					if (existingData.length() > 1) {
						candidate = (existingData.charAt(1) == '.' ? "~." : "~") + candidate.substring(topLevelPath.length() + 1);
					} else {
						candidate = "~" + candidate.substring(topLevelPath.length() + 1);
					}
				}
				candidate = candidate.replace(File.separator, ".");
				completions.add(candidate);
			}
		}

		return false;
	}
}