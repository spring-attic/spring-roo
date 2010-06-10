package org.springframework.roo.addon.dbre;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;

/**
 * Provides {@link DbreMetadata}.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Component
@Service
public class DbreMetadataProviderImpl extends AbstractItdMetadataProvider implements DbreMetadataProvider {

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		metadataDependencyRegistry.registerDependency(ProjectMetadata.getProjectIdentifier(), getProvidesType());
//		configurableMetadataProvider.addMetadataTrigger(new JavaType(RooEntity.class.getName()));
//		pluralMetadataProvider.addMetadataTrigger(new JavaType(RooEntity.class.getName()));
//		beanInfoMetadataProvider.addMetadataTrigger(new JavaType(RooEntity.class.getName()));
//		addProviderRole(ItdProviderRole.ACCESSOR_MUTATOR);
		addMetadataTrigger(new JavaType(RooDbManaged.class.getName()));
	}

	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return DbreMetadata.createIdentifier(javaType, path);
	}

	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = DbreMetadata.getJavaType(metadataIdentificationString);
		Path path = DbreMetadata.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}

	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		// TODO Auto-generated method stub
		// System.out.println("in DbreMetadataProviderImpl.getMetadata");
		return null;
	}

	public String getItdUniquenessFilenameSuffix() {
		return "Dbre";
	}

	public String getProvidesType() {
		return DbreMetadata.getMetadataIdentiferType();
	}
}
