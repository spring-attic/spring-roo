package org.springframework.roo.project.packaging;

import java.util.Arrays;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.GAV;

/**
 * The {@link PackagingProvider} that creates a JAR file.
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
@Component
@Service
public class JarPackaging extends CorePackagingProvider {
	
	// Constants
	private static final Dependency JAXB_API = new Dependency("javax.xml.bind", "jaxb-api", "2.1");
	private static final Dependency JSR250_API = new Dependency("javax.annotation", "jsr250-api", "1.0");

	/**
	 * Constructor invoked by the OSGi container
	 */
	public JarPackaging() {
		super("jar", "jar-pom-template.xml");
	}
	
	@Override
	public boolean isDefault() {
		return true;
	}

	protected void createPom(final JavaPackage topLevelPackage, final String nullableProjectName, final String javaVersion, final GAV parentPom, final String moduleName) {
		super.createPom(topLevelPackage, nullableProjectName, javaVersion, parentPom);

		// TODO might be able to move this block to "createOtherArtifacts" and delete this overriding method
		// Java 5 needs the javax.annotation library (it's included in Java 6 and above), and the jaxb-api for Hibernate
		if ("1.5".equals(javaVersion)) {
			 projectOperations.addDependencies(moduleName, Arrays.asList(JSR250_API, JAXB_API));
		}
	}

	@Override
	protected void createOtherArtifacts() {
		// Set up the Spring application context configuration file
		//applicationContextOperations.createMiddleTierApplicationContext();

		// Set up the logging configuration file
		/*try {
			FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "log4j.properties-template"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, "log4j.properties")).getOutputStream());
		} catch (final IOException e) {
			LOGGER.warning("Unable to install log4j logging configuration");
		}*/
	}
}
