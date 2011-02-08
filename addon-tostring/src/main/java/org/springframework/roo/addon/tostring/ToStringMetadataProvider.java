package org.springframework.roo.addon.tostring;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
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
public final class ToStringMetadataProvider extends AbstractMemberDiscoveringItdMetadataProvider {

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.addNotificationListener(this);
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(new JavaType(RooToString.class.getName()));
	}

	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.removeNotificationListener(this);
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(new JavaType(RooToString.class.getName()));
	}
	
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		MemberDetails memberDetails = memberDetailsScanner.getMemberDetails(ToStringMetadataProvider.class.getName(), (ClassOrInterfaceTypeDetails) governorPhysicalTypeMetadata.getMemberHoldingTypeDetails());

		List<MethodMetadata> locatedAccessors = new ArrayList<MethodMetadata>();
		for (MethodMetadata method : MemberFindingUtils.getMethods(memberDetails)) {
			if (BeanInfoUtils.isAccessorMethod(method)) {
				locatedAccessors.add(method);
				// Track any changes to that method (eg it goes away)
				metadataDependencyRegistry.registerDependency(method.getDeclaredByMetadataId(), metadataIdentificationString);
			}
		}

		return new ToStringMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, locatedAccessors);
	}
	
	protected String getLocalMidToRequest(ItdTypeDetails itdTypeDetails) {
		// Determine if this ITD presents a method we're interested in (namely accessors)
		for (MethodMetadata method : itdTypeDetails.getDeclaredMethods()) {
			if (BeanInfoUtils.isAccessorMethod(method)) {
				return getLocalMid(itdTypeDetails);
			}
		}
		
		return null;
	}

	public String getItdUniquenessFilenameSuffix() {
		return "ToString";
	}

	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = ToStringMetadata.getJavaType(metadataIdentificationString);
		Path path = ToStringMetadata.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}

	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return ToStringMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return ToStringMetadata.getMetadataIdentiferType();
	}
}
