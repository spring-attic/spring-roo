package org.springframework.roo.project.layers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;

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
	
	// Constants
	private static final boolean DEBUG = false;
	
	// Fields
	private final Object mutex = this;
	private final Set<LayerProvider<?>> providers = new TreeSet<LayerProvider<?>>(new DescendingLayerComparator());
	
	/**
	 * Adds the given {@link LayerProvider}
	 * 
	 * @param provider the provider to add (required)
	 */
	protected void bindLayerProvider(final LayerProvider<?> provider) {
		synchronized (mutex) {
			final boolean added = providers.add(provider);
			if (added) {
				log("Added a LayerProvider of type " + provider.getClass());
			}
			else {
				log("Already had this " + provider.getClass());
			}
		}
	}

	/**
	 * Removes the given {@link LayerProvider}
	 * 
	 * @param provider the provider to remove
	 */
	protected void unbindLayerProvider(final LayerProvider<?> provider) {
		synchronized (mutex) {
			if (providers.contains(provider)) {
				log("Removed a LayerProvider of type " + provider.getClass());
				providers.remove(provider);
			}
		}
	}
	
	/**
	 * Logs the given message to the console
	 * 
	 * @param message can be blank
	 */
	private void log(final String message) {
		if (DEBUG) {
			System.out.println(">>>>>>>> " + getClass().getSimpleName() + ": " + message);
		}
	}
	
	public <M extends Enum<M>> MemberTypeAdditions getAdditions(final String metadataId, final JavaType targetEntity, final int layerPosition, final M method) {
		log(metadataId + " is checking " + providers.size() + " providers for the " + targetEntity.getSimpleTypeName() + "." + method + " method");
		for (final LayerProvider<?> provider : new ArrayList<LayerProvider<?>>(providers)) {
			if (provider.getLayerPosition() < layerPosition && provider.supports(method.getClass())) {
				log("Checking position " + provider.getLayerPosition() + " provider with priority " + provider.getPriority());
				// This provider is lower-level than the caller; see if it provides the requested method
				@SuppressWarnings("unchecked")
				final LayerProvider<M> typedProvider = (LayerProvider<M>) provider;
				final MemberTypeAdditions additions = typedProvider.getAdditions(metadataId, targetEntity, method);
				if (additions != null) {
					// This provider can provide the additions for this method
					return additions;
				}
			}
		}
		return null;
	}

	/**
	 * Sorts two {@link LayerProvider}s into descending order of position and
	 * priority.
	 *
	 * @author Andrew Swan
	 * @author Stefan Schmidt
	 * @since 1.2
	 */
	class DescendingLayerComparator implements Comparator<LayerProvider<?>>, Serializable {
		
		// Constants
		private static final long serialVersionUID = 1L;
		
		public int compare(final LayerProvider<?> provider1, final LayerProvider<?> provider2) {
			if (provider1.equals(provider2)) {
				return 0;
			}
			final int positionDifference = provider2.getLayerPosition() - provider1.getLayerPosition();
			if (positionDifference != 0) {
				return positionDifference;
			}
			final int priorityDifference = provider2.getPriority() - provider1.getPriority();
			Assert.state(priorityDifference != 0, provider1.getClass().getSimpleName() + " and " + provider2.getClass().getSimpleName() + " both have position " + provider1.getLayerPosition() + " and priority " + provider1.getPriority());
			return priorityDifference;
		}
	}

	public <M extends Enum<M>> Map<M, MemberTypeAdditions> getAdditions(final String metadataId, final JavaType targetEntity, final int layerPosition, final M... methods) {
		// Collect the additions in a linked HashMap to ensure a repeatable order
		final Map<M, MemberTypeAdditions> additions = new LinkedHashMap<M, MemberTypeAdditions>();
		for (final M method : methods) {
			additions.put(method, getAdditions(metadataId, targetEntity, layerPosition, method));
		}
		return additions;
	}

	public <M extends Enum<M>> Map<M, MemberTypeAdditions> getAllAdditions(final String metadataId, final JavaType targetEntity, final int layerPosition, final Class<M> methodType) {
		return getAdditions(metadataId, targetEntity, layerPosition, methodType.getEnumConstants());
	}
}
