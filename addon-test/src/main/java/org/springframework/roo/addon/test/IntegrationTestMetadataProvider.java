package org.springframework.roo.addon.test;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.configurable.ConfigurableMetadataProvider;
import org.springframework.roo.addon.dod.DataOnDemandMetadata;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;

/**
 * Provides {@link IntegrationTestMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@Component(immediate=true)
@Service
public final class IntegrationTestMetadataProvider extends AbstractItdMetadataProvider {
	
	@Reference private ConfigurableMetadataProvider configurableMetadataProvider;

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		// Integration test classes are @Configurable because they may need DI of other DOD classes that provide M:1 relationships
		configurableMetadataProvider.addMetadataTrigger(new JavaType(RooIntegrationTest.class.getName()));
		addMetadataTrigger(new JavaType(RooIntegrationTest.class.getName()));
	}
	
	protected void deactivate(ComponentContext context) {
		configurableMetadataProvider.removeMetadataTrigger(new JavaType(RooIntegrationTest.class.getName()));
	}
	
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		// We know governor type details are non-null and can be safely cast
		
		// We need to parse the annotation, which we expect to be present
		IntegrationTestAnnotationValues annotationValues = new IntegrationTestAnnotationValues(governorPhysicalTypeMetadata);
		if (!annotationValues.isAnnotationFound() || annotationValues.getEntity() == null) {
			return null;
		}
		
		// Lookup the DOD's metadata
		// The DOD must conform to the DOD naming conventions
		JavaType dodJavaType = new JavaType(annotationValues.getEntity().getFullyQualifiedTypeName() + "DataOnDemand");
		String dataOnDemandMetadataKey = DataOnDemandMetadata.createIdentifier(dodJavaType, Path.SRC_TEST_JAVA);
		DataOnDemandMetadata dataOnDemandMetadata = (DataOnDemandMetadata) metadataService.get(dataOnDemandMetadataKey);
		
		// Lookup the entity's metadata
		JavaType javaType = annotationValues.getEntity();
		Path path = Path.SRC_MAIN_JAVA;
		String entityMetadataKey = EntityMetadata.createIdentifier(javaType, path);
		EntityMetadata entityMetadata = (EntityMetadata) metadataService.get(entityMetadataKey);
 		
		// We need to abort if we couldn't find dependent metadata
		if (dataOnDemandMetadata == null || !dataOnDemandMetadata.isValid() || entityMetadata == null || !entityMetadata.isValid()) {
			return null;
			// throw new IllegalStateException("Integration test provider unable to locate details for persistent type '" + javaType.getFullyQualifiedTypeName() + "'");
		}
		
		MethodMetadata identifierAccessorMethod = entityMetadata.getIdentifierAccessor();
		MethodMetadata versionAccessorMethod = entityMetadata.getVersionAccessor();
		MethodMetadata countMethod = entityMetadata.getCountMethod();
		MethodMetadata findMethod = entityMetadata.getFindMethod();
		MethodMetadata findAllMethod = entityMetadata.getFindAllMethod();
		MethodMetadata findEntriesMethods = entityMetadata.getFindEntriesMethod();
		MethodMetadata flushMethod = entityMetadata.getFlushMethod();
		MethodMetadata mergeMethod = entityMetadata.getMergeMethod();
		MethodMetadata persistMethod = entityMetadata.getPersistMethod();
		MethodMetadata removeMethod = entityMetadata.getRemoveMethod();

		// We need to be informed if our dependent metadata changes
		metadataDependencyRegistry.registerDependency(dataOnDemandMetadataKey, metadataIdentificationString);
		metadataDependencyRegistry.registerDependency(entityMetadataKey, metadataIdentificationString);
		
		// We do not need to monitor the parent, as any changes to the java type associated with the parent will trickle down to
		// the governing java type
		
		return new IntegrationTestMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, annotationValues, dataOnDemandMetadata, 
				identifierAccessorMethod, versionAccessorMethod, countMethod, findMethod, findAllMethod, findEntriesMethods, flushMethod, mergeMethod, persistMethod, removeMethod);
	}
	
	public String getItdUniquenessFilenameSuffix() {
		return "IntegrationTest";
	}

	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = IntegrationTestMetadata.getJavaType(metadataIdentificationString);
		Path path = IntegrationTestMetadata.getPath(metadataIdentificationString);
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(javaType, path);
		return physicalTypeIdentifier;
	}

	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return IntegrationTestMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return IntegrationTestMetadata.getMetadataIdentiferType();
	}
}
