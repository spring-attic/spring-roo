package org.springframework.roo.addon.web.mvc.controller;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTriggerBasedMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Metadata provider for {@link ConversionServiceMetadata}. Monitors notifications for {@link RooConversionService} and 
 * {@link RooWebScaffold} annotated types. Also listens for changes to the scaffolded domain types and their associated 
 * domain types.
 * 
 * @author Rossen Stoyanchev
 * @since 1.1.1
 */
@Component(immediate = true) 
@Service
public final class ConversionServiceMetadataProviderImpl extends AbstractItdMetadataProvider implements ItdTriggerBasedMetadataProvider {

	private static final Logger logger = HandlerUtils.getLogger(ConversionServiceMetadataProviderImpl.class);

	@Reference private TypeLocationService typeLocationService;

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(new JavaType(RooConversionService.class.getName()));
	}

	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(new JavaType(RooConversionService.class.getName()));
	}

	@Override
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		LinkedHashSet<JavaTypeWrapper> rooJavaTypes = findFormBackingObjectTypes();
		if (! registerDependencies(rooJavaTypes, metadataIdentificationString)) {
			logger.finer("Failed to register for one or form backing object type notifications. Postponing ConversionService Metadata creation!");
			return null;
		}
		return new ConversionServiceMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, rooJavaTypes);
	}

	public String getItdUniquenessFilenameSuffix() {
		return "ConversionService";
	}

	public String getProvidesType() {
		return MetadataIdentificationUtils.create(ConversionServiceMetadata.class.getName());
	}

	@Override
	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(ConversionServiceMetadata.class.getName(), javaType, path);
	}

	@Override
	protected String getGovernorPhysicalTypeIdentifier(String metadataId) {
		JavaType javaType = PhysicalTypeIdentifierNamingUtils.getJavaType(ConversionServiceMetadata.class.getName(), metadataId);
		Path path = PhysicalTypeIdentifierNamingUtils.getPath(ConversionServiceMetadata.class.getName(), metadataId);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
 	}

	/* Private helper methods */

	LinkedHashSet<JavaTypeWrapper> findFormBackingObjectTypes() {
		JavaType rooWebScaffold = new JavaType(RooWebScaffold.class.getName());
		LinkedHashSet<JavaTypeWrapper> formBackingObjects = new LinkedHashSet<JavaTypeWrapper>();
		Set<JavaType> controllers = typeLocationService.findTypesWithAnnotation(rooWebScaffold);
		for (JavaType controller : controllers) {
			AnnotationMetadata annotation = new JavaTypeWrapper(controller, metadataService).getTypeAnnotation(rooWebScaffold);
			JavaType javaType = (JavaType) annotation.getAttribute(new JavaSymbolName("formBackingObject")).getValue();
			formBackingObjects.add(new JavaTypeWrapper(javaType , metadataService));
		}
		return formBackingObjects;
	}

	boolean registerDependencies(LinkedHashSet<JavaTypeWrapper> domainJavaTypes, String metadataId) {
		boolean isSuccessful = true;
		metadataDependencyRegistry.registerDependency(WebScaffoldMetadata.getMetadataIdentiferType(), metadataId);
		for (JavaTypeWrapper domainJavaType : domainJavaTypes) {
			if (! domainJavaType.isValidMetadata()) {
				logger.finer("No BeanInfo or Entity metadata found for " + domainJavaType);
				isSuccessful = false;
			}
			metadataDependencyRegistry.registerDependency(domainJavaType.getBeanInfoMetadataId(), metadataId);
			metadataDependencyRegistry.registerDependency(domainJavaType.getEntityMetadataId(), metadataId);
			Set<JavaTypeWrapper> relatedDomainTypes = domainJavaType.getRelatedDomainTypes();
			if (relatedDomainTypes == null) {
				continue;
			}
			for (JavaTypeWrapper relatedType : relatedDomainTypes) {
				metadataDependencyRegistry.registerDependency(relatedType.getBeanInfoMetadataId(), metadataId);
				metadataDependencyRegistry.registerDependency(relatedType.getEntityMetadataId(), metadataId);
			}
		}
		return isSuccessful;
	}

}