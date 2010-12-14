package org.springframework.roo.addon.web.mvc.controller;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.PhysicalTypeMetadataProvider;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.IdentifiableAnnotatedJavaStructure;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTriggerBasedMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.util.Assert;

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

	@Reference private TypeLocationService typeLocationService;
	@Reference private PhysicalTypeMetadataProvider physicalTypeMetadataProvider;

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		metadataDependencyRegistry.registerDependency(WebScaffoldMetadata.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(new JavaType(RooConversionService.class.getName()));
	}

	@Override
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		LinkedHashSet<DomainJavaType> formBackingObjectTypes = findFormBackingObjectTypes();
		if (! registerDependencies(formBackingObjectTypes, metadataIdentificationString)) {
			return null;
		}
		return new ConversionServiceMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, formBackingObjectTypes);
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

	LinkedHashSet<DomainJavaType> findFormBackingObjectTypes() {
		JavaType rooWebScaffold = new JavaType(RooWebScaffold.class.getName());
		LinkedHashSet<DomainJavaType> formBackingObjects = new LinkedHashSet<DomainJavaType>();
		Set<JavaType> controllers = typeLocationService.findTypesWithAnnotation(rooWebScaffold);
		for (JavaType controller : controllers) {
			String id = physicalTypeMetadataProvider.findIdentifier(controller);
			if (id == null) {
				throw new IllegalArgumentException("Cannot locate source for '" + controller.getFullyQualifiedTypeName() + "'");
			}
			PhysicalTypeMetadata ptm = (PhysicalTypeMetadata) metadataService.get(id);
			Assert.notNull(ptm, "Java source code unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(id));
			PhysicalTypeDetails ptd = ptm.getPhysicalTypeDetails();
			Assert.notNull(ptd, "Java source code details unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(id));
			List<AnnotationMetadata> annotations = ((IdentifiableAnnotatedJavaStructure) ptd).getAnnotations();
			AnnotationMetadata annotation = MemberFindingUtils.getAnnotationOfType(annotations, rooWebScaffold);
			AnnotationAttributeValue<?> attribute = annotation.getAttribute(new JavaSymbolName("formBackingObject"));
			JavaType javaType = (JavaType) attribute.getValue();
			formBackingObjects.add(new DomainJavaType(javaType , metadataService, memberDetailsScanner));
		}
		return formBackingObjects;
	}

	boolean registerDependencies(LinkedHashSet<DomainJavaType> domainJavaTypes, String metadataId) {
		for (DomainJavaType domainJavaType : domainJavaTypes) {
			if (! domainJavaType.isValidMetadata()) {
				return false;
			}
			metadataDependencyRegistry.registerDependency(domainJavaType.getBeanInfoMetadataId(), metadataId);
			metadataDependencyRegistry.registerDependency(domainJavaType.getEntityMetadataId(), metadataId);
			for (DomainJavaType relatedType : domainJavaType.getRelatedDomainTypes()) {
				metadataDependencyRegistry.registerDependency(relatedType.getBeanInfoMetadataId(), metadataId);
			}
		}
		return true;
	}

}
