package org.springframework.roo.addon.web.mvc.controller.details;

import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;

/**
 * Aggregates metadata for a given {@link JavaType} which is needed by Web scaffolding add-ons.
 * 
 * @author Stefan Schmidt
 * @since 1.1.2
 */
public class JavaTypeMetadataDetails {

	private JavaType javaType;
	private String plural;
	private boolean isEnumType;
	private boolean isApplicationType;
	private JavaTypePersistenceMetadataDetails persistenceDetails;
	private String controllerPath;
	
	/**
	 * Constructor for JavaTypeMetadataDetails.
	 * 
	 * @param javaType (must not be null)
	 * @param plural (must contain text)
	 * @param isEnumType 
	 * @param isApplicationType
	 * @param persistenceDetails (may be null if no persistence metadata is present for the javaType)
	 * @param controllerPath (must contain text)
	 */
	public JavaTypeMetadataDetails(JavaType javaType, String plural, boolean isEnumType, boolean isApplicationType, JavaTypePersistenceMetadataDetails persistenceDetails, String controllerPath) {
		Assert.notNull(javaType, "Java type required");
		Assert.hasText(plural, "Plural required");
		Assert.hasText(controllerPath, "Controller path required");
		this.javaType = javaType;
		this.plural = plural;
		this.isEnumType = isEnumType;
		this.isApplicationType = isApplicationType;
		this.persistenceDetails = persistenceDetails;
		this.controllerPath = controllerPath;
	}
	
	public JavaType getJavaType() {
		return javaType;
	}

	public String getPlural() {
		return plural;
	}

	public boolean isEnumType() {
		return isEnumType;
	}

	public boolean isApplicationType() {
		return isApplicationType;
	}

	public JavaTypePersistenceMetadataDetails getPersistenceDetails() {
		return persistenceDetails;
	}
	
	public String getControllerPath() {
		return controllerPath;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((javaType == null) ? 0 : javaType.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JavaTypeMetadataDetails other = (JavaTypeMetadataDetails) obj;
		if (javaType == null) {
			if (other.javaType != null)
				return false;
		} else if (!javaType.equals(other.javaType))
			return false;
		return true;
	}
}
