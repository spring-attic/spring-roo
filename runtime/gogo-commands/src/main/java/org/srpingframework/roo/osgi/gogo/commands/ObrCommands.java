package org.srpingframework.roo.osgi.gogo.commands;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;

/**
 * 
 * Extending OBR gogo commands with start functionality
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Component(immediate = true)
@Service
public class ObrCommands implements BundleActivator {
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
    props.put("osgi.command.function", new String[] {"start"});
    props.put("osgi.command.scope", "obr");
    context.registerService(getClass().getName(), this, props);
  }

  public void start(String bundleSymbolicName) throws BundleException, InvalidSyntaxException {
    Bundle currentBundle = getBundle(bundleSymbolicName);
    System.out.println("Starting " + bundleSymbolicName + "; id: " + currentBundle.getBundleId()
        + " ...");
    currentBundle.start();
    System.out.println("Started!");
  }

  private Bundle getBundle(String bundleSymbolicName) throws InvalidSyntaxException {
    // Getting all bundles
    for (Bundle bundle : bundleContext.getBundles()) {
      if (bundle.getSymbolicName().equals(bundleSymbolicName)) {
        return bundle;
      }
    }
    throw new RuntimeException("Unable to find bundle " + bundleSymbolicName);
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    // TODO Auto-generated method stub

  }

}
