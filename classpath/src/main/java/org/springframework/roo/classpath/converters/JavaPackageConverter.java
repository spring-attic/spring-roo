package org.springframework.roo.classpath.converters;

import java.io.File;
import java.util.List;
import java.util.SortedSet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.shell.Completion;
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
public class JavaPackageConverter implements Converter<JavaPackage> {

	// Fields
	@Reference private FileManager fileManager;
	@Reference private LastUsed lastUsed;
	@Reference private ProjectOperations projectOperations;
	@Reference private TypeLocationService typeLocationService;

	public JavaPackage convertFromText(final String value, final Class<?> requiredType, final String optionContext) {
		if (value == null || "".equals(value)) {
			return null;
		}
		String newValue = value.toLowerCase();
		if (value.startsWith("~")) {
			try {
				String topLevelPath = "";
				if (projectOperations.isFocusedProjectAvailable()) {
					topLevelPath = typeLocationService.getTopLevelPackageForModule(projectOperations.getFocusedModule());//projectOperations.getTopLevelPackage().getFullyQualifiedPackageName();
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

	public boolean supports(final Class<?> requiredType, final String optionContext) {
		return JavaPackage.class.isAssignableFrom(requiredType);
	}

	public boolean getAllPossibleValues(final List<Completion> completions, final Class<?> requiredType, String existingData, final String optionContext, final MethodTarget target) {
		if (existingData == null) {
			existingData = "";
		}
		String topLevelPath = "";
		if (!projectOperations.isFocusedProjectAvailable()) {
			return false;
		}

		for (Pom pom : projectOperations.getPomManagementService().getPomMap().values()) {
			for (String type : typeLocationService.getTypesForModule(pom.getPath())) {
				completions.add(new Completion(type.substring(0, type.lastIndexOf('.'))));
			}
		}

		if (true) {
			return false;
		}

		topLevelPath = typeLocationService.getTopLevelPackageForModule(projectOperations.getFocusedModule());

		String newValue = existingData;
		if (existingData.startsWith("~")) {
			if (existingData.length() > 1) {
				newValue = (existingData.charAt(1) == '.' ? topLevelPath : topLevelPath + ".") + existingData.substring(1);
			} else {
				newValue = topLevelPath + File.separator;
			}
		}

		PathResolver pathResolver = projectOperations.getPathResolver();

		// Pass 1: If a '.' suffixes the value then sub-folders will be picked up explicitly
		String antPath = pathResolver.getRoot() + File.separatorChar + newValue.replace(".", File.separator).toLowerCase() + "*";
		SortedSet<FileDetails> entries = fileManager.findMatchingAntPath(antPath);

		// Pass 2: Add a separator to the end of the value to pick up sub-folders
		antPath = pathResolver.getRoot() + File.separatorChar + newValue.replace(".", File.separator).toLowerCase() + File.separator + "*";
		entries.addAll(fileManager.findMatchingAntPath(antPath));

		for (FileDetails fileIdentifier : entries) {
			String candidate = pathResolver.getRelativeSegment(fileIdentifier.getCanonicalPath());
			if (candidate.length() > 0) {
				// Drop the leading "/"
				candidate = candidate.substring(1);
			}

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
				completions.add(new Completion(candidate));
			}
		}

		return false;
	}
}