package org.springframework.roo.classpath.converters;

import static org.springframework.roo.classpath.converters.JavaPackageConverter.TOP_LEVEL_PACKAGE_SYMBOL;
import static org.springframework.roo.project.LogicalPath.MODULE_PATH_SEPARATOR;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.support.util.AnsiEscapeCode;

/**
 * Records the last Java package and type used.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component
@Service
public class LastUsedImpl implements LastUsed {

    private JavaPackage topLevelPackage;
    private JavaPackage javaPackage;
    private JavaType javaType;
    private Pom module;

    @Reference private ProjectOperations projectOperations;
    @Reference private Shell shell;
    @Reference private TypeLocationService typeLocationService;

    public JavaPackage getJavaPackage() {
        return javaPackage;
    }

    public JavaType getJavaType() {
        return javaType;
    }

    public JavaPackage getTopLevelPackage() {
        return topLevelPackage;
    }

    public void setPackage(final JavaPackage javaPackage) {
        Validate.notNull(javaPackage, "JavaPackage required");
        if (javaPackage.getFullyQualifiedPackageName().startsWith("java.")) {
            return;
        }
        javaType = null;
        this.javaPackage = javaPackage;
        setPromptPath(javaPackage.getFullyQualifiedPackageName());
    }

    private void setPromptPath(final String fullyQualifiedName) {
        if (topLevelPackage == null) {
            return;
        }

        String moduleName = "";
        if (module != null && StringUtils.isNotBlank(module.getModuleName())) {
            moduleName = AnsiEscapeCode.decorate(module.getModuleName()
                    + MODULE_PATH_SEPARATOR, AnsiEscapeCode.FG_CYAN);
        }

        topLevelPackage = new JavaPackage(
                typeLocationService
                        .getTopLevelPackageForModule(projectOperations
                                .getFocusedModule()));
        final String path = moduleName
                + fullyQualifiedName.replace(
                        topLevelPackage.getFullyQualifiedPackageName(),
                        TOP_LEVEL_PACKAGE_SYMBOL);
        shell.setPromptPath(path, StringUtils.isNotBlank(moduleName));
    }

    public void setTopLevelPackage(final JavaPackage topLevelPackage) {
        this.topLevelPackage = topLevelPackage;
    }

    public void setType(final JavaType javaType) {
        Validate.notNull(javaType, "JavaType required");
        if (javaType.getPackage().getFullyQualifiedPackageName()
                .startsWith("java.")) {
            return;
        }
        this.javaType = javaType;
        javaPackage = javaType.getPackage();
        setPromptPath(javaType.getFullyQualifiedTypeName());
    }

    public void setType(final JavaType javaType, final Pom module) {
        Validate.notNull(javaType, "JavaType required");
        if (javaType.getPackage().getFullyQualifiedPackageName()
                .startsWith("java.")) {
            return;
        }
        this.module = module;
        this.javaType = javaType;
        javaPackage = javaType.getPackage();
        setPromptPath(javaType.getFullyQualifiedTypeName());
    }
}
