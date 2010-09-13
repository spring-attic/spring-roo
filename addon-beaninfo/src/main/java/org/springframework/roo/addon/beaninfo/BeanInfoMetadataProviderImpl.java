package org.springframework.roo.addon.beaninfo;

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
 * Provides {@link BeanInfoMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@Component(immediate=true)
@Service
public final class BeanInfoMetadataProviderImpl extends AbstractItdMetadataProvider implements BeanInfoMetadataProvider {

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(new JavaType(RooBeanInfo.class.getName()));
	}
	
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		List<MemberHoldingTypeDetails> memberHoldingTypeDetails = memberDetailsScanner.getMemberDetails(this, (ClassOrInterfaceTypeDetails)governorPhysicalTypeMetadata.getPhysicalTypeDetails()).getDetails();
		return new BeanInfoMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, memberHoldingTypeDetails);
	}
	
	public String getItdUniquenessFilenameSuffix() {
		return "BeanInfo";
	}

	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = BeanInfoMetadata.getJavaType(metadataIdentificationString);
		Path path = BeanInfoMetadata.getPath(metadataIdentificationString);
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(javaType, path);
		return physicalTypeIdentifier;
	}

	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return BeanInfoMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return BeanInfoMetadata.getMetadataIdentiferType();
	}
	
}
