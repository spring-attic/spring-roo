package org.springframework.roo.classpath.converters;

import static org.springframework.roo.classpath.converters.JavaPackageConverter.TOP_LEVEL_PACKAGE_SYMBOL;
import static org.springframework.roo.project.LogicalPath.MODULE_PATH_SEPARATOR;
import static org.springframework.roo.shell.OptionContexts.INTERFACE;
import static org.springframework.roo.shell.OptionContexts.PROJECT;
import static org.springframework.roo.shell.OptionContexts.SUPERCLASS;
import static org.springframework.roo.shell.OptionContexts.UPDATE;
import static org.springframework.roo.support.util.AnsiEscapeCode.FG_CYAN;
import static org.springframework.roo.support.util.AnsiEscapeCode.decorate;

import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
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

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.shell.Completion;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

/**
 * Provides conversion to and from {@link JavaType}, with full support for using
 * {@value JavaPackageConverter#TOP_LEVEL_PACKAGE_SYMBOL} as denoting the user's
 * top-level package.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component
@Service
public class JavaTypeConverter implements Converter<JavaType> {

    /**
     * The value that converts to the most recently used {@link JavaType}.
     */
    static final String LAST_USED_INDICATOR = "*";

    private static final List<String> NUMBER_PRIMITIVES = Arrays.asList("byte",
            "short", "int", "long", "float", "double");

    @Reference FileManager fileManager;
    @Reference LastUsed lastUsed;
    @Reference ProjectOperations projectOperations;
    @Reference TypeLocationService typeLocationService;

    public JavaType convertFromText(String value, final Class<?> requiredType,
            final String optionContext) {
        if (StringUtils.isBlank(value)) {
            return null;
        }

        // Check for number primitives
        if (NUMBER_PRIMITIVES.contains(value)) {
            return getNumberPrimitiveType(value);
        }

        if (LAST_USED_INDICATOR.equals(value)) {
            final JavaType result = lastUsed.getJavaType();
            if (result == null) {
                throw new IllegalStateException(
                        "Unknown type; please indicate the type as a command option (ie --xxxx)");
            }
            return result;
        }

        String topLevelPath;
        Pom module = projectOperations.getFocusedModule();

        if (value.contains(MODULE_PATH_SEPARATOR)) {
            final String moduleName = value.substring(0,
                    value.indexOf(MODULE_PATH_SEPARATOR));
            module = projectOperations.getPomFromModuleName(moduleName);
            topLevelPath = typeLocationService
                    .getTopLevelPackageForModule(module);
            value = value.substring(value.indexOf(MODULE_PATH_SEPARATOR) + 1,
                    value.length()).trim();
            if (StringUtils.contains(optionContext, UPDATE)) {
                projectOperations.setModule(module);
            }
        }
        else {
            topLevelPath = typeLocationService
                    .getTopLevelPackageForModule(projectOperations
                            .getFocusedModule());
        }

        if (value.equals(topLevelPath)) {
            return null;
        }

        String newValue = locateExisting(value, topLevelPath);
        if (newValue == null) {
            newValue = locateNew(value, topLevelPath);
        }

        if (StringUtils.isNotBlank(newValue)) {
            final String physicalTypeIdentifier = typeLocationService
                    .getPhysicalTypeIdentifier(new JavaType(newValue));
            if (StringUtils.isNotBlank(physicalTypeIdentifier)) {
                module = projectOperations
                        .getPomFromModuleName(PhysicalTypeIdentifier.getPath(
                                physicalTypeIdentifier).getModule());
            }
        }

        // If the user did not provide a java type name containing a dot, it's
        // taken as relative to the current package directory
        if (!newValue.contains(".")) {
            newValue = (lastUsed.getJavaPackage() == null ? lastUsed
                    .getTopLevelPackage().getFullyQualifiedPackageName()
                    : lastUsed.getJavaPackage().getFullyQualifiedPackageName())
                    + "." + newValue;
        }

        // Automatically capitalise the first letter of the last name segment
        // (i.e. capitalise the type name, but not the package)
        final int index = newValue.lastIndexOf(".");
        if (index > -1 && !newValue.endsWith(".")) {
            String typeName = newValue.substring(index + 1);
            typeName = StringUtils.capitalize(typeName);
            newValue = newValue.substring(0, index).toLowerCase() + "."
                    + typeName;
        }
        final JavaType result = new JavaType(newValue);
        if (StringUtils.contains(optionContext, UPDATE)) {
            lastUsed.setType(result, module);
        }
        return result;
    }

    public boolean getAllPossibleValues(final List<Completion> completions,
            final Class<?> requiredType, String existingData,
            final String optionContext, final MethodTarget target) {
        existingData = StringUtils.stripToEmpty(existingData);

        if (StringUtils.isBlank(optionContext)
                || optionContext.contains(PROJECT)
                || optionContext.contains(SUPERCLASS)
                || optionContext.contains(INTERFACE)) {
            completeProjectSpecificPaths(completions, existingData,
                    optionContext);
        }

        if (StringUtils.contains(optionContext, "java")) {
            completeJavaSpecificPaths(completions, existingData, optionContext);
        }

        return false;
    }

    public boolean supports(final Class<?> requiredType,
            final String optionContext) {
        return JavaType.class.isAssignableFrom(requiredType);
    }

    private void addCompletionsForOtherModuleNames(
            final List<Completion> completions, final Pom targetModule) {
        for (final String moduleName : projectOperations.getModuleNames()) {
            if (StringUtils.isNotBlank(moduleName)
                    && !moduleName.equals(targetModule.getModuleName())) {
                completions.add(new Completion(moduleName
                        + MODULE_PATH_SEPARATOR, decorate(moduleName
                        + MODULE_PATH_SEPARATOR, FG_CYAN), "Modules", 0));
            }
        }
    }

    private void addCompletionsForTypesInTargetModule(
            final List<Completion> completions, final String optionContext,
            final Pom targetModule, final String heading, final String prefix,
            final String formattedPrefix, final String topLevelPackage,
            final String basePackage) {
        final Collection<JavaType> typesInModule = getTypesForModule(
                optionContext, targetModule);
        if (typesInModule.isEmpty()) {
            completions.add(new Completion(prefix + targetModule.getGroupId(),
                    formattedPrefix + targetModule.getGroupId(), heading, 1));
        }
        else {
            completions.add(new Completion(prefix + topLevelPackage,
                    formattedPrefix + topLevelPackage, heading, 1));
            for (final JavaType javaType : typesInModule) {
                String type = javaType.getFullyQualifiedTypeName();
                if (type.startsWith(basePackage)) {
                    type = StringUtils.replace(type, topLevelPackage,
                            TOP_LEVEL_PACKAGE_SYMBOL, 1);
                    completions.add(new Completion(prefix + type,
                            formattedPrefix + type, heading, 1));
                }
            }
        }
    }

    private Collection<JavaType> getTypesForModule(
            final String optionContext, final Pom targetModule) {
        final Collection<JavaType> typesForModule = typeLocationService
                .getTypesForModule(targetModule);
        if (!(optionContext.contains(SUPERCLASS) || optionContext
                .contains(INTERFACE))) {
            return typesForModule;
        }

        final Collection<JavaType> types = new ArrayList<JavaType>();
        for (final JavaType javaType : typesForModule) {
            final ClassOrInterfaceTypeDetails typeDetails = typeLocationService
                    .getTypeDetails(javaType);
            if ((optionContext.contains(SUPERCLASS) && (Modifier
                    .isFinal(typeDetails.getModifier()) || typeDetails
                    .getPhysicalTypeCategory() == PhysicalTypeCategory.INTERFACE))
                    || (optionContext.contains(INTERFACE) && typeDetails
                            .getPhysicalTypeCategory() != PhysicalTypeCategory.INTERFACE)) {
                continue;
            }
            types.add(javaType);
        }
        return types;
    }

    /**
     * Adds common "java." types to the completions. For now we just provide
     * them statically.
     */
    private void completeJavaSpecificPaths(final List<Completion> completions,
            final String existingData, String optionContext) {
        final SortedSet<String> types = new TreeSet<String>();

        if (StringUtils.isBlank(optionContext)) {
            optionContext = "java-all";
        }

        if (optionContext.contains("java-all")
                || optionContext.contains("java-lang")) {
            // lang - other
            types.add(Boolean.class.getName());
            types.add(String.class.getName());
        }

        if (optionContext.contains("java-all")
                || optionContext.contains("java-lang")
                || optionContext.contains("java-number")) {
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

        if (optionContext.contains("java-all")
                || optionContext.contains("java-number")) {
            // misc
            types.add(BigDecimal.class.getName());
            types.add(BigInteger.class.getName());
        }

        if (optionContext.contains("java-all")
                || optionContext.contains("java-util")
                || optionContext.contains("java-collections")) {
            // util
            types.add(Collection.class.getName());
            types.add(List.class.getName());
            types.add(Queue.class.getName());
            types.add(Set.class.getName());
            types.add(SortedSet.class.getName());
            types.add(Map.class.getName());
        }

        if (optionContext.contains("java-all")
                || optionContext.contains("java-util")
                || optionContext.contains("java-date")) {
            // util
            types.add(Date.class.getName());
            types.add(Calendar.class.getName());
        }

        for (final String type : types) {
            if (type.startsWith(existingData) || existingData.startsWith(type)) {
                completions.add(new Completion(type));
            }
        }
    }

    private void completeProjectSpecificPaths(
            final List<Completion> completions, final String existingData,
            final String optionContext) {
        if (!projectOperations.isFocusedProjectAvailable()) {
            return;
        }
        final Pom targetModule;
        final String heading;
        final String prefix;
        final String formattedPrefix;
        final String typeName;
        if (existingData.contains(MODULE_PATH_SEPARATOR)) {
            // Looking for a type in another module
            final String targetModuleName = existingData.substring(0,
                    existingData.indexOf(MODULE_PATH_SEPARATOR));
            targetModule = projectOperations
                    .getPomFromModuleName(targetModuleName);
            heading = "";
            prefix = targetModuleName + MODULE_PATH_SEPARATOR;
            formattedPrefix = decorate(
                    targetModuleName + MODULE_PATH_SEPARATOR, FG_CYAN);
            typeName = StringUtils.substringAfterLast(existingData,
                    MODULE_PATH_SEPARATOR);
        }
        else {
            // Looking for a type in the currently focused module
            targetModule = projectOperations.getFocusedModule();
            heading = targetModule.getModuleName();
            prefix = "";
            formattedPrefix = "";
            typeName = existingData;
        }
        final String topLevelPackage = typeLocationService
                .getTopLevelPackageForModule(targetModule);
        final String basePackage = resolveTopLevelPackageSymbol(typeName,
                topLevelPackage);

        addCompletionsForOtherModuleNames(completions, targetModule);

        if (!"pom".equals(targetModule.getPackaging())) {
            addCompletionsForTypesInTargetModule(completions, optionContext,
                    targetModule, heading, prefix, formattedPrefix,
                    topLevelPackage, basePackage);
        }
    }

    private JavaType getNumberPrimitiveType(final String value) {
        if ("byte".equals(value)) {
            return JavaType.BYTE_PRIMITIVE;
        }
        else if ("short".equals(value)) {
            return JavaType.SHORT_PRIMITIVE;
        }
        else if ("int".equals(value)) {
            return JavaType.INT_PRIMITIVE;
        }
        else if ("long".equals(value)) {
            return JavaType.LONG_PRIMITIVE;
        }
        else if ("float".equals(value)) {
            return JavaType.FLOAT_PRIMITIVE;
        }
        else if ("double".equals(value)) {
            return JavaType.DOUBLE_PRIMITIVE;
        }
        else {
            return null;
        }
    }

    private String locateExisting(final String value, String topLevelPath) {
        String newValue = value;
        if (value.startsWith(TOP_LEVEL_PACKAGE_SYMBOL)) {
            boolean found = false;
            while (!found) {
                if (value.length() > 1) {
                    newValue = (value.charAt(1) == '.' ? topLevelPath
                            : topLevelPath + ".") + value.substring(1);
                }
                else {
                    newValue = topLevelPath + ".";
                }
                final String physicalTypeIdentifier = typeLocationService
                        .getPhysicalTypeIdentifier(new JavaType(newValue));
                if (physicalTypeIdentifier != null) {
                    topLevelPath = typeLocationService
                            .getTopLevelPackageForModule(projectOperations
                                    .getPomFromModuleName(PhysicalTypeIdentifier
                                            .getPath(physicalTypeIdentifier)
                                            .getModule()));
                    found = true;
                }
                else {
                    final int index = topLevelPath.lastIndexOf('.');
                    if (index == -1) {
                        break;
                    }
                    topLevelPath = topLevelPath.substring(0,
                            topLevelPath.lastIndexOf('.'));
                }
            }
            if (!found) {
                return null;
            }
        }

        lastUsed.setTopLevelPackage(new JavaPackage(topLevelPath));
        return newValue;
    }

    private String locateNew(final String value, final String topLevelPath) {
        String newValue = value;
        if (value.startsWith(TOP_LEVEL_PACKAGE_SYMBOL)) {
            if (value.length() > 1) {
                newValue = (value.charAt(1) == '.' ? topLevelPath
                        : topLevelPath + ".") + value.substring(1);
            }
            else {
                newValue = topLevelPath + ".";
            }
        }
        lastUsed.setTopLevelPackage(new JavaPackage(topLevelPath));
        return newValue;
    }

    private String resolveTopLevelPackageSymbol(final String existingData,
            final String topLevelPackage) {
        if (TOP_LEVEL_PACKAGE_SYMBOL.equals(existingData)) {
            // existing data = "~" => "com.foo."
            return topLevelPackage + ".";
        }
        if (existingData.startsWith(TOP_LEVEL_PACKAGE_SYMBOL)) {
            // e.g. turn "~.blah" or "~blah" into "com.foo.blah"
            return topLevelPackage + (existingData.charAt(1) == '.' ? "" : ".")
                    + existingData.substring(1);
        }
        return existingData;
    }
}
