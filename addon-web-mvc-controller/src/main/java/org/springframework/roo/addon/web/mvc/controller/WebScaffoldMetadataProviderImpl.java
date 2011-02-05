package org.springframework.roo.addon.web.mvc.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.addon.beaninfo.BeanInfoUtils;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.addon.finder.FinderMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;

/**
 * Provides {@link WebScaffoldMetadata}.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
@Component(immediate = true) 
@Service 
public final class WebScaffoldMetadataProviderImpl extends AbstractItdMetadataProvider implements WebScaffoldMetadataProvider {
	@Reference private ControllerOperations controllerOperations;
	@Reference private TypeLocationService typeLocationService;
	@Reference private ConversionServiceOperations conversionServiceOperations;

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(new JavaType(RooWebScaffold.class.getName()));
	}

	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		
		// We know governor type details are non-null and can be safely cast

		// We need to parse the annotation, which we expect to be present
		WebScaffoldAnnotationValues annotationValues = new WebScaffoldAnnotationValues(governorPhysicalTypeMetadata);
		if (!annotationValues.isAnnotationFound() || annotationValues.formBackingObject == null) {
			return null;
		}

		// Lookup the form backing object's metadata
		JavaType javaType = annotationValues.formBackingObject;
		Path path = Path.SRC_MAIN_JAVA;
		String beanInfoMetadataKey = BeanInfoMetadata.createIdentifier(javaType, path);
		String entityMetadataKey = EntityMetadata.createIdentifier(javaType, path);

		// We need to lookup the metadata we depend on
		BeanInfoMetadata beanInfoMetadata = (BeanInfoMetadata) metadataService.get(beanInfoMetadataKey);
		EntityMetadata entityMetadata = (EntityMetadata) metadataService.get(entityMetadataKey);
		

		// We need to abort if we couldn't find dependent metadata
		if (beanInfoMetadata == null || !beanInfoMetadata.isValid() || entityMetadata == null || !entityMetadata.isValid()) {
			return null;
		}

		// We need to be informed if our dependent metadata changes
		metadataDependencyRegistry.registerDependency(beanInfoMetadataKey, metadataIdentificationString);
		metadataDependencyRegistry.registerDependency(entityMetadataKey, metadataIdentificationString);
		
		// We also need to be informed if a referenced type is changed
		for (JavaType type : getSpecialDomainTypes(beanInfoMetadata.getJavaBean())) {
			metadataDependencyRegistry.registerDependency(BeanInfoMetadata.createIdentifier(type, path), metadataIdentificationString);
		}
		
		// We want the methods that represent dynamic finders, if there are any
		List<MethodMetadata> dynamicFinderMethods = new ArrayList<MethodMetadata>();
		String finderMetadataKey = FinderMetadata.createIdentifier(javaType, path);
		FinderMetadata finderMetadata = (FinderMetadata) metadataService.get(finderMetadataKey);
		if (finderMetadata != null) {
			dynamicFinderMethods = finderMetadata.getAllDynamicFinders();
		}
		
		installConversionService(governorPhysicalTypeMetadata.getMemberHoldingTypeDetails().getName());

		// We do not need to monitor the parent, as any changes to the java type associated with the parent will trickle down to
		// the governing java type
		return new WebScaffoldMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, metadataService, memberDetailsScanner, annotationValues, beanInfoMetadata, entityMetadata, dynamicFinderMethods, controllerOperations);
	}

	private SortedSet<JavaType> getSpecialDomainTypes(JavaType javaType) {
		SortedSet<JavaType> specialTypes = new TreeSet<JavaType>();
		BeanInfoMetadata bim = (BeanInfoMetadata) metadataService.get(BeanInfoMetadata.createIdentifier(javaType, Path.SRC_MAIN_JAVA));
		if (bim == null) {
			// Unable to get metadata so it is not a JavaType in our project anyway.
			return specialTypes;
		}
		EntityMetadata em = (EntityMetadata) metadataService.get(EntityMetadata.createIdentifier(javaType, Path.SRC_MAIN_JAVA));
		if (em == null) {
			// Unable to get entity metadata so it is not a Entity in our project anyway.
			return specialTypes;
		}
		for (MethodMetadata accessor : bim.getPublicAccessors(false)) {
			// Not interested in identifiers and version fields
			if (accessor.equals(em.getIdentifierAccessor()) || accessor.equals(em.getVersionAccessor())) {
				continue;
			}
			// Not interested in fields that are not exposed via a mutator
			FieldMetadata fieldMetadata = bim.getFieldForPropertyName(BeanInfoUtils.getPropertyNameForJavaBeanMethod(accessor));
			if (fieldMetadata == null || !hasMutator(fieldMetadata, bim)) {
				continue;
			}
			JavaType type = accessor.getReturnType();
			if (type.isCommonCollectionType()) {
				for (JavaType genericType : type.getParameters()) {
					if (isSpecialType(genericType)) {
						specialTypes.add(genericType);
					}
				}
			} else {
				if (isSpecialType(type)) {

					specialTypes.add(type);
				}
			}
		}
		return specialTypes;
	}
	
	void installConversionService(JavaType governor) {
		JavaType rooConversionService = new JavaType(RooConversionService.class.getName());
		if (typeLocationService.findTypesWithAnnotation(rooConversionService).size() > 0) {
			return;
		}
		JavaType rooWebScaffold = new JavaType(RooWebScaffold.class.getName());
		for (ClassOrInterfaceTypeDetails controller : typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(rooWebScaffold)) {
			AnnotationMetadata annotation = MemberFindingUtils.getTypeAnnotation(controller, rooWebScaffold);
			AnnotationAttributeValue<?> attr = annotation.getAttribute(new JavaSymbolName("registerConverters"));
			if (attr != null) {
				if (Boolean.FALSE.equals(attr.getValue())) {
					throw new IllegalStateException("Found registerConverters=false in scaffolded controller " + controller + ". " +
							"Remove this property from all controllers and let Spring ROO install the new application-wide ApplicationConversionServiceFactoryBean. " +
							"Then move your custom getXxxConverter() methods to it, delete the GenericConversionService field and the @PostContruct method.");
				}
			}
		}
		conversionServiceOperations.installConversionService(governor.getPackage());
	}

	private boolean hasMutator(FieldMetadata fieldMetadata, BeanInfoMetadata bim) {
		for (MethodMetadata mutator : bim.getPublicMutators()) {
			if (fieldMetadata.equals(bim.getFieldForPropertyName(BeanInfoUtils.getPropertyNameForJavaBeanMethod(mutator)))) return true;
		}
		return false;
	}

	private boolean isSpecialType(JavaType javaType) {
		// We are only interested if the type is part of our application
		if (metadataService.get(PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA)) != null) {
			return true;
		}
		return false;
	}

	public String getItdUniquenessFilenameSuffix() {
		return "Controller";
	}

	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = WebScaffoldMetadata.getJavaType(metadataIdentificationString);
		Path path = WebScaffoldMetadata.getPath(metadataIdentificationString);
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(javaType, path);
		return physicalTypeIdentifier;
	}

	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return WebScaffoldMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return WebScaffoldMetadata.getMetadataIdentiferType();
	}
}