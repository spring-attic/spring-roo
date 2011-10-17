package org.springframework.roo.classpath.converters;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.shell.Completion;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;
import org.springframework.roo.support.util.StringUtils;

/**
 * Provides conversion to and from {@link JavaType}, with full support for using "~" as denoting the user's top-level package.
 *
 * @author Ben Alex
 * @since 1.0
 */
@Component
@Service
public class JavaTypeConverter implements Converter<JavaType> {

	private static final List<String> NUMBER_PRIMITIVES = Arrays.asList("byte", "short", "int", "long", "float", "double");

	// Fields
	@Reference protected LastUsed lastUsed;
	@Reference protected FileManager fileManager;
	@Reference protected ProjectOperations projectOperations;

	public JavaType convertFromText(final String value, final Class<?> requiredType, final String optionContext) {
		if (value == null || "".equals(value)) {
			return null;
		}

		// Check for number primitives
		if (NUMBER_PRIMITIVES.contains(value)) {
			return getNumberPrimitiveType(value);
		}

		if ("*".equals(value)) {
			JavaType result = lastUsed.getJavaType();
			if (result == null) {
				throw new IllegalStateException("Unknown type; please indicate the type as a command option (ie --xxxx)");
			}
			return result;
		}

		String topLevelPath = null;
		ProjectMetadata projectMetadata = projectOperations.getProjectMetadata();
		if (projectMetadata != null) {
			JavaPackage topLevelPackage = projectMetadata.getTopLevelPackage();
			topLevelPath = topLevelPackage.getFullyQualifiedPackageName();
			lastUsed.setTopLevelPackage(topLevelPackage);
		}

		String newValue = value;
		if (value.startsWith("~") && topLevelPath != null) {
			if (value.length() > 1) {
				newValue = (!(value.charAt(1) == '.') ? topLevelPath + "." : topLevelPath) + value.substring(1);
			} else {
				newValue = topLevelPath;
			}
		}

		// If the user did not provide a java type name containing a dot, it's taken as relative to the current package directory
		if (!newValue.contains(".")) {
			newValue = (lastUsed.getJavaPackage() == null ? lastUsed.getTopLevelPackage().getFullyQualifiedPackageName() : lastUsed.getJavaPackage().getFullyQualifiedPackageName()) + "." + newValue;
		}

		// Automatically capitalize the first letter of the last name segment (ie capitalize the type name, but not the package)
		int index = newValue.lastIndexOf(".");
		if (index > -1 && !newValue.endsWith(".")) {
			String typeName = newValue.substring(index + 1);
			typeName = StringUtils.capitalize(typeName);
			newValue = newValue.substring(0, index).toLowerCase() + "." + typeName;
		}
		JavaType result = new JavaType(newValue);
		if (optionContext.contains("update")) {
			lastUsed.setType(result);
		}
		return result;
	}

	public boolean supports(final Class<?> requiredType, final String optionContext) {
		return JavaType.class.isAssignableFrom(requiredType);
	}

	public boolean getAllPossibleValues(final List<Completion> completions, final Class<?> requiredType, String existingData, final String optionContext, final MethodTarget target) {
		if (existingData == null) {
			existingData = "";
		}

		if (optionContext == null || "".equals(optionContext) || optionContext.contains("project")) {
			completeProjectSpecificPaths(completions, existingData);
		}

		if (optionContext != null && optionContext.contains("java")) {
			completeJavaSpecificPaths(completions, existingData, optionContext);
		}

		return false;
	}

