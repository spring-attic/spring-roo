package org.springframework.roo.addon.configurable;

import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Metadata for {@link RooConfigurable}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class ConfigurableMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {
	private static final String PROVIDES_TYPE_STRING = ConfigurableMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);

	public ConfigurableMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");

		if (!isValid()) {
			return;
		}

		if (isConfigurableAnnotationIntroduced()) {
			builder.addAnnotation(getConfigurableAnnotation());
		}

		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}

	/**
	 * Adds the @org.springframework.beans.factory.annotation.Configurable annotation to the type, unless
	 * it already exists.
	 * 
	 * @return the annotation is already exists or will be created, or null if it will not be created (required)
	 */
	public AnnotationMetadata getConfigurableAnnotation() {
		JavaType javaType = new JavaType("org.springframework.beans.factory.annotation.Configurable");

		if (isConfigurableAnnotationIntroduced()) {
			AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(javaType);
			return annotationBuilder.build();
		}

		return MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, javaType);
	}

	/**
	 * Indicates whether the @org.springframework.beans.factory.annotation.Configurable annotation will
	 * be introduced via this ITD.
	 * 
	 * @return true if it will be introduced, false otherwise
	 */
	public boolean isConfigurableAnnotationIntroduced() {
		JavaType javaType = new JavaType("org.springframework.beans.factory.annotation.Configurable");
		AnnotationMetadata result = MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, javaType);
		return result == null;
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("identifier", getId());
		tsc.append("valid", valid);
		tsc.append("aspectName", aspectName);
		tsc.append("destinationType", destination);
		tsc.append("governor", governorPhysicalTypeMetadata.getId());
		tsc.append("configurableIntroduced", isConfigurableAnnotationIntroduced());
		tsc.append("itdTypeDetails", itdTypeDetails);
		return tsc.toString();
	}

	public static String getMetadataIdentiferType() {
		return PROVIDES_TYPE;
	}

	public static String createIdentifier(JavaType javaType, Path path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
	}

	public static JavaType getJavaType(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static Path getPath(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static boolean isValid(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}
}
