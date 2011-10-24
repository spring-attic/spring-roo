package org.springframework.roo.classpath.converters;

import static org.springframework.roo.project.ContextualPath.MODULE_PATH_SEPARATOR;

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
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.shell.Completion;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;
import org.springframework.roo.support.util.AnsiEscapeCode;
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
	@Reference private TypeLocationService typeLocationService;

	public JavaType convertFromText(String value, final Class<?> requiredType, final String optionContext) {
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

		String topLevelPath;

		Pom module = projectOperations.getFocusedModule();

		if (value.contains(MODULE_PATH_SEPARATOR)) {
			String moduleName = value.substring(0, value.indexOf(MODULE_PATH_SEPARATOR));
			module = projectOperations.getPomManagementService().getPomFromModuleName(moduleName);
			topLevelPath = typeLocationService.getTopLevelPackageForModule(module);
			value = value.substring(value.indexOf(MODULE_PATH_SEPARATOR) + 1, value.length()).trim();
			projectOperations.getPomManagementService().setFocusedModule(module);
		} else {
			topLevelPath = typeLocationService.getTopLevelPackageForModule(projectOperations.getFocusedModule());
		}

		if (value.equals(topLevelPath)) {
			return null;
		}

		String newValue = locateExisting(value, topLevelPath);
		if (newValue == null) {
			newValue = locateNew(value, topLevelPath);
		}

		if (StringUtils.hasText(newValue)) {
			String physicalTypeIdentifier = typeLocationService.getPhysicalTypeIdentifier(new JavaType(newValue));
			if (StringUtils.hasText(physicalTypeIdentifier)) {
				module = projectOperations.getPomManagementService().getPomFromModuleName(PhysicalTypeIdentifier.getPath(physicalTypeIdentifier).getModule());
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
			lastUsed.setType(result, module);
		}
		return result;
	}

	private String locateNew(final String value, final String topLevelPath) {
		String newValue = value;
		if (value.startsWith("~")) {
			if (value.length() > 1) {
				newValue = (value.charAt(1) == '.' ? topLevelPath : topLevelPath + ".") + value.substring(1);
			} else {
				newValue = topLevelPath + ".";
			}
		}

		lastUsed.setTopLevelPackage(new JavaPackage(topLevelPath));

		return newValue;
	}

	private String locateExisting(final String value, String topLevelPath) {
		String newValue = value;
		if (value.startsWith("~")) {
			boolean found = false;
			while (!found) {
				if (value.length() > 1) {
					newValue = (value.charAt(1) == '.' ? topLevelPath : topLevelPath + ".") + value.substring(1);
				} else {
					newValue = topLevelPath + ".";
				}
				String physicalTypeIdentifier = typeLocationService.getPhysicalTypeIdentifier(new JavaType(newValue));
				if (physicalTypeIdentifier != null) {
					topLevelPath = typeLocationService.getTopLevelPackageForModule(projectOperations.getPomFromModuleName(PhysicalTypeIdentifier.getPath(physicalTypeIdentifier).getModule()));
					found = true;
				} else {
					int index = topLevelPath.lastIndexOf('.');
					if (index == -1) {
						break;
					}
					topLevelPath = topLevelPath.substring(0, topLevelPath.lastIndexOf('.'));
				}
			}
			if (found) {
				lastUsed.setTopLevelPackage(new JavaPackage(topLevelPath));
			} else {
				return null;
			}
		}
		return newValue;
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

	private void completeProjectSpecificPaths(final List<Completion> completions, String existingData) {
		String topLevelPath = "";

		if (!projectOperations.isFocusedProjectAvailable()) {
			return;
		}

		topLevelPath = typeLocationService.getTopLevelPackageForModule(projectOperations.getFocusedModule());

		Pom focusedModule = projectOperations.getFocusedModule();
		String focusedModulePath = projectOperations.getFocusedModule().getPath();
		String focusedModuleName = focusedModule.getModuleName();
		boolean intraModule = false;
		if (existingData.contains(MODULE_PATH_SEPARATOR)) {
			focusedModuleName = existingData.substring(0, existingData.indexOf(MODULE_PATH_SEPARATOR));
			focusedModule = projectOperations.getPomFromModuleName(focusedModuleName);
			focusedModulePath = focusedModule.getPath();
			existingData = existingData.substring(existingData.indexOf(MODULE_PATH_SEPARATOR) + 1, existingData.length());
			topLevelPath = typeLocationService.getTopLevelPackageForModule(focusedModule);
			intraModule = true;
		}

		String newValue = existingData;
		if (existingData.startsWith("~")) {
			if (existingData.length() > 1) {
				newValue = (existingData.charAt(1) == '.' ? topLevelPath : topLevelPath + ".") + existingData.substring(1);
			} else {
				newValue = topLevelPath + ".";
			}
		}

		String prefix = "";
		String formattedPrefix = "";

		if (!focusedModulePath.equals(projectOperations.getFocusedModule().getPath())) {
			prefix = focusedModuleName + MODULE_PATH_SEPARATOR;
			formattedPrefix = AnsiEscapeCode.decorate(focusedModuleName + MODULE_PATH_SEPARATOR, AnsiEscapeCode.FG_CYAN);
		}

		for (String moduleName : projectOperations.getPomManagementService().getModuleNames()) {
			if (!moduleName.equals(focusedModuleName)) {
				Completion completion = new Completion(moduleName + MODULE_PATH_SEPARATOR, AnsiEscapeCode.decorate(moduleName + MODULE_PATH_SEPARATOR, AnsiEscapeCode.FG_CYAN), "Modules", 0);
				completions.add(completion);
			}
		}

		String heading = "";
		if (!intraModule) {
			heading = focusedModuleName;
		}
		if (typeLocationService.getTypesForModule(focusedModulePath).isEmpty()) {
			completions.add(new Completion(prefix + focusedModule.getGroupId(), formattedPrefix + focusedModule.getGroupId(), heading, 1));
			return;
		}  else {
			completions.add(new Completion(prefix + topLevelPath, formattedPrefix + topLevelPath, heading, 1));
		}

		for (String type : typeLocationService.getTypesForModule(focusedModulePath)) {
			if (type.startsWith(newValue)) {
				type = type.replaceFirst(topLevelPath, "~");
				completions.add(new Completion(prefix + type, formattedPrefix + type, heading, 1));
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
