package org.springframework.roo.addon.gwt.proxy;

import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.metadata.AbstractMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.ContextualPath;

public class GwtProxyMetadata extends AbstractMetadataItem {

	// Constants
	private static final String PROVIDES_TYPE_STRING = GwtProxyMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);

	// Fields
	private final String proxyTypeContents;

	public GwtProxyMetadata(final String id, final String proxyTypeContents) {
		super(id);
		this.proxyTypeContents = proxyTypeContents;
	}

	public static String getMetadataIdentifierType() {
		return PROVIDES_TYPE;
	}

	public static String createIdentifier(final JavaType javaType, final ContextualPath path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
	}

	public static JavaType getJavaType(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static ContextualPath getPath(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		GwtProxyMetadata that = (GwtProxyMetadata) o;
		return !(proxyTypeContents != null ? !proxyTypeContents.equals(that.proxyTypeContents) : that.proxyTypeContents != null);
	}

	@Override
	public int hashCode() {
		return proxyTypeContents != null ? proxyTypeContents.hashCode() : 0;
	}
}
