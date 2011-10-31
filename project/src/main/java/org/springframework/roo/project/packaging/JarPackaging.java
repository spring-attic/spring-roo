package org.springframework.roo.project.packaging;

import java.util.Arrays;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.GAV;
import org.springframework.roo.project.ProjectOperations;

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
	// Java 5 needs the javax.annotation library (it's included in Java 6 and above), and the jaxb-api for Hibernate
	private static final List<Dependency> JAVA_5_DEPENDENCIES = Arrays.asList(JSR250_API, JAXB_API);

	/**
	 * Constructor invoked by the OSGi container
	 */
	public JarPackaging() {
		super("jar", "jar-pom-template.xml");
	}
	
	public String getId() {
		return "jar";
	}
	
	public boolean isDefault() {
		return true;
	}

	protected String createPom(final JavaPackage topLevelPackage, final String nullableProjectName, final String javaVersion, final GAV parentPom, final String moduleName, final ProjectOperations projectOperations) {
		final String pomPath = super.createPom(topLevelPackage, nullableProjectName, javaVersion, parentPom, moduleName, projectOperations);
		if ("1.5".equals(javaVersion)) {
			projectOperations.addDependencies(moduleName, JAVA_5_DEPENDENCIES);
		}
		return pomPath;
	}

	@Override
	protected void createOtherArtifacts(final JavaPackage topLevelPackage, final String module) {
		applicationContextOperations.createMiddleTierApplicationContext(topLevelPackage, module);
	}
}
