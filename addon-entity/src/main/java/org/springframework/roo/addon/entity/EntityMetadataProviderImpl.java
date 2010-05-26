package org.springframework.roo.addon.entity;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
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
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;

/**
 * Provides {@link EntityMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@Component(immediate = true)
@Service
public final class EntityMetadataProviderImpl extends AbstractItdMetadataProvider implements EntityMetadataProvider {
	@Reference private ConfigurableMetadataProvider configurableMetadataProvider;
	@Reference private PluralMetadataProvider pluralMetadataProvider;
	@Reference private BeanInfoMetadataProvider beanInfoMetadataProvider;
	
	private boolean noArgConstructor = true;
	
	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		metadataDependencyRegistry.registerDependency(ProjectMetadata.getProjectIdentifier(), getProvidesType());
		configurableMetadataProvider.addMetadataTrigger(new JavaType(RooEntity.class.getName()));
		pluralMetadataProvider.addMetadataTrigger(new JavaType(RooEntity.class.getName()));
		beanInfoMetadataProvider.addMetadataTrigger(new JavaType(RooEntity.class.getName()));
		addProviderRole(ItdProviderRole.ACCESSOR_MUTATOR);
		addMetadataTrigger(new JavaType(RooEntity.class.getName()));
	}
	
	protected void deactivate(ComponentContext context) {
		configurableMetadataProvider.removeMetadataTrigger(new JavaType(RooEntity.class.getName()));
		pluralMetadataProvider.removeMetadataTrigger(new JavaType(RooEntity.class.getName()));
		beanInfoMetadataProvider.removeMetadataTrigger(new JavaType(RooEntity.class.getName()));
	}
	
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		// We know governor type details are non-null and can be safely cast
		
		// Now we walk the inheritance hierarchy until we find some existing EntityMetadata
		EntityMetadata parent = null;
		ClassOrInterfaceTypeDetails superCid = ((ClassOrInterfaceTypeDetails) governorPhysicalTypeMetadata.getPhysicalTypeDetails()).getSuperclass();
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
		
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());

		return new EntityMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, parent, noArgConstructor, beanInfo.getPlural(), projectMetadata);
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
