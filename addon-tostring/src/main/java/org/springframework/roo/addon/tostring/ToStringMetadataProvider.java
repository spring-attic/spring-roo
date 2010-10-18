package org.springframework.roo.addon.tostring;

import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;

/**
 * Provides {@link ToStringMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component(immediate = true)
@Service
public final class ToStringMetadataProvider extends AbstractItdMetadataProvider {

	protected void activate(ComponentContext context) {
		// Ensure we're notified of all metadata related to physical Java types, in particular their initial creation
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(new JavaType(RooToString.class.getName()));
	}

	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		List<MemberHoldingTypeDetails> memberHoldingTypeDetails = memberDetailsScanner.getMemberDetails(ToStringMetadataProvider.class.getName(), (ClassOrInterfaceTypeDetails)governorPhysicalTypeMetadata.getPhysicalTypeDetails()).getDetails();
		for (MemberHoldingTypeDetails memberHolder : memberHoldingTypeDetails) {
			metadataDependencyRegistry.registerDependency(memberHolder.getDeclaredByMetadataId(), metadataIdentificationString);
		}
		return new ToStringMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, memberHoldingTypeDetails);
	}

	public String getItdUniquenessFilenameSuffix() {
		return "ToString";
	}

	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = ToStringMetadata.getJavaType(metadataIdentificationString);
		Path path = ToStringMetadata.getPath(metadataIdentificationString);
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(javaType, path);
		return physicalTypeIdentifier;
	}

	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return ToStringMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return ToStringMetadata.getMetadataIdentiferType();
	}
}
