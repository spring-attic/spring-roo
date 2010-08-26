package org.springframework.roo.addon.jdbc.polling.internal;

import java.sql.Driver;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.obr.Capability;
import org.osgi.service.obr.Resource;
import org.springframework.roo.addon.jdbc.JdbcDriverManager;
import org.springframework.roo.addon.jdbc.polling.JdbcDriverProvider;
import org.springframework.roo.obr.AddOnFinder;
import org.springframework.roo.obr.AddOnSearchManager;
import org.springframework.roo.obr.ObrResourceFinder;
import org.springframework.roo.support.util.Assert;

/**
 * Polls all OSGi-located {@link JdbcDriverProvider} instances and returns the first JDBC
 * driver provided by an instance.
 *
 * <p>
 * Failing the location of a suitable {@link JdbcDriverProvider}, automatically suggests an add-on which
 * may be able to provide the driver as a console message.
 *
 * @author Alan Stewart
 * @since 1.1
 */
@Component
@Service
@Reference(name = "jdbcDriverProvider", strategy = ReferenceStrategy.EVENT, policy = ReferencePolicy.DYNAMIC, referenceInterface = JdbcDriverProvider.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE)
public class PollingJdbcDriverManager implements JdbcDriverManager, AddOnFinder {
	private Set<JdbcDriverProvider> providers = new HashSet<JdbcDriverProvider>();
	@Reference private ObrResourceFinder obrResourceFinder;
	@Reference private AddOnSearchManager addOnSearchManager;
	
	protected void bindJdbcDriverProvider(JdbcDriverProvider listener) {
		synchronized (providers) {
			providers.add(listener);
		}
	}

	protected void unbindJdbcDriverProvider(JdbcDriverProvider listener) {
		synchronized (providers) {
			providers.remove(listener);
		}
	}

	public Driver loadDriver(String driverClassName) throws RuntimeException {
		Assert.hasText(driverClassName, "Driver class name required");
		synchronized (providers) {
			for (JdbcDriverProvider provider : providers) {
				Driver driver = provider.loadDriver(driverClassName);
				if (driver != null) {
					return driver;
				}
			}
			// No implementation could provide it
			addOnSearchManager.completeAddOnSearch(driverClassName, this);
			return null;
		}
	}

	public SortedMap<String, String> findAddOnsOffering(String driverClassName) {
		SortedMap<String, String> result = new TreeMap<String, String>();
		List<Resource> resources = obrResourceFinder.getKnownResources();
		if (resources != null) {
			for (Resource resource : resources) {
				outer: for (Capability capability : resource.getCapabilities()) {
					if ("package".equals(capability.getName())) {
						Map<?, ?> props = capability.getProperties();
						Object v = props.get("package");
						if (v != null) {
							String vString = v.toString();
							if (driverClassName.startsWith(vString)) {
								// Found the JDBC driver
								result.put(resource.getSymbolicName(), resource.getPresentationName());
								break outer;
							}
						}
					}
				}
			}
		}
		return result;
	}

	public String getFinderTargetSingular() {
		return "JDBC driver";
	}

	public String getFinderTargetPlural() {
		return "JDBC drivers";
	}
}
