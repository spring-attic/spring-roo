package org.springframework.roo.project;

import org.springframework.roo.metadata.AbstractMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Represents a project.
 *
 * <p>
 * Each ROO instance has a single project active at any time. Different project add-ons are expected
 * to subclass this {@link ProjectMetadata} and implement the abstract methods.
 *
 * <p>
 * The {@link ProjectMetadata} offers convenience methods for acquiring the project name,
 * top level project package name, registered dependencies and path name resolution services.
 *
 * <p>
 * Concrete subclasses should register the correct dependencies the particular project build
 * system requires, plus read those files whenever they change. Subclasses should also provide a valid
 * {@link PathResolver} implementation that understands the target project layout.
 *
 * @author Ben Alex
 * @author Stefan Schmidt
 * @author Alan Stewart
 * @since 1.0
 */
public class ProjectMetadata extends AbstractMetadataItem {

	// Constants
	/**
	 * The MID for the project-level metadata. Is a constant because each
	 * running instance of Roo only ever manages one project at a time.
	 */
	// MID:org.springframework.roo.project.ProjectMetadata#the_project
	private static final String PROJECT_IDENTIFIER = MetadataIdentificationUtils.create(ProjectMetadata.class.getName(), "the_project");

	/**
	 * Returns the metadata ID for the project-level metadata. Is static because
	 * each running instance of Roo only ever manages one project at a time.
	 * 
	 * @param moduleName
	 * @return a non-blank MID
	 */
	public static String getProjectIdentifier(final String moduleName) {
		StringBuilder sb = new StringBuilder(PROJECT_IDENTIFIER);
		if (StringUtils.hasText(moduleName)) {
			sb.append("?").append(moduleName);
		}
		return sb.toString();
	}

	public static boolean isValid(final String metadataIdentificationString) {
		return metadataIdentificationString.startsWith(PROJECT_IDENTIFIER);
	}

	public static String getModuleName(final String metadataIdentificationString) {
		if (metadataIdentificationString.contains("?")) {
			return metadataIdentificationString.substring(metadataIdentificationString.lastIndexOf('?') + 1, metadataIdentificationString.length());
		}
		return "";
	}
	
	// Fields
	private final Pom pom;

	/**
	 * Constructor
	 *
	 * @param pom (required)
	 */
	public ProjectMetadata(final Pom pom) {
		super(getProjectIdentifier(pom.getModuleName()));
		Assert.notNull(pom, "POMs required");
		this.pom = pom;
	}

	public Pom getPom() {
		return pom;
	}

	public String getModuleName() {
		return pom.getModuleName();
	}

	@Override
	public final String toString() {
		final ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("identifier", getId());
		tsc.append("valid", isValid());
		tsc.append("pom", pom);
		return tsc.toString();
	}
}
