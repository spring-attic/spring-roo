package org.springframework.roo.addon.tostring;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
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
	}
	
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		MemberDetails memberDetails = memberDetailsScanner.getMemberDetails(ToStringMetadataProvider.class.getName(), (ClassOrInterfaceTypeDetails)governorPhysicalTypeMetadata.getMemberHoldingTypeDetails());
		
		List<MethodMetadata> locatedAccessors = new ArrayList<MethodMetadata>();
		for (MemberHoldingTypeDetails memberHolder : memberDetails.getDetails()) {
			// avoid BIM; deprecating
			if (memberHolder.getDeclaredByMetadataId().startsWith("MID:org.springframework.roo.addon.beaninfo.BeanInfoMetadata#")) continue;
			
			// Add the methods we care about
			for (MethodMetadata method : memberHolder.getDeclaredMethods()) {
				if (isMethodOfInterest(method)) {
					locatedAccessors.add(method);
					// Track any changes to that method (eg it goes away)
					metadataDependencyRegistry.registerDependency(method.getDeclaredByMetadataId(), metadataIdentificationString);
				}
			}
		}
		
		return new ToStringMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, locatedAccessors);
	}
	
	protected String getLocalMidToRequest(ItdTypeDetails itdTypeDetails) {
		// Determine if this ITD presents a method we're interested in (namely accessors)
		for (MethodMetadata method : itdTypeDetails.getDeclaredMethods()) {
			if (isMethodOfInterest(method)) {
				// We care about this ITD, so formally request an update so we can scan for it and process it
				
				// Determine the governor for this ITD, and the Path the ITD is stored within
				JavaType governorType = itdTypeDetails.getName();
				String providesType = MetadataIdentificationUtils.getMetadataClass(itdTypeDetails.getDeclaredByMetadataId());
				Path itdPath = PhysicalTypeIdentifierNamingUtils.getPath(providesType, itdTypeDetails.getDeclaredByMetadataId());
				
				//  Produce the local MID we're going to use to make the request
				String localMid = createLocalIdentifier(governorType, itdPath);
				
				// Request the local MID
				return localMid;
			}
		}
		
		return null;
	}

	private boolean isMethodOfInterest(MethodMetadata method) {
		return method.getMethodName().getSymbolName().startsWith("get") && method.getParameterTypes().size() == 0 && Modifier.isPublic(method.getModifier());
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
