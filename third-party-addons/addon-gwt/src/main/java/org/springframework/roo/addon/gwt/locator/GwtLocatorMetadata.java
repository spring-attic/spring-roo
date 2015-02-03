package org.springframework.roo.addon.gwt.locator;

import org.apache.commons.lang3.StringUtils;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.metadata.AbstractMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

public class GwtLocatorMetadata extends AbstractMetadataItem {

    private static final String PROVIDES_TYPE_STRING = GwtLocatorMetadata.class
            .getName();
    private static final String PROVIDES_TYPE = MetadataIdentificationUtils
            .create(PROVIDES_TYPE_STRING);

    public static String createIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return PhysicalTypeIdentifierNamingUtils.createIdentifier(
                PROVIDES_TYPE_STRING, javaType, path);
    }

    public static JavaType getJavaType(final String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.getJavaType(
                PROVIDES_TYPE_STRING, metadataIdentificationString);
    }

    public static String getMetadataIdentifierType() {
        return PROVIDES_TYPE;
    }

    public static LogicalPath getPath(final String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING,
                metadataIdentificationString);
    }

    private final String proxyTypeContents;

    public GwtLocatorMetadata(final String id, final String proxyTypeContents) {
        super(id);
        this.proxyTypeContents = proxyTypeContents;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final GwtLocatorMetadata that = (GwtLocatorMetadata) o;
        return StringUtils.equals(proxyTypeContents, that.proxyTypeContents);
    }

    @Override
    public int hashCode() {
        return proxyTypeContents == null ? 0 : proxyTypeContents.hashCode();
    }
}
