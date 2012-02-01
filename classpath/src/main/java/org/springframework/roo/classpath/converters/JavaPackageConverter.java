package org.springframework.roo.classpath.converters;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.shell.Completion;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

/**
 * A {@link Converter} for {@link JavaPackage}s, with support for using
 * {@value #TOP_LEVEL_PACKAGE_SYMBOL} to denote the user's top-level package.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component
@Service
public class JavaPackageConverter implements Converter<JavaPackage> {

    /**
     * The shell character that represents the current project or module's top
     * level Java package.
     */
    public static final String TOP_LEVEL_PACKAGE_SYMBOL = "~";

    @Reference FileManager fileManager;
    @Reference LastUsed lastUsed;
    @Reference ProjectOperations projectOperations;
    @Reference TypeLocationService typeLocationService;

    public JavaPackage convertFromText(final String value,
            final Class<?> requiredType, final String optionContext) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        final JavaPackage result = new JavaPackage(
                convertToFullyQualifiedPackageName(value));
        if (optionContext != null && optionContext.contains("update")) {
            lastUsed.setPackage(result);
        }
        return result;
    }

    private String convertToFullyQualifiedPackageName(final String text) {
        final String normalisedText = StringUtils.removeEnd(text, ".")
                .toLowerCase();
        if (normalisedText.startsWith(TOP_LEVEL_PACKAGE_SYMBOL)) {
            return replaceTopLevelPackageSymbol(normalisedText);
        }
        return normalisedText;
    }

    public boolean getAllPossibleValues(final List<Completion> completions,
            final Class<?> requiredType, final String existingData,
            final String optionContext, final MethodTarget target) {
        if (projectOperations.isFocusedProjectAvailable()) {
            completions.addAll(getCompletionsForAllKnownPackages());
        }
        return false;
    }

    private Collection<Completion> getCompletionsForAllKnownPackages() {
        final Collection<Completion> completions = new LinkedHashSet<Completion>();
        for (final Pom pom : projectOperations.getPoms()) {
            for (final JavaType javaType : typeLocationService
                    .getTypesForModule(pom)) {
                final String type = javaType.getFullyQualifiedTypeName();
                completions.add(new Completion(type.substring(0,
                        type.lastIndexOf('.'))));
            }
        }
        return completions;
    }

    private String getTopLevelPackage() {
        if (projectOperations.isFocusedProjectAvailable()) {
            return typeLocationService
                    .getTopLevelPackageForModule(projectOperations
                            .getFocusedModule());
        }
        // Shouldn't happen if there's a project, i.e. most of the time
        return "";
    }

    /**
     * Replaces the {@link #TOP_LEVEL_PACKAGE_SYMBOL} at the beginning of the
     * given text with the current project/module's top-level package
     * 
     * @param text
     * @return a well-formed Java package name (might have a trailing dot)
     */
    private String replaceTopLevelPackageSymbol(final String text) {
        final String topLevelPackage = getTopLevelPackage();
        if (TOP_LEVEL_PACKAGE_SYMBOL.equals(text)) {
            return topLevelPackage;
        }
        final String textWithoutSymbol = StringUtils.removeStart(text,
                TOP_LEVEL_PACKAGE_SYMBOL);
        return topLevelPackage + "."
                + StringUtils.removeStart(textWithoutSymbol, ".");
    }

    public boolean supports(final Class<?> requiredType,
            final String optionContext) {
        return JavaPackage.class.isAssignableFrom(requiredType);
    }
}