package org.springframework.roo.addon.layers.repository;

import java.util.Arrays;

import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.uaa.client.util.Assert;

/**
 * 
 * @author Stefan Schmidt
 * @author Andrew Swan
 * @since 1.2
 */
public class RepositoryJpaMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {
	
	// Constants
	private static final String PROVIDES_TYPE_STRING = RepositoryJpaMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	private static final String SPRING_JPA_REPOSITORY = "org.springframework.data.jpa.repository.JpaRepository";
	
	/**
	 * Constructor
	 *
	 * @param identifier the identifier for this item of metadata (required)
	 * @param aspectName the Java type of the ITD (required)
	 * @param governorPhysicalTypeMetadata the governor, which is expected to contain a {@link ClassOrInterfaceTypeDetails} (required)
	 * @param governorDetails (required)
	 * @param annotationValues (required)
	 */
	public RepositoryJpaMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, MemberDetails governorDetails, RepositoryJpaAnnotationValues annotationValues) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.notNull(annotationValues, "Annotation values required");
		Assert.notNull(governorDetails, "Governor member details required");
		
		// Make the user's Repository interface extend Spring Data's JpaRepository interface if it doesn't already
		final JavaType springJpaRepository = new JavaType(SPRING_JPA_REPOSITORY, 0, DataType.TYPE, null, Arrays.asList(annotationValues.getDomainType(), JavaType.LONG_OBJECT));
		if (!governorPhysicalTypeMetadata.getMemberHoldingTypeDetails().extendsType(springJpaRepository)) {
			builder.addExtendsTypes(springJpaRepository);
		}
		
		// Build the ITD
		itdTypeDetails = builder.build();
	}

	public static final String getMetadataIdentiferType() {
		return PROVIDES_TYPE;
	}

	public static final String createIdentifier(JavaType javaType, Path path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
	}

	public static final JavaType getJavaType(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static final Path getPath(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static boolean isValid(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}
	
	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("identifier", getId());
		tsc.append("valid", valid);
		tsc.append("aspectName", aspectName);
		tsc.append("destinationType", destination);
		tsc.append("governor", governorPhysicalTypeMetadata.getId());
		tsc.append("itdTypeDetails", itdTypeDetails);
		return tsc.toString();
	}
}
