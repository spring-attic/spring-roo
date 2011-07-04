package org.springframework.roo.project.layers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.Pair;

/**
 * The {@link LayerService} implementation.
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
@Component(immediate=true)
@Service
@Reference(name = "layerProvider", strategy = ReferenceStrategy.EVENT, policy = ReferencePolicy.DYNAMIC, referenceInterface = LayerProvider.class, cardinality = ReferenceCardinality.MANDATORY_MULTIPLE) 
public class LayerServiceImpl implements LayerService {
	
	private final Object mutex = this;

	private final Set<LayerProvider> providers = new TreeSet<LayerProvider>(new DescendingLayerComparator());
	
	public MemberTypeAdditions getMemberTypeAdditions(final String metadataId, final String methodIdentifier, final JavaType targetEntity, final int layerPosition,	final Pair<JavaType, JavaSymbolName>... methodParameters) {
		Assert.hasText(metadataId, "metadataId is required");
		Assert.hasText(methodIdentifier, "methodIdentifier is required");
		Assert.notNull(targetEntity, "targetEntity is required");
		for (LayerProvider provider : new ArrayList<LayerProvider>(providers)) {
			if (provider.getLayerPosition() >= layerPosition) {
				continue;
			}
			MemberTypeAdditions additions = provider.getMemberTypeAdditions(metadataId, methodIdentifier, targetEntity, methodParameters);
			if (additions != null) {
				return additions;
			}
		}
		return null;
	}

	protected void bindLayerProvider(LayerProvider provider) {
		synchronized (mutex) {
			providers.add(provider);
		}
	}

	protected void unbindLayerProvider(LayerProvider provider) {
		synchronized (mutex) {
			if (providers.contains(provider)) {
				providers.remove(provider);
			}
		}
	}

	/**
	 * Sorts two {@link LayerProvider}s into descending order of position.
	 *
	 * @author Andrew Swan
	 * @author Stefan Schmidt
	 * @since 1.2
	 */
	class DescendingLayerComparator implements Comparator<LayerProvider>, Serializable {
		
		private static final long serialVersionUID = 1L;
		
		public int compare(final LayerProvider provider1, final LayerProvider provider2) {
			if (provider1.equals(provider2)) {
				return 0;
			}
			final int difference = provider2.getLayerPosition() - provider1.getLayerPosition();
			Assert.state(difference != 0, provider1.getClass().getSimpleName() + " and " + provider2.getClass().getSimpleName() + " both have position " + provider1.getLayerPosition());
			return difference;
		}
	}
}
