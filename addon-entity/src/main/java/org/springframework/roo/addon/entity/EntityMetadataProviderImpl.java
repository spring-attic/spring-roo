package org.springframework.roo.addon.entity;

import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.configurable.ConfigurableMetadataProvider;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.addon.plural.PluralMetadataProvider;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;

/**
 * Provides {@link EntityMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component(immediate = true)
@Service
public final class EntityMetadataProviderImpl extends AbstractIdentifierServiceAwareMetadataProvider implements EntityMetadataProvider {
	@Reference private ConfigurableMetadataProvider configurableMetadataProvider;
	@Reference private PluralMetadataProvider pluralMetadataProvider;
	
	private boolean noArgConstructor = true;
	
	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		configurableMetadataProvider.addMetadataTrigger(new JavaType(RooEntity.class.getName()));
		pluralMetadataProvider.addMetadataTrigger(new JavaType(RooEntity.class.getName()));
		addMetadataTrigger(new JavaType(RooEntity.class.getName()));
	}
	
	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		configurableMetadataProvider.removeMetadataTrigger(new JavaType(RooEntity.class.getName()));
		pluralMetadataProvider.removeMetadataTrigger(new JavaType(RooEntity.class.getName()));
		removeMetadataTrigger(new JavaType(RooEntity.class.getName()));
	}
	
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		// We know governor type details are non-null and can be safely cast
		
		// Now we walk the inheritance hierarchy until we find some existing EntityMetadata
		EntityMetadata parent = null;
		ClassOrInterfaceTypeDetails superCid = ((ClassOrInterfaceTypeDetails) governorPhysicalTypeMetadata.getMemberHoldingTypeDetails()).getSuperclass();
		while (superCid != null && parent == null) {
			String superCidPhysicalTypeIdentifier = superCid.getDeclaredByMetadataId();
			Path path = PhysicalTypeIdentifier.getPath(superCidPhysicalTypeIdentifier);
			String superCidLocalIdentifier = createLocalIdentifier(superCid.getName(), path);
			parent = (EntityMetadata) metadataService.get(superCidLocalIdentifier);
			superCid = superCid.getSuperclass();
		}
		
		// We also need the plural
		JavaType javaType = EntityMetadata.getJavaType(metadataIdentificationString);
		Path path = EntityMetadata.getPath(metadataIdentificationString);
		String key = PluralMetadata.createIdentifier(javaType, path);
		PluralMetadata pluralMetadata = (PluralMetadata) metadataService.get(key);
		if (pluralMetadata == null) {
			// Can't acquire the plural
			return null;
		}
		metadataDependencyRegistry.registerDependency(key, metadataIdentificationString);
	
		// If the project itself changes, we want a chance to refresh this item
		metadataDependencyRegistry.registerDependency(ProjectMetadata.getProjectIdentifier(), metadataIdentificationString);
		
		// We do not need to monitor the parent, as any changes to the java type associated with the parent will trickle down to
		// the governing java type
		
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());

		ClassOrInterfaceTypeDetails cid = (ClassOrInterfaceTypeDetails) governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();
		MemberDetails memberDetails = memberDetailsScanner.getMemberDetails(this.getClass().getName(), cid);

		List<Identifier> identifierServiceResult = getIdentifiersForType(javaType);
		
		return new EntityMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, parent, noArgConstructor, pluralMetadata.getPlural(), projectMetadata, memberDetails, identifierServiceResult);
	}
	
	public String getItdUniquenessFilenameSuffix() {
		return "Entity";
	}

	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = EntityMetadata.getJavaType(metadataIdentificationString);
		Path path = EntityMetadata.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}

	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return EntityMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return EntityMetadata.getMetadataIdentifierType();
	}

	/**
	 * Allows disabling the automated creation of no arg constructors. This might be appropriate, for example, if another add-on is providing
	 * more sophisticated constructor creation facilities.
	 * 
	 * @param noArgConstructor automatically causes any {@link EntityMetadata} to have a no-arg constructor added if there are zero no-arg
	 * constructors defined in the {@link PhysicalTypeMetadata} (defaults to true).
	 */
	public void setNoArgConstructor(boolean noArgConstructor) {
		this.noArgConstructor = noArgConstructor;
	}
}
