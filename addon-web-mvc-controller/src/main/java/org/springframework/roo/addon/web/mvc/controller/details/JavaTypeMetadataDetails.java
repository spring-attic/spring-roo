package org.springframework.roo.addon.web.mvc.controller.details;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.model.JavaType;

/**
 * Aggregates metadata for a given {@link JavaType} which is needed by Web
 * scaffolding add-ons.
 * 
 * @author Stefan Schmidt
 * @since 1.1.2
 */
public class JavaTypeMetadataDetails {

    private final String controllerPath;
    private final boolean isApplicationType;
    private final boolean isEnumType;
    private final JavaType javaType;
    private final JavaTypePersistenceMetadataDetails persistenceDetails;
    private final String plural;

    /**
     * Constructor for JavaTypeMetadataDetails.
     * 
     * @param javaType (must not be null)
     * @param plural (must contain text)
     * @param isEnumType
     * @param isApplicationType
     * @param persistenceDetails (may be null if no persistence metadata is
     *            present for the javaType)
     * @param controllerPath (must contain text)
     */
    public JavaTypeMetadataDetails(final JavaType javaType,
            final String plural, final boolean isEnumType,
            final boolean isApplicationType,
            final JavaTypePersistenceMetadataDetails persistenceDetails,
            final String controllerPath) {
        Validate.notNull(javaType, "Java type required");
        Validate.notBlank(plural, "Plural required");
        Validate.notBlank(controllerPath, "Controller path required");
        this.javaType = javaType;
        this.plural = plural;
        this.isEnumType = isEnumType;
        this.isApplicationType = isApplicationType;
        this.persistenceDetails = persistenceDetails;
        this.controllerPath = controllerPath;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof JavaTypeMetadataDetails)) {
            return false;
        }
        return javaType.equals(((JavaTypeMetadataDetails) obj).getJavaType());
    }

    public String getControllerPath() {
        return controllerPath;
    }

    public JavaType getJavaType() {
        return javaType;
    }

    public JavaTypePersistenceMetadataDetails getPersistenceDetails() {
        return persistenceDetails;
    }

    public String getPlural() {
        return plural;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (javaType == null ? 0 : javaType.hashCode());
        return result;
    }

    public boolean isApplicationType() {
        return isApplicationType;
    }

    public boolean isEnumType() {
        return isEnumType;
    }

    /**
     * Indicates whether this {@link JavaType} is persisted by the application.
     * 
     * @return <code>false</code> if for example it's an enum type
     * @since 1.2.1
     */
    public boolean isPersistent() {
        return isApplicationType && persistenceDetails != null;
    }

    @Override
    public String toString() {
        // For debugging
        return javaType.getFullyQualifiedTypeName();
    }
}
