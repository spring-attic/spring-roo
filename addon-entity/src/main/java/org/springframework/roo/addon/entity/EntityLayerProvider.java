package org.springframework.roo.addon.entity;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.layers.CoreLayerProvider;
import org.springframework.roo.project.layers.LayerType;
import org.springframework.roo.project.layers.MemberTypeAdditions;
import org.springframework.roo.support.util.Pair;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.uaa.client.util.Assert;

/**
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
@Component
@Service
public class EntityLayerProvider extends CoreLayerProvider {
	
	// Constants
	private static final Path PATH = Path.SRC_MAIN_JAVA;
	private static final JavaType ROO_ENTITY = new JavaType(RooEntity.class.getName());
	
	// Fields
	@Reference private MetadataService metadataService;

	public MemberTypeAdditions getMemberTypeAdditions(String metadataId, String methodIdentifier, JavaType targetEntity, Pair<JavaType, JavaSymbolName>... methodParameters) {
		Assert.isTrue(StringUtils.hasText(metadataId), "Metadata identifier required");
		Assert.notNull(methodIdentifier, "Method identifier required");
		Assert.notNull(targetEntity, "Target enitity type required");
		
		EntityAnnotationValues rooEntityAnnotation = getRooEntityAnnotationValues(targetEntity);
		if (rooEntityAnnotation == null) {
			return null;
		}
		
		if (methodIdentifier.equals(PersistenceCustomDataKeys.FIND_ALL_METHOD.name())) {
			return getFindAllMethod(metadataId, targetEntity, rooEntityAnnotation);
		} else if (methodIdentifier.equals(PersistenceCustomDataKeys.PERSIST_METHOD.name())) {
			return getPersistMethod(metadataId, targetEntity, rooEntityAnnotation, methodParameters);
		} else if (methodIdentifier.equals(PersistenceCustomDataKeys.MERGE_METHOD.name())) {
			return getMergeMethod(metadataId, targetEntity, rooEntityAnnotation, methodParameters);
		} else if (methodIdentifier.equals(PersistenceCustomDataKeys.REMOVE_METHOD.name())) {
			return getRemoveMethod(metadataId, targetEntity, rooEntityAnnotation, methodParameters);
		}
		return null;
	}

	private MemberTypeAdditions getFindAllMethod(String metadataId, JavaType entityType, EntityAnnotationValues rooEntityAnnotation) {
		String plural = getPlural(entityType);
		if (!StringUtils.hasText(rooEntityAnnotation.getFindAllMethod()) || plural == null) {
			return null;
		}
		return MemberTypeAdditions.getInstance(new ClassOrInterfaceTypeDetailsBuilder(metadataId), entityType.getFullyQualifiedTypeName(), rooEntityAnnotation.getFindAllMethod() + plural);
	}
	
	private MemberTypeAdditions getPersistMethod(String metadataId, JavaType entityType, EntityAnnotationValues rooEntityAnnotation, Pair<JavaType, JavaSymbolName>... methodParameters) {
		if (!StringUtils.hasText(rooEntityAnnotation.getPersistMethod()) || methodParameters == null || methodParameters.length != 1 || !methodParameters[0].getKey().equals(entityType)) {
			return null;
		}
		return MemberTypeAdditions.getInstance(new ClassOrInterfaceTypeDetailsBuilder(metadataId), methodParameters[0].getValue().getSymbolName(), rooEntityAnnotation.getPersistMethod());
	}
	
	private MemberTypeAdditions getMergeMethod(String metadataId, JavaType entityType, EntityAnnotationValues rooEntityAnnotation, Pair<JavaType, JavaSymbolName>... methodParameters) {
		if (!StringUtils.hasText(rooEntityAnnotation.getMergeMethod()) || methodParameters == null || methodParameters.length != 1 || !methodParameters[0].getKey().equals(entityType)) {
			return null;
		}
		return MemberTypeAdditions.getInstance(new ClassOrInterfaceTypeDetailsBuilder(metadataId), methodParameters[0].getValue().getSymbolName(), rooEntityAnnotation.getMergeMethod());
	}
	
	private MemberTypeAdditions getRemoveMethod(String metadataId, JavaType entityType, EntityAnnotationValues rooEntityAnnotation, Pair<JavaType, JavaSymbolName>... methodParameters) {
		if (!StringUtils.hasText(rooEntityAnnotation.getRemoveMethod()) || methodParameters == null || methodParameters.length != 1 || !methodParameters[0].getKey().equals(entityType)) {
			return null;
		}
		return MemberTypeAdditions.getInstance(new ClassOrInterfaceTypeDetailsBuilder(metadataId), methodParameters[0].getValue().getSymbolName(), rooEntityAnnotation.getRemoveMethod());
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
