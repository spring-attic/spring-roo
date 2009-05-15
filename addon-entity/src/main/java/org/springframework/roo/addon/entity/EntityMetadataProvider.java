package org.springframework.roo.addon.entity;

import org.springframework.roo.addon.beaninfo.BeanInfoMetadataProvider;
import org.springframework.roo.addon.configurable.ConfigurableMetadataProvider;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.addon.plural.PluralMetadataProvider;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdProviderRole;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;
import org.springframework.roo.support.util.Assert;

/**
 * Provides {@link EntityMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@ScopeDevelopment
public final class EntityMetadataProvider extends AbstractItdMetadataProvider {

	private boolean noArgConstructor = true;
	
	public EntityMetadataProvider(MetadataService metadataService, MetadataDependencyRegistry metadataDependencyRegistry, FileManager fileManager, ConfigurableMetadataProvider configurableMetadataProvider, PluralMetadataProvider pluralMetadataProvider, BeanInfoMetadataProvider beanInfoMetadataProvider) {
		super(metadataService, metadataDependencyRegistry, fileManager);
		Assert.notNull(configurableMetadataProvider, "Configurable metadata provider required");
		Assert.notNull(pluralMetadataProvider, "Plural metadata provider required");
		Assert.notNull(beanInfoMetadataProvider, "Bean info metadata provider required");
		configurableMetadataProvider.addMetadataTrigger(new JavaType(RooEntity.class.getName()));
		pluralMetadataProvider.addMetadataTrigger(new JavaType(RooEntity.class.getName()));
		beanInfoMetadataProvider.addMetadataTrigger(new JavaType(RooEntity.class.getName()));
		addProviderRole(ItdProviderRole.ACCESSOR_MUTATOR);
		addMetadataTrigger(new JavaType(RooEntity.class.getName()));
	}
	
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		// We know governor type details are non-null and can be safely cast
		
		// Now we walk the inheritance hierarchy until we find some existing EntityMetadata
		EntityMetadata parent = null;
		ClassOrInterfaceTypeDetails superCid = ((ClassOrInterfaceTypeDetails)governorPhysicalTypeMetadata.getPhysicalTypeDetails()).getSuperclass();
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
		PluralMetadata beanInfo = (PluralMetadata) metadataService.get(key);
		metadataDependencyRegistry.registerDependency(key, metadataIdentificationString);
		if (beanInfo == null) {
			// Can't acquire the plural
			return null;
		}
		
		// We do not need to monitor the parent, as any changes to the java type associated with the parent will trickle down to
		// the governing java type
		
		return new EntityMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, parent, noArgConstructor, beanInfo.getPlural());
	}
	
	public String getItdUniquenessFilenameSuffix() {
		return "Entity";
	}

	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = EntityMetadata.getJavaType(metadataIdentificationString);
		Path path = EntityMetadata.getPath(metadataIdentificationString);
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(javaType, path);
		return physicalTypeIdentifier;
	}

	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return EntityMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return EntityMetadata.getMetadataIdentiferType();
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
