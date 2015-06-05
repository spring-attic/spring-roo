package org.springframework.roo.web.ui;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.web.ui.controllers.EntitiesServlet;
import org.springframework.roo.web.ui.controllers.FieldsServlet;
import org.springframework.roo.web.ui.controllers.PersistenceServlet;
import org.springframework.roo.web.ui.controllers.ProjectServlet;
import org.springframework.roo.web.ui.controllers.RedirectServlet;

/**
 * 
 * This class registers all available servlets related
 * with Spring Roo Web Tool.
 * 
 * Provides Spring Roo Web Tool UI static resources.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Component(immediate = true)
@Service
public class ServletRegister implements BundleActivator {

	private ServiceTracker httpTracker;
	private BundleContext context;
	
	private static final Logger LOGGER = HandlerUtils
			.getLogger(ServletRegister.class);

	protected void activate(final ComponentContext cContext) {
		context = cContext.getBundleContext();
		try {
			start(context);
			LOGGER.log(Level.INFO, "Spring Roo Web UI started at 'http://localhost:9191/'");
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Unabled to connect to 'http://localhost:9191/'");
		}
	}

	public void start(BundleContext context) throws Exception {
		httpTracker = new ServiceTracker(context, HttpService.class.getName(),
				null) {
			public void removedService(ServiceReference reference,
					Object service) {
				// HTTP service is no longer available, unregister our
				// servlet...
				try {
					((HttpService) service).unregister("/");
					((HttpService) service).unregister("/spring-roo");
					((HttpService) service).unregister("/spring-roo/project");
					((HttpService) service).unregister("/spring-roo/persistence");
					((HttpService) service).unregister("/spring-roo/entities");
					((HttpService) service).unregister("/spring-roo/fields");
					
					//((HttpService) service).unregister("/spring-roo/shell");
					
				} catch (IllegalArgumentException exception) {
					// Ignore; servlet registration probably failed earlier
					// on...
				}
			}

			public Object addingService(ServiceReference reference) {
				// HTTP service is available, register our servlet...
				HttpService httpService = (HttpService) this.context
						.getService(reference);
				try {
					httpService.registerServlet("/", new RedirectServlet(), null, null);
					httpService.registerResources("/spring-roo", "ui", null);
					httpService.registerServlet("/spring-roo/project", new ProjectServlet(context), null, null);
					httpService.registerServlet("/spring-roo/persistence", new PersistenceServlet(context), null, null);
					httpService.registerServlet("/spring-roo/entities", new EntitiesServlet(context), null, null);
					httpService.registerServlet("/spring-roo/fields", new FieldsServlet(context), null, null);
					
					//httpService.registerServlet("/spring-roo/shell", new ShellServlet(context), null, null);
				} catch (Exception exception) {
					exception.printStackTrace();
				}
				return httpService;
			}
		};
		// start tracking all HTTP services...
		httpTracker.open();
	}

	public void stop(BundleContext context) throws Exception {
		// stop tracking all HTTP services...
		httpTracker.close();
	}
}