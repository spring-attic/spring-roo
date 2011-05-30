package org.springframework.roo.layers;

import java.io.Serializable;
import java.util.Collections;
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

/**
 * 
 * @author Stefan Schmidt
 * @since 1.2
 * 
 */
@Component(immediate=true)
@Service
@Reference(name = "layerProvider", strategy = ReferenceStrategy.EVENT, policy = ReferencePolicy.DYNAMIC, referenceInterface = LayerProvider.class, cardinality = ReferenceCardinality.MANDATORY_MULTIPLE) 
public class LayerServiceImpl implements LayerService {
	private Object mutex = this;
	private Set<LayerProvider> providers = new TreeSet<LayerProvider>(new PersistenceProviderComparator());
	
	public MemberTypeAdditions integratePersistMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType) {
		for (LayerProvider provider : Collections.unmodifiableSet(providers)) {
			MemberTypeAdditions additions = provider.integratePersistMethod(declaredByMetadataId, entityVariableName, entityType);
			if (additions != null) {
				return additions;
			}
		}
		return null;
	}

	public MemberTypeAdditions integrateUpdateMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType) {
		for (LayerProvider provider : Collections.unmodifiableSet(providers)) {
			MemberTypeAdditions additions = provider.integrateUpdateMethod(declaredByMetadataId, entityVariableName, entityType);
			if (additions != null) {
				return additions;
			}
		}
		return null;
	}
	
	public MemberTypeAdditions integrateDeleteMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType) {
		for (LayerProvider provider : Collections.unmodifiableSet(providers)) {
			MemberTypeAdditions additions = provider.integrateDeleteMethod(declaredByMetadataId, entityVariableName, entityType);
			if (additions != null) {
				return additions;
			}
		}
		return null;
	}
	
	public MemberTypeAdditions integrateFindMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType) {
		for (LayerProvider provider : Collections.unmodifiableSet(providers)) {
			MemberTypeAdditions additions = provider.integrateFindMethod(declaredByMetadataId, entityVariableName, entityType);
			if (additions != null) {
				return additions;
			}
		}
		return null;
	}

	public MemberTypeAdditions integrateFindAllMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType) {
		for (LayerProvider provider : Collections.unmodifiableSet(providers)) {
			MemberTypeAdditions additions = provider.integrateFindAllMethod(declaredByMetadataId, entityVariableName, entityType);
			if (additions != null) {
				return additions;
			}
		}
		return null;
	}
	
	public MemberTypeAdditions integrateFinderMethod(String declaredByMetadataId, String finderName, JavaType entityType) {
		for (LayerProvider provider : Collections.unmodifiableSet(providers)) {
			MemberTypeAdditions additions = provider.integrateFinderMethod(declaredByMetadataId, finderName, entityType);
			if (additions != null) {
				return additions;
			}
		}
		return null;
	}
	
	public MemberTypeAdditions integrateFindEntriesMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType) {
		for (LayerProvider provider : Collections.unmodifiableSet(providers)) {
			MemberTypeAdditions additions = provider.integrateFindEntriesMethod(declaredByMetadataId, entityVariableName, entityType);
			if (additions != null) {
				return additions;
			}
		}
		return null;
	}

	public MemberTypeAdditions integrateCountMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType) {
		for (LayerProvider provider : Collections.unmodifiableSet(providers)) {
			MemberTypeAdditions additions = provider.integrateCountMethod(declaredByMetadataId, entityVariableName, entityType);
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

	class PersistenceProviderComparator implements Comparator<LayerProvider>, Serializable {
		private static final long serialVersionUID = 1L;
		public int compare(LayerProvider provider1, LayerProvider provider2) {
			if (provider1.equals(provider2)) {
				return 0;
			}
			int difference = provider1.getLayerType().getOrder() - provider2.getLayerType().getOrder();
			if (difference == 0) {
				throw new IllegalStateException(provider1.getClass().getSimpleName() + " and " + provider2.getClass().getSimpleName() + " both have order = " + provider1.getLayerType().getOrder());
			}
			return difference;
		}
	}
}
