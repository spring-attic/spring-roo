package org.springframework.roo.addon.dod;

import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.addon.configurable.ConfigurableMetadataProvider;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;
import org.springframework.roo.support.util.Assert;

/**
 * Provides {@link DataOnDemandMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@ScopeDevelopment
public final class DataOnDemandMetadataProvider extends AbstractItdMetadataProvider {
	
	public DataOnDemandMetadataProvider(MetadataService metadataService, MetadataDependencyRegistry metadataDependencyRegistry, FileManager fileManager, ConfigurableMetadataProvider configurableMetadataProvider) {
		super(metadataService, metadataDependencyRegistry, fileManager);
		
		// DOD classes are @Configurable because they may need DI of other DOD classes that provide M:1 relationships
		Assert.notNull(configurableMetadataProvider, "Configurable metadata provider required");
		configurableMetadataProvider.addMetadataTrigger(new JavaType(RooDataOnDemand.class.getName()));
		
		addMetadataTrigger(new JavaType(RooDataOnDemand.class.getName()));
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
		
		// We need to lookup the metadata we depend on
		BeanInfoMetadata beanInfoMetadata = (BeanInfoMetadata) metadataService.get(beanInfoMetadataKey);
		EntityMetadata entityMetadata = (EntityMetadata) metadataService.get(entityMetadataKey);
 		
		// We need to abort if we couldn't find dependent metadata
		if (beanInfoMetadata == null || !beanInfoMetadata.isValid() || entityMetadata == null || !entityMetadata.isValid()) {
			return null;
//			throw new IllegalStateException("Data on demand unable to locate details for persistent type '" + javaType.getFullyQualifiedTypeName() + "'");
		}
		
		MethodMetadata findEntriesMethod = entityMetadata.getFindEntriesMethod();
		if (findEntriesMethod == null) {
			return null;
			//throw new IllegalStateException("Data on demand requires '" + javaType.getFullyQualifiedTypeName() + "' to provide a 'findEntries' method");
		}
		
		MethodMetadata persistMethod = entityMetadata.getPersistMethod();
		
		// We need to be informed if our dependent metadata changes
		metadataDependencyRegistry.registerDependency(beanInfoMetadataKey, metadataIdentificationString);
		metadataDependencyRegistry.registerDependency(entityMetadataKey, metadataIdentificationString);
		
		// We do not need to monitor the parent, as any changes to the java type associated with the parent will trickle down to
		// the governing java type
		
		return new DataOnDemandMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, annotationValues, beanInfoMetadata, findEntriesMethod, persistMethod, metadataService, metadataDependencyRegistry);
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
