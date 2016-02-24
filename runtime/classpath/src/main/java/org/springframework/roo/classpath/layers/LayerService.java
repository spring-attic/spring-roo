package org.springframework.roo.classpath.layers;

import java.util.Collection;

import org.springframework.roo.model.JavaType;

/**
 * Provides upper-layer code (such as MVC, GWT, and tests) with the
 * {@link MemberTypeAdditions} they need to make to their source code in order
 * to invoke persistence-related operations such as <code>persist</code> and
 * <code>find</code>.
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
public interface LayerService {

    /**
     * Returns source code modifications for a requested operation offered by a
     * layer provider
     * 
     * @param metadataIdentificationString Id of calling metadata provider
     *            (required)
     * @param methodIdentifier specifies the method which is being requested
     *            (required)
     * @param targetEntity specifies the target entity (required)
     * @param idType specifies the ID type used by the target entity (required)
     * @param layerPosition the position of the layer invoking this method
     * @param methodParameters parameters passed in to the method (types and
     *            names), if any
     * @return {@link MemberTypeAdditions} if a layer provider can offer this
     *         functionality, <code>null</code> otherwise
     */
    MemberTypeAdditions getMemberTypeAdditions(
            String metadataIdentificationString, String methodIdentifier,
            JavaType targetEntity, JavaType idType, int layerPosition,
            Collection<? extends MethodParameter> methodParameters);

    /**
     * Returns source code modifications for a requested operation offered by a
     * layer provider
     * 
     * @param metadataIdentificationString Id of calling metadata provider
     *            (required)
     * @param methodIdentifier specifies the method which is being requested
     *            (required)
     * @param targetEntity specifies the target entity (required)
     * @param idType specifies the ID type used by the target entity (required)
     * @param layerPosition the position of the layer invoking this method
     * @param methodParameters parameters passed in to the method (types and
     *            names), if any
     * @return {@link MemberTypeAdditions} if a layer provider can offer this
     *         functionality, <code>null</code> otherwise
     */
    MemberTypeAdditions getMemberTypeAdditions(
            String metadataIdentificationString, String methodIdentifier,
            JavaType targetEntity, JavaType idType, int layerPosition,
            boolean autowire,
            Collection<? extends MethodParameter> methodParameters);

    /**
     * Returns source code modifications for a requested operation offered by a
     * layer provider
     * 
     * @param metadataIdentificationString Id of calling metadata provider
     *            (required)
     * @param methodIdentifier specifies the method which is being requested
     *            (required)
     * @param targetEntity specifies the target entity (required)
     * @param idType specifies the ID type used by the target entity (required)
     * @param layerPosition the position of the layer invoking this method
     * @param methodParameters parameters passed in to the method (types and
     *            names), if any
     * @return {@link MemberTypeAdditions} if a layer provider can offer this
     *         functionality, <code>null</code> otherwise
     */
    MemberTypeAdditions getMemberTypeAdditions(
            String metadataIdentificationString, String methodIdentifier,
            JavaType targetEntity, JavaType idType, int layerPosition,
            MethodParameter... methodParameters);

    /**
     * Returns source code modifications for a requested operation offered by a
     * layer provider
     * 
     * @param metadataIdentificationString Id of calling metadata provider
     *            (required)
     * @param methodIdentifier specifies the method which is being requested
     *            (required)
     * @param targetEntity specifies the target entity (required)
     * @param idType specifies the ID type used by the target entity (required)
     * @param layerPosition the position of the layer invoking this method
     * @param methodParameters parameters passed in to the method (types and
     *            names), if any
     * @return {@link MemberTypeAdditions} if a layer provider can offer this
     *         functionality, <code>null</code> otherwise
     */
    MemberTypeAdditions getMemberTypeAdditions(
            String metadataIdentificationString, String methodIdentifier,
            JavaType targetEntity, JavaType idType, int layerPosition,
            boolean autowire, MethodParameter... methodParameters);
}
