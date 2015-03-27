package org.srpingframework.roo.osgi.gogo.commands;

import java.io.IOException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.subsystem.Subsystem;

/**
 * 
 * Adding Subsystem Gogo commands
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Component(immediate = true)
@Service
public class SubsystemCommands implements BundleActivator {
	private BundleContext bundleContext;
	
	// TODO: Improve Gogo commands ROO implementation using
	// apache felix @Command annotation
	
   	protected void activate(final ComponentContext context) {
    	try {
			start(context.getBundleContext());
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

	public void start(BundleContext context) throws Exception {
		bundleContext = context;
		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put("osgi.command.function", new String[] { "deploy", "install",
				"uninstall", "start", "stop", "list", });
		props.put("osgi.command.scope", "subsystem");
		context.registerService(getClass().getName(), this, props);
	}
	
	public void deploy(String url) throws IOException{
		// Install subsystem using url
		Subsystem subsystem = install(url);
		// Starting installed subsystem
		start(subsystem.getSubsystemId());
	}

	public Subsystem install(String url) throws IOException {
		System.out.println("Installing subsystem by URL: " + url);
		Subsystem rootSubsystem = getSubsystem(0);
		Subsystem s = rootSubsystem.install(url, new URL(url).openStream());
		System.out.println("Subsystem successfully installed: "
				+ s.getSymbolicName() + "; id: " + s.getSubsystemId());
		return s;
	}

	public void uninstall(long id) {
		System.out.println("Uninstalling subsystem: " + id);
		Subsystem subsystem = getSubsystem(id);
		subsystem.uninstall();
		System.out.println("Subsystem successfully uninstalled: "
				+ subsystem.getSymbolicName() + "; id: " + subsystem.getSubsystemId());
	}

	public void start(long id) {
		System.out.println("Starting subsystem: " + id);
		Subsystem subsystem = getSubsystem(id);
		subsystem.start();
		System.out.println("Subsystem successfully started: "
				+ subsystem.getSymbolicName() + "; id: " + subsystem.getSubsystemId());
	}

	public void stop(long id) {
		System.out.println("Stopping subsystem: " + id);
		Subsystem subsystem = getSubsystem(id);
		subsystem.stop();
		System.out.println("Subsystem successfully stopped: "
				+ subsystem.getSymbolicName() + "; id: " + subsystem.getSubsystemId());
		
	}

	public void list() throws InvalidSyntaxException {
		Map<Long, String> subsystems = new TreeMap<Long, String>();
		for (ServiceReference<Subsystem> ref : bundleContext
				.getServiceReferences(Subsystem.class, null)) {
			Subsystem s = bundleContext.getService(ref);
			if (s != null) {
				subsystems.put(
						s.getSubsystemId(),
						String.format("%d\t%s\t%s %s", s.getSubsystemId(),
								s.getState(), s.getSymbolicName(),
								s.getVersion()));
			}
		}
		for (String entry : subsystems.values()) {
			System.out.println(entry);
		}
	}

	private Subsystem getSubsystem(long id) {
		try {
			for (ServiceReference<Subsystem> ref : bundleContext
					.getServiceReferences(Subsystem.class, "(subsystem.id="
							+ id + ")")) {
				Subsystem svc = bundleContext.getService(ref);
				if (svc != null)
					return svc;
			}
		} catch (InvalidSyntaxException e) {
			throw new RuntimeException(e);
		}
		throw new RuntimeException("Unable to find subsystem " + id);
	}

	public void stop(BundleContext context) throws Exception {
	}
}