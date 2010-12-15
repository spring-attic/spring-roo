package org.springframework.roo.addon.dod;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.addon.configurable.ConfigurableMetadataProvider;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;

/**
 * Provides {@link DataOnDemandMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component(immediate = true)
@Service
public final class DataOnDemandMetadataProvider extends AbstractItdMetadataProvider {
	@Reference private ConfigurableMetadataProvider configurableMetadataProvider;
	
	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		// DOD classes are @Configurable because they may need DI of other DOD classes that provide M:1 relationships
		configurableMetadataProvider.addMetadataTrigger(new JavaType(RooDataOnDemand.class.getName()));
		addMetadataTrigger(new JavaType(RooDataOnDemand.class.getName()));
	}
	
	protected void deactivate(ComponentContext context) {
		configurableMetadataProvider.removeMetadataTrigger(new JavaType(RooDataOnDemand.class.getName()));
	}
	
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		// We know governor type details are non-null and can be safely cast
		
		// We need to parse the annotation, which we expect to be present
		DataOnDemandAnnotationValues annotationValues = new DataOnDemandAnnotationValues(governorPhysicalTypeMetadata);
		if (!annotationValues.isAnnotationFound() || annotationValues.getEntity() == null) {
			return null;
		}
		
		// Lookup the entity's metadata
		JavaType javaType = annotationValues.getEntity();
		Path path = Path.SRC_MAIN_JAVA;
		String beanInfoMetadataKey = BeanInfoMetadata.createIdentifier(javaType, path);
		String entityMetadataKey = EntityMetadata.createIdentifier(javaType, path);
		String classOrInterfaceMetadataKey = PhysicalTypeIdentifier.createIdentifier(javaType, path);
		
		// We need to lookup the metadata we depend on
		BeanInfoMetadata beanInfoMetadata = (BeanInfoMetadata) metadataService.get(beanInfoMetadataKey);
		EntityMetadata entityMetadata = (EntityMetadata) metadataService.get(entityMetadataKey);
		PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(classOrInterfaceMetadataKey);
		if (physicalTypeMetadata == null) {
			return null;
		}
		ClassOrInterfaceTypeDetails entityClassOrInterfaceTypeDetails = (ClassOrInterfaceTypeDetails) physicalTypeMetadata.getPhysicalTypeDetails();
		
		// We need to abort if we couldn't find dependent metadata
		if (beanInfoMetadata == null || !beanInfoMetadata.isValid() || entityMetadata == null || !entityMetadata.isValid()) {
			return null;
		}
		
		MethodMetadata findEntriesMethod = entityMetadata.getFindEntriesMethod();
		if (findEntriesMethod == null) {
			return null;
		}
		
		MethodMetadata persistMethod = entityMetadata.getPersistMethod();
		MethodMetadata flushMethod = entityMetadata.getFlushMethod();

		MethodMetadata findMethod = entityMetadata.getFindMethod();
		MethodMetadata identifierAccessor = entityMetadata.getIdentifierAccessor();
		
		// We need to be informed if our dependent metadata changes
		metadataDependencyRegistry.registerDependency(beanInfoMetadataKey, metadataIdentificationString);
		metadataDependencyRegistry.registerDependency(entityMetadataKey, metadataIdentificationString);
		
		// We do not need to monitor the parent, as any changes to the java type associated with the parent will trickle down to
		// the governing java type
		
		return new DataOnDemandMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, annotationValues, beanInfoMetadata, identifierAccessor, findMethod, findEntriesMethod, persistMethod, flushMethod, metadataService, metadataDependencyRegistry, memberDetailsScanner, entityClassOrInterfaceTypeDetails);
	}
	
	public String getItdUniquenessFilenameSuffix() {
		return "DataOnDemand";
	}

	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = DataOnDemandMetadata.getJavaType(metadataIdentificationString);
		Path path = DataOnDemandMetadata.getPath(metadataIdentificationString);
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(javaType, path);
		return physicalTypeIdentifier;
	}

	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return DataOnDemandMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return DataOnDemandMetadata.getMetadataIdentiferType();
	}
}
