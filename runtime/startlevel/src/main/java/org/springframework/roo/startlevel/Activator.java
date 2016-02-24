package org.springframework.roo.startlevel;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.IOUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.startlevel.StartLevel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Changes the OSGi framework to start level 99 once all the services marked
 * with "immediate" are activated.
 * <p>
 * The OSGi Declarative Service Specification ensures services are only loaded
 * when required. While the "immediate" attribute ensures they are loaded
 * eagerly, it is impossible to receive a callback when all "immediate" services
 * have finished loading. This {@link BundleActivator} resolves this issue by
 * discovering all enabled service components with an "immediate" flag and
 * monitoring their startup. Once all are started, the OSGi framework is set to
 * start level 99, which enables other classes that depend on the activation of
 * such services to react to the start level change.
 * <p>
 * Note that this functionality is only provided for services (simple components
 * are insufficient). Services must be defined in the XML file indicated by the
 * "Service-Component" manifest header.
 * 
 * @author Ben Alex
 */
public class Activator implements BundleActivator {

    /** key: required class, any one of its services interfaces */
    private final SortedMap<String, String> requiredImplementations = new TreeMap<String, String>();
    private final SortedSet<String> runningImplementations = new TreeSet<String>();
    private StartLevel startLevel;
    private ServiceReference startLevelServiceReference;

    private String getClassName(final ServiceReference sr,
            final BundleContext context) {
        if (sr == null) {
            return null;
        }
        if (sr.getProperty("component.name") != null) {
            // Roo's convention is the component name should be the fully
            // qualified class name.
            // Roo's other convention is bundle symbolic names should be fully
            // qualified package names.
            // However, the user can change the BSN or component name, so we
            // need to do a quick sanity check.
            final String componentName = sr.getProperty("component.name")
                    .toString();
            if (componentName.startsWith(sr.getBundle().getSymbolicName())) {
                // The type name appears under the BSN package, so they probably
                // haven't changed our convention
                return componentName;
            }
        }

        // To get here we couldn't rely on component name. The following is far
        // less reliable given the
        // service may be unavailable by the time we try to do a getService(sr)
        // invocation (ROO-1156).
        final Object obj = context.getService(sr);
        if (obj == null) {
            return null;
        }
        return obj.getClass().getName();
    }

    private void potentiallyChangeStartLevel() {
        if (requiredImplementations.keySet().equals(runningImplementations)) {
            if (System.getProperty("roo.pause") != null) {
                System.out
                        .println("roo.pause detected; press any key to proceed");
                try {
                    System.in.read();
                }
                catch (final IOException ignored) {
                }
            }
            startLevel.setStartLevel(99);
        }
    }

    public void process(final URL url) {
        Document document;
        InputStream is = null;
        try {
            is = url.openStream();
            document = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder().parse(is);
        }
        catch (final Exception ex) {
            throw new IllegalStateException("Could not open " + url, ex);
        }
        finally {
            IOUtils.closeQuietly(is);
        }

        final Element rootElement = (Element) document.getFirstChild();
        final NodeList components = rootElement
                .getElementsByTagName("scr:component");
        if (components == null || components.getLength() == 0) {
            return;
        }

        for (int i = 0; i < components.getLength(); i++) {
            final Element component = (Element) components.item(i);

            // Is this component enabled?
            if (!component.hasAttribute("enabled")
                    || !"true".equals(component.getAttribute("enabled"))) {
                // Disabled, so skip it
                continue;
            }

            // Is this an immediate starter?
            if (!component.hasAttribute("immediate")
                    || !"true".equals(component.getAttribute("immediate"))) {
                // Not an immediate starter, so skip it
                continue;
            }

            // Calculate implementing class name correctly
            String componentName = null;
            final NodeList implementation = component
                    .getElementsByTagName("implementation");
            if (implementation != null && implementation.getLength() == 1) {
                final Element impl = (Element) implementation.item(0);
                if (impl.hasAttribute("class")) {
                    componentName = impl.getAttribute("class");
                }
            }

            // Get its first implementing service
            String serviceInterface = null;
            final NodeList service = component.getElementsByTagName("service");
            if (service != null && service.getLength() == 1) {
                final Element s = (Element) service.item(0);
                final NodeList provide = s.getElementsByTagName("provide");
                if (provide != null && provide.getLength() > 0) {
                    final Element firstProvide = (Element) provide.item(0);
                    if (firstProvide.hasAttribute("interface")) {
                        serviceInterface = firstProvide
                                .getAttribute("interface");
                    }
                }
            }

            if (componentName != null && serviceInterface != null) {
                requiredImplementations.put(componentName, serviceInterface);
            }
        }
    }

    public void start(final BundleContext context) throws Exception {
        startLevelServiceReference = context
                .getServiceReference(StartLevel.class.getName());
        startLevel = (StartLevel) context
                .getService(startLevelServiceReference);
        for (final Bundle bundle : context.getBundles()) {
            final String value = bundle.getHeaders().get("Service-Component");
            if (value != null) {
                List<String> componentDescriptions = Arrays.asList(value.split("\\s*,\\s*"));
                for (String desc : componentDescriptions) {
                    final URL url = bundle.getResource(desc);
                    process(url);
                }
            }
        }

        // Ensure I'm notified of other services changes
        final BundleContext myContext = context;
        context.addServiceListener(new ServiceListener() {
            public void serviceChanged(final ServiceEvent event) {
                final ServiceReference sr = event.getServiceReference();
                final String className = getClassName(sr, myContext);
                if (sr != null) {
                    myContext.ungetService(sr);
                }
                if (className == null) {
                    // Something went wrong
                    return;
                }
                if (event.getType() == ServiceEvent.REGISTERED) {
                    if (requiredImplementations.keySet().contains(className)) {
                        runningImplementations.add(className);
                        potentiallyChangeStartLevel();
                    }
                }
                else if (event.getType() == ServiceEvent.UNREGISTERING) {
                    if (runningImplementations.contains(className)) {
                        runningImplementations.remove(className);
                        potentiallyChangeStartLevel();
                    }
                }
            }
        });

        // Now identify if any services I was interested in are already running
        for (final String requiredService : requiredImplementations.keySet()) {
            final String correspondingInterface = requiredImplementations
                    .get(requiredService);
            final ServiceReference[] srs = context.getServiceReferences(
                    correspondingInterface, null);
            if (srs != null) {
                for (final ServiceReference sr : srs) {
                    final String className = getClassName(sr, context);
                    if (className == null) {
                        // Something went wrong
                        continue;
                    }
                    if (requiredImplementations.keySet().contains(className)) {
                        runningImplementations.add(className);
                    }
                }
            }
        }

        // Potentially change the start level, now that we've added all the
        // known started services
        potentiallyChangeStartLevel();
    }

    public void stop(final BundleContext context) throws Exception {
        context.ungetService(startLevelServiceReference);
    }
}
