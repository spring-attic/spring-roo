package org.springframework.roo.addon.entity;

import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.layers.CoreLayerProvider;
import org.springframework.roo.project.layers.LayerType;
import org.springframework.roo.project.layers.MemberTypeAdditions;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.Pair;
import org.springframework.roo.support.util.PairList;
import org.springframework.roo.support.util.StringUtils;

/**
 * The {@link org.springframework.roo.project.layers.LayerProvider} for the
 * {@link LayerType#ACTIVE_RECORD} layer.
 * 
 * @author Stefan Schmidt
 * @author Andrew Swan
 * @since 1.2
 */
@Component
@Service
public class EntityLayerProvider extends CoreLayerProvider {
	
	// Fields
	@Reference private EntityMetadataProvider entityMetadataProvider;
	@Reference private MetadataService metadataService;

	public MemberTypeAdditions getMemberTypeAdditions(final String callerMID, final String methodIdentifier, final JavaType targetEntity, final Pair<JavaType, JavaSymbolName>... methodParameters) {
		Assert.isTrue(StringUtils.hasText(callerMID), "Metadata identifier required");
		Assert.hasText(methodIdentifier, "Method identifier required");
		Assert.notNull(targetEntity, "Target enitity type required");
		
		// Get the values of this entity's @RooEntity annotation
		final EntityAnnotationValues annotationValues = entityMetadataProvider.getAnnotationValues(targetEntity);
		if (annotationValues == null) {
			return null;
		}
		
		// Check the entity has a plural form
		final String plural = getPlural(targetEntity);
		if (!StringUtils.hasText(plural)) {
			return null;
		}
		
		// Look for an entity layer method with this ID and parameter types
		final PairList<JavaType, JavaSymbolName> parameterList = new PairList<JavaType, JavaSymbolName>(methodParameters);
		final List<JavaType> parameterTypes = parameterList.getKeys();
		final EntityLayerMethod method = EntityLayerMethod.valueOf(methodIdentifier, parameterTypes, targetEntity);
		if (method == null) {
			return null;
		}
		
		// It's an entity layer method; see if it's specified by the annotation
		final String methodName = method.getName(annotationValues, targetEntity, plural);
		if (!StringUtils.hasText(methodName)) {
			return null;
		}
		
		// We have everything needed to generate a method call
		return new MemberTypeAdditions(null, methodName, method.getCall(annotationValues, targetEntity, plural, parameterList.getValues()));
	}
	
	/**
	 * Returns the plural form of the given entity
	 * 
	 * @param javaType the entity for which to get the plural (required)
	 * @return <code>null</code> if it can't be found or is actually <code>null</code>
	 */
	private String getPlural(final JavaType javaType) {
		final String key = PluralMetadata.createIdentifier(javaType);
		final PluralMetadata pluralMetadata = (PluralMetadata) metadataService.get(key);
		if (pluralMetadata == null) {
			// Can't acquire the plural
			return null;
		}
		return pluralMetadata.getPlural();
	}

	public int getLayerPosition() {
		return LayerType.ACTIVE_RECORD.getPosition();
	}
	
	/**
	 * For use by unit tests
	 * 
	 * @param entityMetadataProvider
	 */
	void setEntityMetadataProvider(final EntityMetadataProvider entityMetadataProvider) {
		this.entityMetadataProvider = entityMetadataProvider;
	}
	
	/**
	 * For use by unit tests
	 * 
	 * @param metadataService
	 */
	void setMetadataService(final MetadataService metadataService) {
		this.metadataService = metadataService;
	}
}
