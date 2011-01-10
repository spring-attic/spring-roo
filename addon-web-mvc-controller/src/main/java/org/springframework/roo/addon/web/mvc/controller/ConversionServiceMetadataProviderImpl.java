package org.springframework.roo.addon.web.mvc.controller;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.addon.beaninfo.BeanInfoUtils;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTriggerBasedMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.util.Assert;

/**
 * Metadata provider for {@link ConversionServiceMetadata}. Monitors
 * notifications for {@link RooConversionService} and {@link RooWebScaffold}
 * annotated types. Also listens for changes to the scaffolded domain types and
 * their associated domain types.
 * 
 * @author Rossen Stoyanchev
 * @author Stefan Schmidt
 * @since 1.1.1
 */
@Component(immediate = true)
@Service
public final class ConversionServiceMetadataProviderImpl extends AbstractItdMetadataProvider implements ItdTriggerBasedMetadataProvider {

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
		Set<JavaTypeMetadataHolder> rooJavaTypes = findDomainTypesRequiringAConverter();
		registerDependencies(rooJavaTypes, metadataIdentificationString);
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

	Set<JavaTypeMetadataHolder> findDomainTypesRequiringAConverter() {
		JavaType rooWebScaffold = new JavaType(RooWebScaffold.class.getName());
		Set<JavaTypeMetadataHolder> javaTypes = new HashSet<JavaTypeMetadataHolder>();
		for (JavaType controller : typeLocationService.findTypesWithAnnotation(rooWebScaffold)) {
			PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(controller, Path.SRC_MAIN_JAVA));
			Assert.notNull(physicalTypeMetadata, "Unable to obtain physical type metdata for type " + controller.getFullyQualifiedTypeName());
			WebScaffoldAnnotationValues webScaffoldAnnotationValues = new WebScaffoldAnnotationValues(physicalTypeMetadata);
			JavaTypeMetadataHolder formBackingType = getJavaTypeMdHolder(webScaffoldAnnotationValues.getFormBackingObject());
			if (formBackingType != null) {
				javaTypes.add(formBackingType);
				javaTypes.addAll(findRelatedDomainTypes(formBackingType));
			}
		}
		return javaTypes;
	}
	
	private JavaTypeMetadataHolder getJavaTypeMdHolder(JavaType javaType) {
		PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA));
		EntityMetadata entityMetadata = (EntityMetadata) metadataService.get(EntityMetadata.createIdentifier(javaType, Path.SRC_MAIN_JAVA));
		BeanInfoMetadata beanInfoMetadata = (BeanInfoMetadata) metadataService.get(BeanInfoMetadata.createIdentifier(javaType, Path.SRC_MAIN_JAVA));
		if (physicalTypeMetadata == null || beanInfoMetadata == null || physicalTypeMetadata == null) {
			return null;
		}
		MemberHoldingTypeDetails memberHoldingTypeDetails = physicalTypeMetadata.getMemberHoldingTypeDetails();
		return new JavaTypeMetadataHolder(javaType, beanInfoMetadata, entityMetadata, memberHoldingTypeDetails!= null ? memberHoldingTypeDetails.getPhysicalTypeCategory().equals(PhysicalTypeCategory.ENUMERATION) : false);
	}
	
	private void registerDependencies(Set<JavaTypeMetadataHolder> domainTypes, String metadataId) {
		metadataDependencyRegistry.registerDependency(WebScaffoldMetadata.getMetadataIdentiferType(), metadataId);
		for (JavaTypeMetadataHolder domainJavaType : domainTypes) {
			metadataDependencyRegistry.registerDependency(domainJavaType.getBeanInfoMetadata().getId(), metadataId);
			metadataDependencyRegistry.registerDependency(domainJavaType.getEntityMetadata().getId(), metadataId);
		}
	}

	private Set<JavaTypeMetadataHolder> findRelatedDomainTypes(JavaTypeMetadataHolder javaTypeInfo) {
		LinkedHashSet<JavaTypeMetadataHolder> relatedDomainTypes = new LinkedHashSet<JavaTypeMetadataHolder>();
		BeanInfoMetadata beanInfoMetadata = javaTypeInfo.getBeanInfoMetadata();
		EntityMetadata entityMetadata = javaTypeInfo.getEntityMetadata();
		for (MethodMetadata accessor : beanInfoMetadata.getPublicAccessors(false)) {
			// Not interested in identifiers and version fields
			if (accessor.equals(entityMetadata.getIdentifierAccessor()) || accessor.equals(entityMetadata.getVersionAccessor())) {
				continue;
			}
			// Not interested in fields that are not exposed via a mutator
			FieldMetadata fieldMetadata = beanInfoMetadata.getFieldForPropertyName(BeanInfoUtils.getPropertyNameForJavaBeanMethod(accessor));
			if (fieldMetadata == null || !hasMutator(fieldMetadata, beanInfoMetadata)) {
				continue;
			}
			JavaType type = accessor.getReturnType();
			if (type.isCommonCollectionType()) {
				for (JavaType genericType : type.getParameters()) {
					if (isApplicationType(genericType)) {
						JavaTypeMetadataHolder jtMd = getJavaTypeMdHolder(genericType);
						if (jtMd != null) {
							relatedDomainTypes.add(jtMd);
						}
					}
				}
			} else {
				if (isApplicationType(type) && (!isEmbeddedFieldType(fieldMetadata))) {
					JavaTypeMetadataHolder jtMd = getJavaTypeMdHolder(type);
					if (jtMd != null) {
						relatedDomainTypes.add(jtMd);
					}
				}
			}
		}
		return relatedDomainTypes;
	}

	private boolean isApplicationType(JavaType javaType) {
		return (metadataService.get(PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA)) != null);
	}

	private boolean isEmbeddedFieldType(FieldMetadata field) {
		return MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.persistence.Embedded")) != null;
	}

	private boolean hasMutator(FieldMetadata fieldMetadata, BeanInfoMetadata bim) {
		for (MethodMetadata mutator : bim.getPublicMutators()) {
			if (fieldMetadata.equals(bim.getFieldForPropertyName(BeanInfoUtils.getPropertyNameForJavaBeanMethod(mutator)))) {
				return true;
			}
		}
		return false;
	}
}