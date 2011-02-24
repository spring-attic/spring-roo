package org.springframework.roo.uaa;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.springframework.uaa.client.ProxyService;
import org.springframework.uaa.client.UaaDetectedProducts;
import org.springframework.uaa.client.UaaService;
import org.springframework.uaa.client.UaaServiceFactory;

/**
 * Registers various UAA-provided instances as OSGi components. This uses the UAA factories and conventions,
 * plus simplifies replacement by STS.
 * 
 * @author Ben Alex
 * @since 1.1.1
 *
 */
@Component
public class UaaRelatedComponentRegistrationHelper {

	private Set<ServiceRegistration> registrations = new HashSet<ServiceRegistration>();
	
	protected void activate(ComponentContext context) {
		registrations.add(context.getBundleContext().registerService(UaaService.class.getName(), UaaServiceFactory.getUaaService(), new Hashtable<Object, Object>()));
		registrations.add(context.getBundleContext().registerService(UaaDetectedProducts.class.getName(), UaaServiceFactory.getUaaDetectedProducts(), new Hashtable<Object, Object>()));
		registrations.add(context.getBundleContext().registerService(ProxyService.class.getName(), UaaServiceFactory.getProxyService(), new Hashtable<Object, Object>()));
	}
	
	protected void deactivate(ComponentContext context) {
		for (ServiceRegistration registration : registrations) {
			registration.unregister();
		}
	}
}
