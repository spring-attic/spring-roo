package org.springframework.roo.addon.layers.service;

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
 * Locates interfaces annotated with {@link RooService} that meet certain
 * criteria.
 * 
 * Factored out of {@link ServiceLayerProvider} to simplify unit testing of that
 * class.
 *
 * @author Stefan Schmidt
 * @author Andrew Swan
 * @since 1.2
 */
@Component
@Service
public class ServiceInterfaceLocatorImpl implements ServiceInterfaceLocator, MetadataNotificationListener {
	
	// Constants
	private static final Map<JavaType, Collection<ClassOrInterfaceTypeDetails>> domainTypeToServiceMap = new HashMap<JavaType, Collection<ClassOrInterfaceTypeDetails>>();

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
		if (metadataItem instanceof ServiceInterfaceMetadata) {
			ServiceInterfaceMetadata serviceMetadata = (ServiceInterfaceMetadata) metadataItem;
			ItdTypeDetails serviceItd = serviceMetadata.getMemberHoldingTypeDetails();
			if (serviceItd == null) {
				return;
			}
			for (JavaType domainType : serviceMetadata.getServiceAnnotationValues().getDomainTypes()) {
				initMapValue(domainType);
				domainTypeToServiceMap.get(domainType).add(serviceItd.getGovernor());
			}
		}
	}
	
	public Collection<ClassOrInterfaceTypeDetails> getServiceInterfaces(JavaType domainType) {
		initMapValue(domainType);
		return domainTypeToServiceMap.get(domainType);
	}

	private void initMapValue(JavaType domainType) {
		if (!domainTypeToServiceMap.containsKey(domainType)) {
			domainTypeToServiceMap.put(domainType, new ArrayList<ClassOrInterfaceTypeDetails>());
		}
	}
}