package org.springframework.roo.addon.entity;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.layers.CoreLayerProvider;
import org.springframework.roo.project.layers.LayerType;
import org.springframework.roo.project.layers.MemberTypeAdditions;
import org.springframework.roo.project.layers.PersistenceMethod;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.uaa.client.util.Assert;

/**
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
@Component
@Service
public class EntityLayerProvider extends CoreLayerProvider<PersistenceMethod> {
	
	// Constants
	private static final Path PATH = Path.SRC_MAIN_JAVA;
	private static final JavaType ROO_ENTITY = new JavaType(RooEntity.class.getName());
	
	// Fields
	@Reference private MetadataService metadataService;
	
	public boolean supports(final Class<?> methodType) {
		return PersistenceMethod.class.equals(methodType);
	}
	
	public MemberTypeAdditions getAdditions(final String metadataId, final JavaType targetEntity, final PersistenceMethod method) {
		Assert.isTrue(StringUtils.hasText(metadataId), "Metadata identifier required");
		Assert.notNull(method, "Method required");
		Assert.notNull(targetEntity, "Target enitity type required");
		switch (method) {
			case FIND_ALL:
				return getFindAllMethod(metadataId, targetEntity);
			default:
				return null;	// TODO
		}
	}

	private MemberTypeAdditions getFindAllMethod(String metadataId, JavaType entityType) {
		EntityAnnotationValues rooEntityAnnotation = getRooEntityAnnotationValues(entityType);
		if (rooEntityAnnotation == null) {
			return null;
		}
		String plural = getPlural(entityType);
		if (rooEntityAnnotation == null || !StringUtils.hasText(rooEntityAnnotation.getFindAllMethod()) || plural == null) {
			return null;
		}
		return new MemberTypeAdditions(new ClassOrInterfaceTypeDetailsBuilder(metadataId), entityType.getFullyQualifiedTypeName() + "." + rooEntityAnnotation.getFindAllMethod() + plural + "()");
	}
	
	private EntityAnnotationValues getRooEntityAnnotationValues(JavaType javaType) {
		Assert.notNull(javaType, "JavaType required");
		PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA));
		if (physicalTypeMetadata == null || physicalTypeMetadata.getMemberHoldingTypeDetails() == null || MemberFindingUtils.getAnnotationOfType(physicalTypeMetadata.getMemberHoldingTypeDetails().getAnnotations(), ROO_ENTITY) == null) {
			return null;
		}
		return new EntityAnnotationValues(physicalTypeMetadata);
	}
	
	private String getPlural(JavaType javaType) {
		String key = PluralMetadata.createIdentifier(javaType, PATH);
		PluralMetadata pluralMetadata = (PluralMetadata) metadataService.get(key);
		if (pluralMetadata == null) {
			// Can't acquire the plural
			return null;
		}
		return pluralMetadata.getPlural();
	}

	public int getLayerPosition() {
		return LayerType.ACTIVE_RECORD.getPosition();
	}
}
