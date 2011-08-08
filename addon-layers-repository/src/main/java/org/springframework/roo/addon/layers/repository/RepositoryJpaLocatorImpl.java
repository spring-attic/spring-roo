package org.springframework.roo.addon.layers.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataItem;
import org.springframework.roo.metadata.MetadataNotificationListener;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;

/**
 * The {@link RepositoryJpaLocator} implementation.
 *
 * @author Stefan Schmidt
 * @author Andrew Swan
 * @since 1.2
 */
@Component
@Service
public class RepositoryJpaLocatorImpl implements RepositoryJpaLocator, MetadataNotificationListener {
	
	// Constants
	private static final Map<JavaType, Collection<ClassOrInterfaceTypeDetails>> domainTypeToRepoMap = new HashMap<JavaType, Collection<ClassOrInterfaceTypeDetails>>();

	// Fields
	@Reference private MetadataService metadataService;
	@Reference private MetadataDependencyRegistry metadataDependencyRegistry;
	
	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.addNotificationListener(this);
	}
	
	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.removeNotificationListener(this);
	}

	public void notify(String upstreamDependency, String downstreamDependency) {
		MetadataItem metadataItem = metadataService.get(upstreamDependency);
		if (metadataItem == null) {
			return;
		}
		if (metadataItem instanceof RepositoryJpaMetadata) {
			RepositoryJpaMetadata repositoryJpaMetadata = (RepositoryJpaMetadata) metadataItem;
			ItdTypeDetails repoItd = repositoryJpaMetadata.getMemberHoldingTypeDetails();
			if (repoItd == null) {
				return;
			}
			JavaType domainType = repositoryJpaMetadata.getAnnotationValues().getDomainType();
			initMapValue(domainType);
			domainTypeToRepoMap.get(domainType).add(repoItd.getGovernor());
		}
	}

	public Collection<ClassOrInterfaceTypeDetails> getRepositories(final JavaType domainType) {
		initMapValue(domainType);
		return domainTypeToRepoMap.get(domainType);
	}
	
	private void initMapValue(JavaType domainType) {
		if (!domainTypeToRepoMap.containsKey(domainType)) {
			domainTypeToRepoMap.put(domainType, new ArrayList<ClassOrInterfaceTypeDetails>());
		}
	}
}
