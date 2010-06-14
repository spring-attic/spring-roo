package org.springframework.roo.addon.solr;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.addon.entity.EntityMetadataProvider;
import org.springframework.roo.addon.tostring.ToStringMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.support.util.Assert;

/**
 * Provides {@link SolrMetadata}.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 *
 */
@Component
@Service
public final class SolrMetadataProvider extends AbstractItdMetadataProvider {
	
	@Reference private EntityMetadataProvider entityMetadataProvider;
	@Reference private PathResolver pathResolver;
	@Reference private FileManager fileManager;
	//private FileManager fileManagerS;

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		entityMetadataProvider.addMetadataTrigger(new JavaType(RooSolrSearchable.class.getName()));
		addMetadataTrigger(new JavaType(RooSolrSearchable.class.getName()));
//		this.fileManagerS = fileManager;
		System.out.println("activate: fm " + fileManager + " pr " + pathResolver);
	}
	
	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		entityMetadataProvider.removeMetadataTrigger(new JavaType(RooSolrSearchable.class.getName()));
		removeMetadataTrigger(new JavaType(RooSolrSearchable.class.getName()));
//		System.out.println("deactivate: " + fileManager + " equals " + fileManager.equals(fileManagerS));
		
	}
	
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		// Acquire bean info (we need getters details, specifically)
		JavaType javaType = SolrMetadata.getJavaType(metadataIdentificationString);
		Path path = SolrMetadata.getPath(metadataIdentificationString);
		String entityMetadataKey = EntityMetadata.createIdentifier(javaType, path);
		String beanInfoMetadataKey = BeanInfoMetadata.createIdentifier(javaType, path);
		String toStringMetadataKey = ToStringMetadata.createIdentifier(javaType, path);

		// We need to parse the annotation, which we expect to be present
		SolrSearchAnnotationValues annotationValues = new SolrSearchAnnotationValues(governorPhysicalTypeMetadata);
		if (!annotationValues.isAnnotationFound() || annotationValues.searchMethod == null) {
			return null;
		}
		
		// We want to be notified if the getter info changes in any way 
		metadataDependencyRegistry.registerDependency(entityMetadataKey, metadataIdentificationString);
		metadataDependencyRegistry.registerDependency(beanInfoMetadataKey, metadataIdentificationString);
		metadataDependencyRegistry.registerDependency(toStringMetadataKey, metadataIdentificationString);
		EntityMetadata entityMetadata = (EntityMetadata) metadataService.get(entityMetadataKey);
		BeanInfoMetadata beanInfoMetadata = (BeanInfoMetadata) metadataService.get(beanInfoMetadataKey);
		ToStringMetadata toStringMetadata = (ToStringMetadata) metadataService.get(toStringMetadataKey);
		
		// Abort if we don't have getter information available
		if (entityMetadata == null || beanInfoMetadata == null) {
			return null;
		}
		// Otherwise go off and create the to Solr metadata
		return new SolrMetadata(metadataIdentificationString, aspectName, annotationValues, governorPhysicalTypeMetadata, entityMetadata, beanInfoMetadata, toStringMetadata, metadataService, pathResolver, fileManager);
	}
	
	public String getItdUniquenessFilenameSuffix() {
		return "SolrSearch";
	}

	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = SolrMetadata.getJavaType(metadataIdentificationString);
		Path path = SolrMetadata.getPath(metadataIdentificationString);
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(javaType, path);
		return physicalTypeIdentifier;
	}
	
	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return SolrMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return SolrMetadata.getMetadataIdentiferType();
	}
}