	/**
	 * Adds common "java." types to the completions. For now we just provide them statically.
	 */
	private void completeJavaSpecificPaths(final List<Completion> completions, final String existingData, String optionContext) {
		SortedSet<String> types = new TreeSet<String>();

		if (optionContext == null || "".equals(optionContext)) {
			optionContext = "java-all";
		}

		if (optionContext.contains("java-all") || optionContext.contains("java-lang")) {
			// lang - other
			types.add(Boolean.class.getName());
			types.add(String.class.getName());
		}

		if (optionContext.contains("java-all") || optionContext.contains("java-lang") || optionContext.contains("java-number")) {
			// lang - numeric
			types.add(Number.class.getName());
			types.add(Short.class.getName());
			types.add(Byte.class.getName());
			types.add(Integer.class.getName());
			types.add(Long.class.getName());
			types.add(Float.class.getName());
			types.add(Double.class.getName());
			types.add(Byte.TYPE.getName());
			types.add(Short.TYPE.getName());
			types.add(Integer.TYPE.getName());
			types.add(Long.TYPE.getName());
			types.add(Float.TYPE.getName());
			types.add(Double.TYPE.getName());
		}

		if (optionContext.contains("java-all") || optionContext.contains("java-number")) {
			// misc
			types.add(BigDecimal.class.getName());
			types.add(BigInteger.class.getName());
		}

		if (optionContext.contains("java-all") || optionContext.contains("java-util") || optionContext.contains("java-collections")) {
			// util
			types.add(Collection.class.getName());
			types.add(List.class.getName());
			types.add(Queue.class.getName());
			types.add(Set.class.getName());
			types.add(SortedSet.class.getName());
			types.add(Map.class.getName());
		}

		if (optionContext.contains("java-all") || optionContext.contains("java-util") || optionContext.contains("java-date")) {
			// util
			types.add(Date.class.getName());
			types.add(Calendar.class.getName());
		}

		for (String type : types) {
			if (type.startsWith(existingData) || existingData.startsWith(type)) {
				completions.add(new Completion(type));
			}
		}
	}

	private void completeProjectSpecificPaths(final List<String> completions, final String existingData) {
		String topLevelPath = "";
		ProjectMetadata projectMetadata = projectOperations.getProjectMetadata();

		if (projectMetadata == null) {
			return;
		}

		topLevelPath = projectMetadata.getTopLevelPackage().getFullyQualifiedPackageName();

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
		String antPath = projectOperations.getPathResolver().getRoot(Path.SRC_MAIN_JAVA) + File.separatorChar + newValue.replace(".", File.separator) + "*";
		SortedSet<FileDetails> entries = fileManager.findMatchingAntPath(antPath);

		// Pass 2: Add a separator to the end of the value to pick up sub-folders
		antPath = pathResolver.getRoot(Path.SRC_MAIN_JAVA) + File.separatorChar + newValue.replace(".", File.separator) + File.separator + "*";
		entries.addAll(fileManager.findMatchingAntPath(antPath));

		for (FileDetails fileIdentifier : entries) {
			String candidate = pathResolver.getRelativeSegment(fileIdentifier.getCanonicalPath()).substring(1); // drop the leading "/"
			boolean include = false;
			boolean directory = false;
			if (fileIdentifier.getFile().isDirectory()) {
				// Do not include directories that start with ., as this is used for purposes like SVN (see ROO-125)
				if (!fileIdentifier.getFile().getName().startsWith(".")) {
					include = true;
					directory = true;
				}
			} else {
				// A file
				if (candidate.endsWith(".java")) {
					candidate = candidate.substring(0, candidate.length() - 5); // Drop .java
					include = true;
				}
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
				if (directory) {
					candidate = candidate + ".";
				}
				completions.add(new Completion(candidate));
			}
		}
	}

	private JavaType getNumberPrimitiveType(final String value) {
		if ("byte".equals(value)) {
			return JavaType.BYTE_PRIMITIVE;
		} else if ("short".equals(value)) {
			return JavaType.SHORT_PRIMITIVE;
		} else if ("int".equals(value)) {
			return JavaType.INT_PRIMITIVE;
		} else if ("long".equals(value)) {
			return JavaType.LONG_PRIMITIVE;
		} else if ("float".equals(value)) {
			return JavaType.FLOAT_PRIMITIVE;
		} else if ("double".equals(value)) {
			return JavaType.DOUBLE_PRIMITIVE;
		} else {
			return null;
		}
	}
}
