package org.springframework.roo.classpath.layers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaType;

/**
 * The {@link LayerService} implementation.
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
@Component
@Service
@Reference(name = "layerProvider", strategy = ReferenceStrategy.EVENT, policy = ReferencePolicy.DYNAMIC, referenceInterface = LayerProvider.class, cardinality = ReferenceCardinality.MANDATORY_MULTIPLE)
public class LayerServiceImpl implements LayerService {

    /**
     * Sorts two {@link LayerProvider}s into descending order of position.
     * 
     * @author Andrew Swan
     * @author Stefan Schmidt
     * @since 1.2.0
     */
    static class DescendingLayerComparator implements
            Comparator<LayerProvider>, Serializable {

        private static final long serialVersionUID = 2840103254559366403L;

        public int compare(final LayerProvider provider1,
                final LayerProvider provider2) {
            if (provider1.equals(provider2)) {
                return 0;
            }
            final int difference = provider2.getLayerPosition()
                    - provider1.getLayerPosition();
            Validate.validState(difference != 0, provider1.getClass()
                    .getSimpleName()
                    + " and "
                    + provider2.getClass().getSimpleName()
                    + " both have position " + provider1.getLayerPosition());
            return difference;
        }
    }

    // Mutex
    private final Object mutex = new Object();

    private final SortedSet<LayerProvider> providers = new TreeSet<LayerProvider>(
            new DescendingLayerComparator());

    protected void bindLayerProvider(final LayerProvider provider) {
        synchronized (mutex) {
            providers.add(provider);
        }
    }

    public MemberTypeAdditions getMemberTypeAdditions(
            final String metadataIdentificationString,
            final String methodIdentifier, final JavaType targetEntity,
            final JavaType idType, final int layerPosition,
            final Collection<? extends MethodParameter> methodParameters) {
        final MethodParameter[] methodParametersArray = methodParameters
                .toArray(new MethodParameter[methodParameters.size()]);
        return getMemberTypeAdditions(metadataIdentificationString,
                methodIdentifier, targetEntity, idType, layerPosition,
                methodParametersArray);
    }

    public MemberTypeAdditions getMemberTypeAdditions(
            final String metadataIdentificationString,
            final String methodIdentifier, final JavaType targetEntity,
            final JavaType idType, final int layerPosition, boolean autowire,
            final Collection<? extends MethodParameter> methodParameters) {
        final MethodParameter[] methodParametersArray = methodParameters
                .toArray(new MethodParameter[methodParameters.size()]);
        return getMemberTypeAdditions(metadataIdentificationString,
                methodIdentifier, targetEntity, idType, layerPosition,
                autowire, methodParametersArray);
    }

    public MemberTypeAdditions getMemberTypeAdditions(
            final String metadataIdentificationString,
            final String methodIdentifier, final JavaType targetEntity,
            final JavaType idType, final int layerPosition,
            final MethodParameter... methodParameters) {
        return getMemberTypeAdditions(metadataIdentificationString,
                methodIdentifier, targetEntity, idType, layerPosition, true,
                methodParameters);
    }

    public MemberTypeAdditions getMemberTypeAdditions(
            final String metadataIdentificationString,
            final String methodIdentifier, final JavaType targetEntity,
            final JavaType idType, final int layerPosition,
            final boolean autowire, final MethodParameter... methodParameters) {
        Validate.notBlank(metadataIdentificationString,
                "metadataIdentificationString is required");
        Validate.notBlank(methodIdentifier, "methodIdentifier is required");
        Validate.notNull(targetEntity, "targetEntity is required");
        for (final LayerProvider provider : new ArrayList<LayerProvider>(
                providers)) {
            if (provider.getLayerPosition() >= layerPosition) {
                continue;
            }
            final MemberTypeAdditions additions = provider
                    .getMemberTypeAdditions(metadataIdentificationString,
                            methodIdentifier, targetEntity, idType, autowire,
                            methodParameters);
            if (additions != null) {
                return additions;
            }
        }
        return null;
    }

    protected void unbindLayerProvider(final LayerProvider provider) {
        synchronized (mutex) {
            if (providers.contains(provider)) {
                providers.remove(provider);
            }
        }
    }
}
