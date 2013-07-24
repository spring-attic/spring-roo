package org.springframework.roo.addon.jpa.activerecord;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.ImportMetadataBuilder;
import org.springframework.roo.classpath.layers.CoreLayerProvider;
import org.springframework.roo.classpath.layers.LayerType;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.classpath.layers.MethodParameter;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.PairList;

/**
 * The {@link org.springframework.roo.classpath.layers.LayerProvider} for the
 * {@link LayerType#ACTIVE_RECORD} layer.
 * 
 * @author Stefan Schmidt
 * @author Andrew Swan
 * @since 1.2.0
 */
@Component
@Service
public class EntityLayerProvider extends CoreLayerProvider {

    @Reference private JpaActiveRecordMetadataProvider jpaActiveRecordMetadataProvider;
    @Reference private MetadataService metadataService;
    @Reference TypeLocationService typeLocationService;

    public int getLayerPosition() {
        return LayerType.ACTIVE_RECORD.getPosition();
    }

    public MemberTypeAdditions getMemberTypeAdditions(final String callerMID,
            final String methodIdentifier, final JavaType targetEntity,
            final JavaType idType, final MethodParameter... callerParameters) {

        return getMemberTypeAdditions(callerMID, methodIdentifier,
                targetEntity, idType, true, callerParameters);
    }

    public MemberTypeAdditions getMemberTypeAdditions(final String callerMID,
            final String methodIdentifier, final JavaType targetEntity,
            final JavaType idType, boolean autowire,
            final MethodParameter... callerParameters) {
        Validate.isTrue(StringUtils.isNotBlank(callerMID),
                "Metadata identifier required");
        Validate.notBlank(methodIdentifier, "Method identifier required");
        Validate.notNull(targetEntity, "Target enitity type required");

        // Get the CRUD-related values of this entity's @RooJpaActiveRecord
        // annotation
        final JpaCrudAnnotationValues annotationValues = jpaActiveRecordMetadataProvider
                .getAnnotationValues(targetEntity);
        if (annotationValues == null) {
            return null;
        }

        // Check the entity has a plural form
        final String plural = getPlural(targetEntity);
        if (StringUtils.isBlank(plural)) {
            return null;
        }

        // Look for an entity layer method with this ID and types of parameter
        final List<JavaType> parameterTypes = new PairList<JavaType, JavaSymbolName>(
                callerParameters).getKeys();
        final EntityLayerMethod method = EntityLayerMethod.valueOf(
                methodIdentifier, parameterTypes, targetEntity, idType);
        if (method == null) {
            return null;
        }

        // It's an entity layer method; see if it's specified by the annotation
        final String methodName = method.getName(annotationValues,
                targetEntity, plural);
        if (StringUtils.isBlank(methodName)) {
            return null;
        }

        // We have everything needed to generate a method call
        final List<MethodParameter> callerParameterList = Arrays
                .asList(callerParameters);
        final String methodCall = method.getCall(annotationValues,
                targetEntity, plural, callerParameterList);
        final ClassOrInterfaceTypeDetailsBuilder additionsBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                callerMID);
        if (method.isStatic()) {
            additionsBuilder.add(ImportMetadataBuilder.getImport(callerMID,
                    targetEntity));
        }
        return new MemberTypeAdditions(additionsBuilder, methodName,
                methodCall, method.isStatic(),
                method.getParameters(callerParameterList));
    }

    /**
     * Returns the plural form of the given entity
     * 
     * @param javaType the entity for which to get the plural (required)
     * @return <code>null</code> if it can't be found or is actually
     *         <code>null</code>
     */
    private String getPlural(final JavaType javaType) {
        final String key = PluralMetadata.createIdentifier(javaType,
                typeLocationService.getTypePath(javaType));
        final PluralMetadata pluralMetadata = (PluralMetadata) metadataService
                .get(key);
        if (pluralMetadata == null) {
            // Can't acquire the plural
            return null;
        }
        return pluralMetadata.getPlural();
    }

    /**
     * For use by unit tests
     * 
     * @param jpaActiveRecordMetadataProvider
     */
    void setJpaActiveRecordMetadataProvider(
            final JpaActiveRecordMetadataProvider jpaActiveRecordMetadataProvider) {
        this.jpaActiveRecordMetadataProvider = jpaActiveRecordMetadataProvider;
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
