package org.springframework.roo.project.packaging;

import static org.springframework.roo.project.Path.SPRING_CONFIG_ROOT;
import static org.springframework.roo.project.Path.SRC_MAIN_JAVA;
import static org.springframework.roo.project.Path.SRC_MAIN_RESOURCES;
import static org.springframework.roo.project.Path.SRC_TEST_JAVA;
import static org.springframework.roo.project.Path.SRC_TEST_RESOURCES;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.GAV;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;

/**
 * The {@link PackagingProvider} that creates a JAR file.
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
@Component
@Service
public class JarPackaging extends AbstractCorePackagingProvider {

    private static final Dependency JAXB_API = new Dependency("javax.xml.bind",
            "jaxb-api", "2.1");
    private static final Dependency JSR250_API = new Dependency(
            "javax.annotation", "jsr250-api", "1.0");
    // Java 5 needs the javax.annotation library (it's included in Java 6 and
    // above), and the jaxb-api for Hibernate
    private static final List<Dependency> JAVA_5_DEPENDENCIES = Arrays.asList(
            JSR250_API, JAXB_API);
    public static final String NAME = "jar";

    /**
     * Constructor invoked by the OSGi container
     */
    public JarPackaging() {
        super(NAME, "jar-pom-template.xml");
    }

    @Override
    protected void createOtherArtifacts(final JavaPackage topLevelPackage,
            final String module, final ProjectOperations projectOperations) {

        super.createOtherArtifacts(topLevelPackage, module, projectOperations);
        final String fullyQualifiedModuleName = getFullyQualifiedModuleName(
                module, projectOperations);
        applicationContextOperations.createMiddleTierApplicationContext(
                topLevelPackage, fullyQualifiedModuleName);
    }

    @Override
    protected String createPom(final JavaPackage topLevelPackage,
            final String nullableProjectName, final String javaVersion,
            final GAV parentPom, final String moduleName,
            final ProjectOperations projectOperations) {

        final String pomPath = super.createPom(topLevelPackage,
                nullableProjectName, javaVersion, parentPom, moduleName,
                projectOperations);
        if ("1.5".equals(javaVersion)) {
            projectOperations.addDependencies(moduleName, JAVA_5_DEPENDENCIES);
        }
        return pomPath;
    }

    public Collection<Path> getPaths() {
        return Arrays.asList(SRC_MAIN_JAVA, SRC_MAIN_RESOURCES, SRC_TEST_JAVA,
                SRC_TEST_RESOURCES, SPRING_CONFIG_ROOT);
    }

    @Override
    public boolean isDefault() {
        return true;
    }
}
