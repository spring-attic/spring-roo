package org.springframework.roo.addon.gwt.request;

import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.metadata.AbstractMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.ContextualPath;

public class GwtRequestMetadata extends AbstractMetadataItem {

	// Constants
	private static final String PROVIDES_TYPE_STRING = GwtRequestMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);

	// Fields
	private final String requestTypeContents;

	public GwtRequestMetadata(final String id, final String requestTypeContents) {
		super(id);
		this.requestTypeContents = requestTypeContents;
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
		GwtRequestMetadata that = (GwtRequestMetadata) o;
		return !(requestTypeContents != null ? !requestTypeContents.equals(that.requestTypeContents) : that.requestTypeContents != null);
	}

	@Override
	public int hashCode() {
		return requestTypeContents != null ? requestTypeContents.hashCode() : 0;
	}
}
