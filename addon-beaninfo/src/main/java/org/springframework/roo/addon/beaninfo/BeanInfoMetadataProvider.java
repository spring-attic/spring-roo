package org.springframework.roo.addon.beaninfo;

import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdProviderRole;
import org.springframework.roo.classpath.itd.ItdRoleAwareMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataItem;
import org.springframework.roo.metadata.MetadataProvider;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;
import org.springframework.roo.support.util.Assert;

/**
 * Provides {@link BeanInfoMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@ScopeDevelopment
public final class BeanInfoMetadataProvider extends AbstractItdMetadataProvider {

	public BeanInfoMetadataProvider(MetadataService metadataService, MetadataDependencyRegistry metadataDependencyRegistry, FileManager fileManager) {
		super(metadataService, metadataDependencyRegistry, fileManager);
		addMetadataTrigger(new JavaType(RooBeanInfo.class.getName()));
	}
	
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		// Create a list of metadata which the metadata should look for accessors within
		List<MemberHoldingTypeDetails> memberHoldingTypeDetails = new ArrayList<MemberHoldingTypeDetails>();
		
		ClassOrInterfaceTypeDetails cid = (ClassOrInterfaceTypeDetails) governorPhysicalTypeMetadata.getPhysicalTypeDetails();
		
		// Build a List representing the class hierarchy, where the first element is the absolute superclass
		List<ClassOrInterfaceTypeDetails> cidHierarchy = new ArrayList<ClassOrInterfaceTypeDetails>();
		while (cid != null) {
			cidHierarchy.add(0, cid);  // note to the top of the list
			cid = cid.getSuperclass();
		}
		
		// Now we add this governor, plus all of its superclasses
		for (ClassOrInterfaceTypeDetails currentClass : cidHierarchy) {
			memberHoldingTypeDetails.add(currentClass);
			
			// Add metadata representing accessors offered by other ITDs 
			for (MetadataProvider provider : metadataService.getRegisteredProviders()) {
				// We're only interested in ITD providers which provide accessors
				if (this.equals(provider) || !(provider instanceof ItdRoleAwareMetadataProvider) || !((ItdRoleAwareMetadataProvider)provider).getRoles().contains(ItdProviderRole.ACCESSOR_MUTATOR)) {
					continue;
				}
				
				// Determine the key the ITD provider uses for this particular type
				String key = ((ItdMetadataProvider)provider).getIdForPhysicalJavaType(currentClass.getDeclaredByMetadataId());
				Assert.isTrue(MetadataIdentificationUtils.isIdentifyingInstance(key), "ITD metadata provider '" + provider + "' returned an illegal key ('" + key + "'");
				
				// Register a dependency, as we need to know whenever an ITD changes its contents
				// Only need to bother for our governor, though - superclasses trickle down to governor anyway, so we find out that way
				if (currentClass.equals(governorPhysicalTypeMetadata.getPhysicalTypeDetails())) {
					// Dealing with governor at the moment, so we should register
					metadataDependencyRegistry.registerDependency(key, metadataIdentificationString);
				}
				
				// Get the metadata and ensure we have ITD type details available
				MetadataItem metadataItem = metadataService.get(key);
				if (metadataItem == null || !metadataItem.isValid()) {
					continue;
				}
				Assert.isInstanceOf(ItdTypeDetailsProvidingMetadataItem.class, metadataItem, "ITD metadata provider '" + provider + "' failed to return the correct metadata type");
				ItdTypeDetailsProvidingMetadataItem itdTypeDetailsMd = (ItdTypeDetailsProvidingMetadataItem) metadataItem;
				if (itdTypeDetailsMd.getItdTypeDetails() == null) {
					continue;
				}
				
				metadataDependencyRegistry.registerDependency(key, metadataIdentificationString);
				
				// Include its accessors
				memberHoldingTypeDetails.add(itdTypeDetailsMd.getItdTypeDetails());
			}
		}

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
